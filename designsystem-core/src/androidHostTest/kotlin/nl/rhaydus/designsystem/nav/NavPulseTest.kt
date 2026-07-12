package nl.rhaydus.designsystem.nav

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NavPulseTest {
    @Nested
    inner class CountFor {
        @Test
        fun `returns 0 for a key that has never been pulsed`() {
            // ----- Arrange -----
            val navPulse = NavPulse()

            // ----- Act -----
            val result = navPulse.countFor("library")

            // ----- Assert -----
            result shouldBe 0
        }

        @Test
        fun `returns 0 for other keys while an unrelated key was pulsed`() {
            // ----- Arrange -----
            val navPulse = NavPulse()
            navPulse.pulse("library")

            // ----- Act -----
            val result = navPulse.countFor("explore")

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
            navPulse.pulse("library")

            // ----- Assert -----
            navPulse.countFor("library") shouldBe 1
        }

        @Test
        fun `accumulates across repeated pulses of the same key`() {
            // ----- Arrange -----
            val navPulse = NavPulse()

            // ----- Act -----
            navPulse.pulse("library")
            navPulse.pulse("library")
            navPulse.pulse("library")

            // ----- Assert -----
            navPulse.countFor("library") shouldBe 3
        }

        @Test
        fun `tracks arbitrary object keys by reference equality`() {
            // ----- Arrange -----
            val navPulse = NavPulse()
            val libraryTab = Any()

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
            pulsedInstance.pulse("library")

            // ----- Assert -----
            untouchedInstance.countFor("library") shouldBe 0
        }
    }
}
