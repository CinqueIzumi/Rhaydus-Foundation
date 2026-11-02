package fixture.states

class CopyingFunctionUiState(val stored: ProfileUiModel) {
    fun renamed(): ProfileUiModel = stored.copy(name = "b")
}
