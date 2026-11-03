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

// Writes git-cliff's changelog context (every release with its parsed, grouped commits) as JSON, using
// the repository's cliff.toml. RenderPackagingTask turns it into release notes for the package manifests.
// Needs `git-cliff` on the PATH and the full git history (CI: `fetch-depth: 0`).

package org.spotube.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault(because = "Reads the git history, which is not a declared input")
abstract class GitCliffContextTask @Inject constructor(private val execOperations: ExecOperations) : DefaultTask() {
    @get:Input
    abstract val executable: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val configFile: RegularFileProperty

    @get:Internal
    abstract val repositoryDir: DirectoryProperty

    /** Tag for the commits since the last release (`--tag`); unset leaves them unreleased (nightlies). */
    @get:Input
    @get:Optional
    abstract val tag: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    init {
        group = "distribution"
        description = "Writes the git-cliff changelog context (releases and their commits) as JSON."
        executable.convention("git-cliff")
        // New commits and tags change the result without touching any declared input
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun run() {
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        val args = buildList {
            add(executable.get())
            addAll(listOf("--config", configFile.get().asFile.absolutePath, "--context"))
            tag.orNull?.let { addAll(listOf("--tag", it)) }
            addAll(listOf("--output", output.absolutePath))
        }
        try {
            execOperations.exec {
                workingDir = repositoryDir.get().asFile
                commandLine(args)
            }
        } catch (e: Exception) {
            throw GradleException(
                "Running git-cliff failed (${args.joinToString(" ")}). Install it from https://git-cliff.org " +
                    "and make sure the full git history is available.",
                e,
            )
        }
    }
}
