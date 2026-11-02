package nl.rhaydus.designsystem.nav

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NavPulseTest {
    private data object Library : NavPulseKey

    private data object Explore : NavPulseKey

    private class PlainKey : NavPulseKey

    @Nested
    inner class CountFor {
        @Test
        fun `returns 0 for a key that has never been pulsed`() {
            // ----- Arrange -----
            val navPulse = NavPulse()

            // ----- Act -----
            val result = navPulse.countFor(Library)

            // ----- Assert -----
            result shouldBe 0
        }

        @Test
        fun `returns 0 for other keys while an unrelated key was pulsed`() {
            // ----- Arrange -----
            val navPulse = NavPulse()
            navPulse.pulse(Library)

            // ----- Act -----
            val result = navPulse.countFor(Explore)

            // ----- Assert -----
            result shouldBe 0
        }
    }

    @Nested
    inner class Pulse {
        @Test
        fun `increments the count for the pulsed key`() {
            // ----- Arrange -----
            val navPulse = NavPulse()

            // ----- Act -----
            navPulse.pulse(Library)

            // ----- Assert -----
            navPulse.countFor(Library) shouldBe 1
        }

        @Test
        fun `accumulates across repeated pulses of the same key`() {
            // ----- Arrange -----
            val navPulse = NavPulse()

            // ----- Act -----
            navPulse.pulse(Library)
            navPulse.pulse(Library)
            navPulse.pulse(Library)

            // ----- Assert -----
            navPulse.countFor(Library) shouldBe 3
        }

        @Test
        fun `tracks arbitrary object keys by reference equality`() {
            // ----- Arrange -----
            val navPulse = NavPulse()
            val libraryTab = PlainKey()

            // ----- Act -----
            navPulse.pulse(libraryTab)

            // ----- Assert -----
            navPulse.countFor(libraryTab) shouldBe 1
        }

        @Test
        fun `does not affect a separate NavPulse instance`() {
            // ----- Arrange -----
            val pulsedInstance = NavPulse()
            val untouchedInstance = NavPulse()

            // ----- Act -----
            pulsedInstance.pulse(Library)

            // ----- Assert -----
            untouchedInstance.countFor(Library) shouldBe 0
        }
    }
}
