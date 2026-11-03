/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

// Renders one package manager's directory (packaging/<target>) into the build dir: `*.mustache` files are
// rendered with JMustache (suffix dropped), everything else is copied as is.
//
// Template data:
//   version, tag, pkgrel, repositoryUrl
//   artifacts.<key>.{fileName, url, sha256, sha256Upper}   release assets found in artifactsDir
//   release.{version, date, url, groups, notes}            the release being packaged
//   releases                                               tagged releases, newest first (metainfo)
//     groups[].{title, entries[].text}                     changelog grouped like cliff.toml
//   #indent2 / #indent4                                    indent a multi-line block (YAML block scalars)
//
// Missing variables fail the render, so a manifest never ships with an empty checksum or version.
// HTML/XML escaping is on for .xml and .nuspec output only.

package org.spotube.gradle

import com.samskivert.mustache.Mustache
import com.samskivert.mustache.MustacheException
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@CacheableTask
abstract class RenderPackagingTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val templateDir: DirectoryProperty

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val pkgrel: Property<String>

    @get:Input
    abstract val repositoryUrl: Property<String>

    /** Release assets by template key, e.g. `linuxX86_64` -> `spotube-linux-6.0.0-x86_64.tar.xz`. */
    @get:Input
    abstract val artifactNames: MapProperty<String, String>

    /** Directory holding the downloaded release assets; only needed by templates that use `artifacts`. */
    @get:Internal
    abstract val artifactsDir: DirectoryProperty

    /** The release assets that exist in [artifactsDir]; their checksums are part of the output. */
    @get:Input
    protected val artifactChecksums: Map<String, String>
        get() = artifactFiles().mapValues { (_, file) -> sha256(file) }

    /** git-cliff `--context` output (GitCliffContextTask). */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val changelogContext: RegularFileProperty

    /** How many tagged releases `releases` lists. */
    @get:Input
    abstract val maxReleases: Property<Int>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        group = "distribution"
        pkgrel.convention("1")
        maxReleases.convention(10)
    }

    @TaskAction
    fun render() {
        val source = templateDir.get().asFile
        val target = outputDir.get().asFile
        target.deleteRecursively()
        target.mkdirs()

        val data = templateData()
        source.walkTopDown().filter { it.isFile }.forEach { file ->
            val relative = file.relativeTo(source).path
            if (!relative.endsWith(TEMPLATE_SUFFIX)) {
                file.copyTo(target.resolve(relative), overwrite = true)
                return@forEach
            }
            val output = target.resolve(relative.removeSuffix(TEMPLATE_SUFFIX))
            val compiler = Mustache.compiler()
                .escapeHTML(output.extension in XML_EXTENSIONS)
                .emptyStringIsFalse(true)
                .zeroIsFalse(true)
            val rendered = try {
                compiler.compile(file.readText()).execute(data)
            } catch (e: MustacheException) {
                throw GradleException(
                    "Rendering $relative failed: ${e.message?.trimEnd('.')}. Templates that use `artifacts` need " +
                        "-PpackagingArtifactsDir=<dir with the release assets>.",
                    e,
                )
            }
            output.parentFile.mkdirs()
            output.writeText(rendered)
        }
    }

    private fun templateData(): Map<String, Any> {
        val version = version.get()
        val tag = "v$version"
        val repositoryUrl = repositoryUrl.get()
        val downloadUrl = "$repositoryUrl/releases/download/$tag"

        val artifacts = artifactFiles().mapValues { (_, file) ->
            val sha256 = sha256(file)
            mapOf(
                "fileName" to file.name,
                "url" to "$downloadUrl/${file.name}",
                "sha256" to sha256,
                "sha256Upper" to sha256.uppercase(),
            )
        }

        val changelog = changelogContext.orNull?.asFile?.let(::readChangelog).orEmpty()
        val tagged = changelog.filter { it.version != null }.sortedByDescending { it.timestamp }
        // The release being packaged; nightlies have no tag, so they get the unreleased commits
        val current = changelog.firstOrNull { it.version == tag }
            ?: changelog.firstOrNull { it.version == null }
            ?: ChangelogRelease(tag, Instant.now().epochSecond, emptyList())

        return mapOf(
            "version" to version,
            "tag" to tag,
            "pkgrel" to pkgrel.get(),
            "repositoryUrl" to repositoryUrl,
            "artifacts" to artifacts,
            "release" to releaseData(current, version, repositoryUrl),
            "releases" to tagged.take(maxReleases.get()).map {
                releaseData(it, it.version!!.removePrefix("v"), repositoryUrl)
            },
            "indent2" to indent(2),
            "indent4" to indent(4),
        )
    }

    private fun artifactFiles(): Map<String, File> {
        val dir = artifactsDir.orNull?.asFile ?: return emptyMap()
        return artifactNames.get()
            .mapValues { (_, name) -> dir.resolve(name) }
            .filterValues { it.isFile }
    }

    private fun releaseData(release: ChangelogRelease, version: String, repositoryUrl: String): Map<String, Any> {
        val groups = release.commits
            .groupBy { it.group }
            .toSortedMap()
            .map { (group, commits) ->
                mapOf(
                    // cliff.toml orders groups with an HTML comment prefix: "<!-- 0 -->Features"
                    "title" to group.replace(HTML_COMMENT, "").trim(),
                    "entries" to commits.map { mapOf("text" to it.text) },
                )
            }
        val notes = groups.joinToString("\n\n") { group ->
            @Suppress("UNCHECKED_CAST")
            val entries = group["entries"] as List<Map<String, String>>
            "${group["title"]}\n" + entries.joinToString("\n") { "- ${it["text"]}" }
        }
        return mapOf(
            "version" to version,
            "date" to LocalDate.ofInstant(Instant.ofEpochSecond(release.timestamp), ZoneOffset.UTC).toString(),
            "url" to "$repositoryUrl/releases/tag/v$version",
            "groups" to groups,
            "notes" to notes,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun readChangelog(file: File): List<ChangelogRelease> {
        val releases = JsonSlurper().parse(file) as List<Map<String, Any?>>
        return releases.map { release ->
            val commits = (release["commits"] as List<Map<String, Any?>>).mapNotNull { commit ->
                val group = commit["group"] as String? ?: return@mapNotNull null
                val message = (commit["message"] as String).plainText().replaceFirstChar { it.uppercase() }
                val scope = commit["scope"] as String?
                val breaking = commit["breaking"] == true
                val text = buildString {
                    if (breaking) append("Breaking: ")
                    if (scope != null) append("$scope: ")
                    append(message)
                }
                ChangelogCommit(group, text)
            }
            ChangelogRelease(
                version = release["version"] as String?,
                timestamp = (release["timestamp"] as Number?)?.toLong() ?: Instant.now().epochSecond,
                commits = commits,
            )
        }
    }

    private data class ChangelogRelease(val version: String?, val timestamp: Long, val commits: List<ChangelogCommit>)

    private data class ChangelogCommit(val group: String, val text: String)

    private companion object {
        const val TEMPLATE_SUFFIX = ".mustache"
        val XML_EXTENSIONS = setOf("xml", "nuspec")
        val HTML_COMMENT = Regex("<!--.*?-->")
        val MARKDOWN_LINK = Regex("""\[([^\]]+)]\([^)]+\)""")

        // cliff.toml's commit preprocessors turn issue numbers into Markdown links
        fun String.plainText() = replace(MARKDOWN_LINK, "$1").replace("**", "").trim()

        fun indent(spaces: Int) = Mustache.Lambda { fragment, out ->
            val prefix = " ".repeat(spaces)
            out.write(fragment.execute().lines().joinToString("\n") { if (it.isEmpty()) it else prefix + it })
        }

        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(1 shl 16)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
