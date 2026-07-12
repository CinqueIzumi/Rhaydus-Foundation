package nl.rhaydus.designsystem.haptics

internal object NoOpHaptics : Haptics {
    override fun commit() = Unit

    override fun reject() = Unit

    override fun select() = Unit

    override fun threshold() = Unit

    override fun tickle() = Unit

    override fun lift() = Unit

    override fun drop() = Unit

    override fun milestone() = Unit
}
