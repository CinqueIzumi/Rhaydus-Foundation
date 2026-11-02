package fixture.states

class ExemptUiState {
    @Exempt
    val model: ProfileUiModel
        get() = ProfileUiModel(name = "a")

    @Exempt
    fun built(): ProfileUiModel = ProfileUiModel(name = "b")
}
