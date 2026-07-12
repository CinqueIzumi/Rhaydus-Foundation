package nl.rhaydus.designsystem.component

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StarRatingInputTest {
    // Shared row geometry: 5 stars, 100px each, 20px spacing → 120px slot.
    private val starSizePx = 100f
    private val spacingPx = 20f
    private val starCount = 5

    @Nested
    inner class RatingForOffsetX {
        @Test
        fun `returns half value for x in the first star's left half`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = ratingForOffsetX(
                x = 10f,
                starSizePx = starSizePx,
                spacingPx = spacingPx,
                starCount = starCount,
            )

            // ----- Assert -----
            result shouldBe 0.5
        }

        @Test
        fun `returns whole value for x past the first star's midpoint`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = ratingForOffsetX(
                x = 60f,
                starSizePx = starSizePx,
                spacingPx = spacingPx,
                starCount = starCount,
            )

            // ----- Assert -----
            result shouldBe 1.0
        }

        @Test
        fun `returns half value for a later star's left half`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = ratingForOffsetX(
                x = 250f,
                starSizePx = starSizePx,
                spacingPx = spacingPx,
                starCount = starCount,
            )

            // ----- Assert -----
            result shouldBe 2.5
        }

        @Test
        fun `returns whole value for a later star's right half`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = ratingForOffsetX(
                x = 290f,
                starSizePx = starSizePx,
                spacingPx = spacingPx,
                starCount = starCount,
            )

            // ----- Assert -----
            result shouldBe 3.0
        }

        @Test
        fun `returns 0-point-5 for x at the very start of the row — minimum rating`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = ratingForOffsetX(
                x = 0f,
                starSizePx = starSizePx,
                spacingPx = spacingPx,
                starCount = starCount,
            )

            // ----- Assert -----
            result shouldBe 0.5
        }

        @Test
        fun `clamps to starCount for x well past the last star — index clamped to the last slot`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = ratingForOffsetX(
                x = 1_000f,
                starSizePx = starSizePx,
                spacingPx = spacingPx,
                starCount = starCount,
            )

            // ----- Assert -----
            result shouldBe 5.0
        }

        @Test
        fun `treats the spacing gap as the preceding star's whole value — localX exceeds star width`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = ratingForOffsetX(
                x = 110f,
                starSizePx = starSizePx,
                spacingPx = spacingPx,
                starCount = starCount,
            )

            // ----- Assert -----
            result shouldBe 1.0
        }
    }
}
