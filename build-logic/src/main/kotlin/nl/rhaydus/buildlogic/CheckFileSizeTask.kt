package nl.rhaydus.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails when a file of [sourceFiles] in scope is longer than [maxLines], listing each offender with its
 * line count. A file is in scope when its module is in [wholeModules], or when a directory segment between
 * its source set and its name is in [packageSegments]. [sourceFiles] holds the candidate files so Gradle
 * tracks the source directories; [moduleDirectories] maps each project path to its directory relative to
 * [rootDirectory].
 */
abstract class CheckFileSizeTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:Input
    abstract val maxLines: Property<Int>

    @get:Input
    abstract val packageSegments: SetProperty<String>

    @get:Input
    abstract val wholeModules: SetProperty<String>

    @get:Input
    abstract val moduleDirectories: MapProperty<String, String>

    /** Only relativises the paths in the failure message and the module lookup. */
    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @TaskAction
    fun check() {
        val limit = maxLines.get()
        val root = rootDirectory.get().asFile
        val segments = packageSegments.get()
        val whole = wholeModules.get()
        val modules = moduleDirectories.get().entries.sortedByDescending { it.value.length }

        val files = sourceFiles.files.filter { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            val module = modules.first { relative.startsWith(it.value.prefixed()) }

            module.key in whole ||
                relative.removePrefix(module.value.prefixed())
                    .split("/")
                    .drop(1)
                    .dropLast(1)
                    .any { it in segments }
        }

        val violations = files
            .map { file -> file to file.useLines { it.count() } }
            .filter { (_, lines) -> lines > limit }
            .map { (file, lines) -> "${file.relativeTo(root).invariantSeparatorsPath}  ($lines lines)" }
            .sorted()

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Files over $limit lines:\n" + violations.joinToString("\n") { "  - $it" },
            )
        }

        logger.lifecycle("checkFileSize: ${files.size} file(s) checked, all within $limit lines.")
    }

    private fun String.prefixed(): String = if (isEmpty()) "src/" else "$this/src/"
}
