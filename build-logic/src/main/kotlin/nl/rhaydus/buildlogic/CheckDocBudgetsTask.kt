package nl.rhaydus.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Gates the doc budgets in CI: a file already over its budget still passes as long as it has not grown,
 * in the budget's own unit, since the merge-base with the default branch (`origin/<branch>`, falling back
 * to the local branch). A file that is over budget and did not exist at that merge-base always fails.
 * Resolving no merge-base at all (a shallow checkout) is a hard failure rather than a silent pass.
 *
 * The inputs mirror the `docGuard` object the `doc-guard.sh` hook reads, so both enforce one predicate.
 * Without a budgets file the budget checks are skipped; empty doc lists switch off the matching citation
 * checks.
 */
abstract class CheckDocBudgetsTask : DefaultTask() {
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val configFile: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val budgetsFile: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val activeFile: RegularFileProperty

    @get:Input
    abstract val workingDocs: ListProperty<String>

    @get:Input
    abstract val permanentDocs: ListProperty<String>

    @get:Input
    abstract val decisionPattern: Property<String>

    @get:Input
    abstract val stepPattern: Property<String>

    @get:Input
    abstract val defaultBranch: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val markdownFiles: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val configFiles: ConfigurableFileCollection

    @get:Internal
    abstract val repoRoot: DirectoryProperty

    @TaskAction
    fun check() {
        val root = repoRoot.get().asFile
        val budgets = budgetsFile.orNull?.asFile?.takeIf { it.isFile }?.let { DocBudgets.parse(it.readText()) }
        val violations = mutableListOf<String>()

        if (budgets != null) {
            val mergeBase = resolveMergeBase(root, defaultBranch.get())
            checkFileBudgets(root, budgets, mergeBase, violations)
            checkNowSection(root, budgets, mergeBase, violations)
        }
        checkMarkdownCitations(root, violations)
        checkConfigComments(root, violations)

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Doc budgets exceeded:\n" + violations.sorted().joinToString("\n") { "  - $it" },
            )
        }

        logger.lifecycle("checkDocBudgets: ${markdownFiles.files.size} markdown files within budget.")
    }

    private fun checkFileBudgets(
        root: File,
        budgets: DocBudgets,
        mergeBase: String,
        violations: MutableList<String>,
    ) {
        markdownFiles.files.forEach { file ->
            val relativePath = file.relativeTo(root).invariantSeparatorsPath
            val rule = budgets.rules.firstOrNull { it.matches(relativePath) } ?: return@forEach
            val currentSize = sizeOf(file.readBytes(), rule.unit)
            if (currentSize <= rule.limit) return@forEach

            val limit = "limit ${rule.limit}${rule.unit.suffix}"
            val baseBytes = gitShow(root, mergeBase, relativePath)
            if (baseBytes == null) {
                violations += "$relativePath: new file over budget (${rule.glob} ${rule.token}) - " +
                    "$currentSize${rule.unit.suffix} ($limit)"
                return@forEach
            }

            val baseSize = sizeOf(baseBytes, rule.unit)
            if (currentSize > baseSize) {
                violations += "$relativePath: over budget and grew since merge-base (${rule.glob} ${rule.token}) - " +
                    "$baseSize${rule.unit.suffix} -> $currentSize${rule.unit.suffix} ($limit)"
            }
        }
    }

    private fun checkNowSection(
        root: File,
        budgets: DocBudgets,
        mergeBase: String,
        violations: MutableList<String>,
    ) {
        val nowLimit = budgets.nowLimit ?: return
        val tracked = activeFile.orNull?.asFile?.takeIf { it.isFile }?.readLines() ?: return
        tracked.map { it.trim() }.filter { it.isNotEmpty() }.forEach { relativePath ->
            val file = root.resolve(relativePath)
            if (file.isFile.not()) return@forEach

            val currentNow = lineCountOf(extractNowSection(file.readText()))
            if (currentNow <= nowLimit) return@forEach

            val baseBytes = gitShow(root, mergeBase, relativePath)
            val baseNow = baseBytes?.let { lineCountOf(extractNowSection(it.toString(Charsets.UTF_8))) } ?: 0
            if (baseBytes == null || currentNow > baseNow) {
                violations += "$relativePath ## Now section: over budget (## Now ${nowLimit}L) - " +
                    "${baseNow}L -> ${currentNow}L"
            }
        }
    }

    private fun checkMarkdownCitations(
        root: File,
        violations: MutableList<String>,
    ) {
        val permanent = permanentDocs.get()
        if (permanent.isEmpty()) return
        val planDirectories = workingDocs.get().map { Regex(Regex.escape(it) + """[^/\s]+/""") }
        val decision = Regex(decisionPattern.get())
        val step = Regex(stepPattern.get())

        markdownFiles.files.forEach { file ->
            val relativePath = file.relativeTo(root).invariantSeparatorsPath
            if (isListed(relativePath, permanent).not()) return@forEach

            file.readText().lineSequence().forEachIndexed { index, line ->
                val at = "$relativePath:${index + 1}"
                if (planDirectories.any { it.containsMatchIn(line) }) {
                    violations += "$at: cites a plan directory - $PLAN_HINT"
                }
                if (decision.containsMatchIn(line)) violations += "$at: cites a decision number - $PLAN_HINT"
                if (step.containsMatchIn(line)) violations += "$at: cites a plan step - $PLAN_HINT"
            }
        }
    }

    private fun checkConfigComments(
        root: File,
        violations: MutableList<String>,
    ) {
        val working = workingDocs.get().map { Regex(Regex.escape(it)) }
        val decision = Regex(decisionPattern.get())
        val step = Regex(stepPattern.get())

        configFiles.files.forEach { file ->
            val relativePath = file.relativeTo(root).invariantSeparatorsPath
            val extractComment: (String) -> String? = when {
                relativePath.endsWith(".yml") || relativePath.endsWith(".yaml") -> DocCommentExtractors::yamlStyle
                relativePath.endsWith(".kts") -> DocCommentExtractors::kotlinStyle
                else -> return@forEach
            }

            file.readText().lineSequence().forEachIndexed { index, line ->
                val comment = extractComment(line) ?: return@forEachIndexed
                val at = "$relativePath:${index + 1}"
                if (working.any { it.containsMatchIn(comment) }) {
                    violations += "$at: comment cites working docs - $PLAN_HINT"
                }
                if (decision.containsMatchIn(comment)) violations += "$at: comment cites a decision number - $PLAN_HINT"
                if (step.containsMatchIn(comment)) violations += "$at: comment cites a plan step - $PLAN_HINT"
            }
        }
    }

    private fun sizeOf(
        bytes: ByteArray,
        unit: BudgetUnit,
    ): Long = when (unit) {
        BudgetUnit.BYTES -> bytes.size.toLong()
        BudgetUnit.LINES -> lineCountOf(bytes.toString(Charsets.UTF_8))
    }

    private fun isListed(
        relativePath: String,
        entries: List<String>,
    ): Boolean = entries.any { if (it.endsWith("/")) relativePath.startsWith(it) else relativePath == it }

    private fun lineCountOf(text: String): Long = when {
        text.isEmpty() -> 0
        text.endsWith("\n") -> text.count { it == '\n' }.toLong()
        else -> text.count { it == '\n' }.toLong() + 1
    }

    private fun extractNowSection(text: String): String {
        val lines = text.split("\n")
        val start = lines.indexOfFirst { it.trimEnd() == "## Now" }
        if (start == -1) return ""
        val section = mutableListOf<String>()
        for (index in start until lines.size) {
            if (index > start && lines[index].startsWith("## ")) break
            section += lines[index]
        }
        return section.joinToString("\n")
    }

    private fun runGit(
        workingDir: File,
        vararg args: String,
    ): ByteArray? {
        val process = ProcessBuilder(listOf("git") + args).directory(workingDir).start()
        val output = process.inputStream.use { it.readBytes() }
        process.errorStream.use { it.readBytes() }
        return if (process.waitFor() == 0) output else null
    }

    private fun resolveMergeBase(
        root: File,
        branch: String,
    ): String =
        listOf("origin/$branch", branch).firstNotNullOfOrNull { ref ->
            runGit(root, "merge-base", "HEAD", ref)?.toString(Charsets.UTF_8)?.trim()
        } ?: throw GradleException(
            "checkDocBudgets: cannot resolve a git merge-base against origin/$branch or $branch - " +
                "the checkout needs full history (fetch-depth: 0).",
        )

    private fun gitShow(
        root: File,
        ref: String,
        relativePath: String,
    ): ByteArray? = runGit(root, "show", "$ref:$relativePath")

    private companion object {
        const val PLAN_HINT = "plans are deleted when they finish; cite nothing from the working docs. " +
            "The reason goes in the PR description."
    }
}
