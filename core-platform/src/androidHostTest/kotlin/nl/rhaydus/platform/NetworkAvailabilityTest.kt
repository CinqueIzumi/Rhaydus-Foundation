package nl.rhaydus.platform

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NetworkAvailabilityTest {
    private class FakeProvider(
        override val isOnline: MutableStateFlow<Boolean>,
    ) : NetworkAvailabilityProvider {
        override suspend fun awaitOnline() {
            isOnline.first { it }
        }
    }

    @Nested
    inner class IsOnline {
        @Test
        fun `returns true when the installed provider's isOnline is true`() {
            // ----- Arrange -----
            NetworkAvailability.install(FakeProvider(MutableStateFlow(true)))

            // ----- Act -----
            val result = NetworkAvailability.isOnline()

            // ----- Assert -----
            result shouldBe true
        }

        @Test
        fun `returns false when the installed provider's isOnline is false`() {
            // ----- Arrange -----
            NetworkAvailability.install(FakeProvider(MutableStateFlow(false)))

            // ----- Act -----
            val result = NetworkAvailability.isOnline()

            // ----- Assert -----
            result shouldBe false
        }
    }
}
