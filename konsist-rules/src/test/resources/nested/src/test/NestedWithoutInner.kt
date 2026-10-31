package fixture.nested

class GroupedTest {
    @Nested
    class Case {
        @Test
        fun works() = Unit
    }
}
