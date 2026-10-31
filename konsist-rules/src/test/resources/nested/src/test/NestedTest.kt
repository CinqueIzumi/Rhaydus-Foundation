package fixture.nested

class GroupedTest {
    @Nested
    inner class Case {
        @Test
        fun works() = Unit
    }
}
