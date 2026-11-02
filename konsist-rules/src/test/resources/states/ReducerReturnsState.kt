package fixture.states

class ReducerUiState(val stored: ProfileUiModel) {
    fun rename(): ReducerUiState = copy(stored = stored.copy(name = "b"))
}
