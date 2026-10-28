package nl.rhaydus.buildlogic

internal data class ExternalEdge(
    val from: String,
    val configurationName: String,
    val group: String,
    val name: String,
)
