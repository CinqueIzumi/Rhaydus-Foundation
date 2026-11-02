package fixture.states

class MappedUiState(val items: List<String>) {
    val models: List<Any>
        get() = items.map { it.toUiModel() }
}
