package nl.rhaydus.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * `rhaydus.doc-budgets`: registers `checkDocBudgets` on the applying project, which should be the root.
 * The gate reads the `docGuard` object of the project's `.claude/rhaydus.json` (see [DocBudgetsExtension]),
 * the same predicate source as the `rhaydus-kotlin` plugin's `doc-guard.sh` hook, so the write-time hook and
 * CI cannot drift. Without that config the task does nothing.
 */
class DocBudgetsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        val extension = extensions.create("docBudgets", DocBudgetsExtension::class.java)
        extension.configFile.convention(layout.projectDirectory.file(DEFAULT_CONFIG_PATH))
        extension.defaultBranch.convention("main")

        val config = extension.configFile.map { DocGuardConfig.load(it.asFile) }
        val excluded = listOf("**/build/**", "**/.gradle/**", "**/.git/**", "**/.idea/**")

        tasks.register<CheckDocBudgetsTask>("checkDocBudgets") {
            group = "verification"
            description = "Fails on any markdown file that crosses its configured doc budget."

            configFile.set(extension.configFile)
            budgetsFile.set(layout.projectDirectory.file(config.map { it.budgetsFile }))
            activeFile.set(layout.projectDirectory.file(config.map { it.activeFile }))
            workingDocs.set(config.map { it.workingDocs })
            permanentDocs.set(config.map { it.permanentDocs })
            decisionPattern.set(config.map { it.decisionPattern })
            stepPattern.set(config.map { it.stepPattern })
            defaultBranch.set(extension.defaultBranch)
            repoRoot.set(layout.projectDirectory)
            markdownFiles.from(
                fileTree(layout.projectDirectory) {
                    include("**/*.md")
                    exclude(excluded)
                },
            )
            configFiles.from(
                fileTree(layout.projectDirectory) {
                    include("config/**", "**/*.yml", "**/*.yaml", "**/*.kts")
                    exclude(excluded)
                },
            )
        }
    }

    private companion object {
        const val DEFAULT_CONFIG_PATH = ".claude/rhaydus.json"
    }
}
