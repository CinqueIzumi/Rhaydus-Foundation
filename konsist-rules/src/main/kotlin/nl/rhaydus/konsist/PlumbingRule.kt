package nl.rhaydus.konsist

/**
 * A package fragment whose top-level declarations with one of [nameSuffixes] are module-internal plumbing.
 *
 * @property packageFragment a substring of the file's package, for example `.presentation.action`.
 */
data class PlumbingRule(
    val packageFragment: String,
    val nameSuffixes: List<String>,
)
