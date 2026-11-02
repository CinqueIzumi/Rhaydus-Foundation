package fixture.states

class QualifiedUiState {
    val model: Outer.FooUiModel
        get() = Outer.FooUiModel(name = "a")
}
