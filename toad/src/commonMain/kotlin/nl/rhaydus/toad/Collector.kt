package nl.rhaydus.toad

interface Collector<S : UiState, E : UiEvent, D : ActionDependencies, V : LocalVariables> {
    suspend fun onLaunch(
        scope: ActionScope<S, E, V>,
        dependencies: D,
    )
}
