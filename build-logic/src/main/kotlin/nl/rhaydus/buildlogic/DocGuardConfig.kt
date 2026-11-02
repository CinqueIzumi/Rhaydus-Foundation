package nl.rhaydus.buildlogic

import groovy.json.JsonSlurper
import java.io.File

/**
 * The `docGuard` object of a project's `.claude/rhaydus.json`, the predicate source shared by the
 * `rhaydus-kotlin` plugin's write-time `doc-guard.sh` hook and the `checkDocBudgets` task. Paths are
 * project-relative; an entry ending in `/` is a prefix, any other entry names one file. A missing file or
 * key yields [DISABLED]; any `docGuard` object, even an empty one, enables it.
 */
internal data class DocGuardConfig(
    val budgetsFile: String?,
    val activeFile: String?,
    val workingDocs: List<String>,
    val permanentDocs: List<String>,
    val decisionPattern: String,
    val stepPattern: String,
    val isEnabled: Boolean = true,
) {
    companion object {
        const val DEFAULT_DECISION_PATTERN = """\bD[0-9]{1,2}\b"""
        const val DEFAULT_STEP_PATTERN = """\bS[0-9]{1,2}-[0-9A-Z]"""

        val DISABLED = DocGuardConfig(
            budgetsFile = null,
            activeFile = null,
            workingDocs = emptyList(),
            permanentDocs = emptyList(),
            decisionPattern = DEFAULT_DECISION_PATTERN,
            stepPattern = DEFAULT_STEP_PATTERN,
            isEnabled = false,
        )

        fun load(file: File): DocGuardConfig {
            if (file.isFile.not()) return DISABLED

            val root = JsonSlurper().parse(file) as? Map<*, *> ?: return DISABLED

            val section = root["docGuard"] as? Map<*, *> ?: return DISABLED

            return DocGuardConfig(
                budgetsFile = section["budgetsFile"] as? String,
                activeFile = section["activeFile"] as? String,
                workingDocs = strings(section["workingDocs"]),
                permanentDocs = strings(section["permanentDocs"]),
                decisionPattern = section["decisionPattern"] as? String ?: DEFAULT_DECISION_PATTERN,
                stepPattern = section["stepPattern"] as? String ?: DEFAULT_STEP_PATTERN,
            )
        }

        private fun strings(value: Any?): List<String> =
            (value as? List<*>)?.filterIsInstance<String>().orEmpty()
    }
}
