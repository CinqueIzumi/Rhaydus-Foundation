package fixture.types

sealed interface Result

class Ok : Result

object Failed : Result
