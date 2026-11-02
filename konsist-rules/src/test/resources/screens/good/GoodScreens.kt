package fixture.screens

enum class Mode { A }

data class Payload(val id: Int) : Serializable

class GoodScreen(
    val id: Long,
    val title: String,
    val mode: Mode,
    val payload: Payload,
    @TransientNavArg val callback: () -> Unit,
    plain: List<String>,
) : Screen
