package fixture.states

class ProfileUiState(val stored: ProfileUiModel) {
    val model: ProfileUiModel
        get() = stored.copy(name = "b")
}
