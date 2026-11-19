package org.spotube.gradle

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Lets the app switch off Millennium's built-in interaction gel.
 *
 * Millennium draws hover, focus and press feedback that no theme value reaches:
 * - `millenniumInteractionSurface`, used by nearly every control, paints a hard-coded white sheen
 *   and rim on hover/focus and a dark shade on press, scaled by `millenniumInteractionLevels`;
 * - list tiles fill their hover/press states from `listTileBackground`, an accent gradient.
 *
 * For each of those functions this patch renames the original and puts a straight-line wrapper in
 * its place: it calls the original, then passes the result and the original arguments to the
 * same-named function on the app's `MillenniumInteraction` object, which returns it unchanged in
 * the Classic style and flattens it in the Modern one. Nothing else in the library changes.
 *
 * Applied to the desktop runtime classpath by [PatchMillenniumJar] and to Android classes by
 * `MillenniumInteractionAsmFactory` in composeApp/build.gradle.kts (it implements an AGP interface,
 * and buildSrc cannot see AGP's classes). iOS links the library natively and keeps the gel.
 */
object MillenniumInteractionPatch {
    private const val FILTER_OWNER = "org/spotube/spotube/core/ui/theming/MillenniumInteraction"

    private data class Target(val owner: String, val name: String, val descriptor: String)

    private val targets = listOf(
        Target(
            "org/spotube/millennium/theme/InteractionKt", "millenniumInteractionLevels",
            "(Lorg/spotube/millennium/theme/MillenniumControlState;)Lkotlin/Pair;"
        ),
        Target(
            "org/spotube/millennium/composables/ListTileKt", "listTileBackground",
            "(Lorg/spotube/millennium/theme/MillenniumColors;ZLorg/spotube/millennium/theme/MillenniumControlState;F)" +
                "Landroidx/compose/ui/graphics/Brush;"
        ),
    )

    /** Binary class names (dots) of the classes this patch rewrites. */
    val classNames: Set<String> = targets.map { it.owner.replace('/', '.') }.toSet()

    fun visitor(className: String, next: ClassVisitor): ClassVisitor {
        val owner = className.replace('.', '/')
        val wanted = targets.filter { it.owner == owner }
        return object : ClassVisitor(Opcodes.ASM9, next) {
            private val found = mutableMapOf<Target, Pair<Int, String?>>()

            override fun visitMethod(
                access: Int,
                name: String,
                descriptor: String,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor? {
                val target = wanted.firstOrNull { it.name == name && it.descriptor == descriptor }
                    ?: return super.visitMethod(access, name, descriptor, signature, exceptions)
                found[target] = access to signature
                return super.visitMethod(access, renamed(name), descriptor, signature, exceptions)
            }

            override fun visitEnd() {
                for (target in wanted) {
                    val (access, signature) = found[target]
                        ?: error("${target.owner}.${target.name}${target.descriptor} not found; the Millennium interaction patch needs updating")
                    writeWrapper(target, access, signature)
                }
                super.visitEnd()
            }

            // No branches, so the method needs no stack map frames.
            private fun writeWrapper(target: Target, access: Int, signature: String?) {
                val args = Type.getArgumentTypes(target.descriptor)
                val result = Type.getReturnType(target.descriptor)
                val isStatic = access and Opcodes.ACC_STATIC != 0
                check(isStatic) { "${target.name} is expected to be a top-level (static) function" }
                val mv = super.visitMethod(access, target.name, target.descriptor, signature, null)
                mv.visitCode()
                fun loadArgs() {
                    var slot = 0
                    for (arg in args) {
                        mv.visitVarInsn(arg.getOpcode(Opcodes.ILOAD), slot)
                        slot += arg.size
                    }
                }
                loadArgs()
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, target.owner, renamed(target.name), target.descriptor, false)
                loadArgs()
                val filterDescriptor = Type.getMethodDescriptor(result, result, *args)
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, FILTER_OWNER, target.name, filterDescriptor, false)
                mv.visitInsn(result.getOpcode(Opcodes.IRETURN))
                val argSlots = args.sumOf { it.size }
                mv.visitMaxs(result.size + argSlots, argSlots)
                mv.visitEnd()
            }
        }
    }

    private fun renamed(name: String) = "$name\$millennium"

    fun patch(className: String, classBytes: ByteArray): ByteArray {
        val reader = ClassReader(classBytes)
        val writer = ClassWriter(reader, 0)
        reader.accept(visitor(className, writer), 0)
        return writer.toByteArray()
    }
}

/** Desktop: rewrites the Millennium JVM jar on the runtime classpath, passes other jars through. */
abstract class PatchMillenniumJar : TransformAction<TransformParameters.None> {
    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val input: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val file = input.get().asFile
        if (!file.isFile || !file.name.startsWith("millennium-jvm")) {
            outputs.file(input)
            return
        }
        val entries = MillenniumInteractionPatch.classNames.associateBy { it.replace('.', '/') + ".class" }
        val out = outputs.file("${file.nameWithoutExtension}-flat-interaction.jar")
        val patched = mutableSetOf<String>()
        ZipFile(file).use { zip ->
            ZipOutputStream(out.outputStream().buffered()).use { zos ->
                for (entry in zip.entries()) {
                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    val className = entries[entry.name]
                    val content = if (className != null) {
                        patched += className
                        MillenniumInteractionPatch.patch(className, bytes)
                    } else bytes
                    zos.putNextEntry(ZipEntry(entry.name).apply { time = entry.time })
                    zos.write(content)
                    zos.closeEntry()
                }
            }
        }
        val missing = MillenniumInteractionPatch.classNames - patched
        check(missing.isEmpty()) { "${file.name} lacks $missing; the Millennium interaction patch needs updating" }
    }
}
