package nl.rhaydus.buildlogic

internal data class ProjectEdge(
    val from: String,
    val to: String,
    val configurationName: String,
)
