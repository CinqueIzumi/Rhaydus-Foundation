package nl.rhaydus.buildlogic

/** The project-specific half of [R8KeepContract], resolved from [R8KeepContractExtension]. */
internal data class R8KeepContractSettings(
    val projectPrefix: String,
    val databaseImplSuffix: String?,
    val renamedOnlySupertypes: List<String>,
)
