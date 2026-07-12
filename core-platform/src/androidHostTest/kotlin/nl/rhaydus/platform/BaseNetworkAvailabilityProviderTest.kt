package nl.rhaydus.platform

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BaseNetworkAvailabilityProviderTest {
    private class FakeProvider(
        override val isOnline: MutableStateFlow<Boolean>,
    ) : BaseNetworkAvailabilityProvider()

    @Nested
    inner class AwaitOnline {
        @Test
        fun `returns immediately when isOnline is already true`() =
            runTest {
                // ----- Arrange -----
                val provider = FakeProvider(MutableStateFlow(true))

                // ----- Act & Assert -----
                provider.awaitOnline()
            }

        @Test
        fun `suspends while offline and completes once isOnline flips to true`() =
            runTest {
                // ----- Arrange -----
                val isOnline = MutableStateFlow(false)
                val provider = FakeProvider(isOnline)

                // ----- Act -----
                val job = launch { provider.awaitOnline() }
                runCurrent()

                // ----- Assert -----
                job.isCompleted shouldBe false

                isOnline.value = true
                runCurrent()
                job.isCompleted shouldBe true
            }
    }
}
