package nl.rhaydus.buildlogic

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

/**
 * Configuration for the `rhaydus.doc-budgets` gate ([DocBudgetsConventionPlugin]).
 *
 * - [configFile] is the project config whose `docGuard` object both the gate and the `doc-guard.sh` hook
 *   read. Defaults to `.claude/rhaydus.json` under the project directory; a missing file or key makes the
 *   gate a no-op.
 * - [defaultBranch] is the branch whose merge-base with `HEAD` anchors the ratchet. Defaults to `main`.
 */
abstract class DocBudgetsExtension {
    abstract val configFile: RegularFileProperty

    abstract val defaultBranch: Property<String>
}
