package nl.rhaydus.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * `rhaydus.file-size`: registers `checkFileSize` on the applying project, which should be the root, and
 * wires it into every subproject's `check`. The gate fails when a `.kt` file in scope is longer than the
 * limit; the scope and the limit come from [FileSizeExtension]. There is no baseline and no exemption list.
 */
class FileSizeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        val extension = extensions.create(
            "fileSize",
            FileSizeExtension::class.java,
        )

        extension.maxLines.convention(DEFAULT_MAX_LINES)
        extension.packageSegments.convention(setOf(DEFAULT_PACKAGE_SEGMENT))
        extension.wholeModules.convention(emptySet())

        val checkFileSize = tasks.register<CheckFileSizeTask>("checkFileSize") {
            group = "verification"
            description = "Fails when a main source file in scope exceeds the configured line limit."

            maxLines.set(extension.maxLines)
            packageSegments.set(extension.packageSegments)
            wholeModules.set(extension.wholeModules)
            rootDirectory.set(layout.projectDirectory)

            moduleDirectories.set(
                rootDirectory.map { root ->
                    subprojects.associate { module ->
                        module.path to module.projectDir.relativeTo(root.asFile).invariantSeparatorsPath
                    }
                },
            )

            sourceFiles.from(
                files(
                    providers.provider {
                        subprojects.map { module ->
                            fileTree(module.projectDir.resolve("src")) {
                                include(
                                    "main/**/*.kt",
                                    "*Main/**/*.kt",
                                )
                            }
                        }
                    },
                ),
            )
        }

        subprojects {
            tasks.matching { it.name == "check" }.configureEach {
                dependsOn(checkFileSize)
            }
        }
    }

    private companion object {
        const val DEFAULT_MAX_LINES = 600
        const val DEFAULT_PACKAGE_SEGMENT = "presentation"
    }
}
