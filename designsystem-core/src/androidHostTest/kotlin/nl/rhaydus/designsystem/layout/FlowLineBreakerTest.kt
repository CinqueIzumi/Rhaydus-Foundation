package nl.rhaydus.designsystem.layout

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FlowLineBreakerTest {
    private fun breaker(
        maxLines: Int,
        indicatorWidth: Int = 20,
    ) = FlowLineBreaker(
        maxWidth = 100,
        gap = 10,
        maxLines = maxLines,
        indicatorWidth = indicatorWidth,
    )

    @Nested
    inner class Place {
        @Test
        fun `keeps an item on the current line when it fits exactly including the gap`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 2)

            breaker.place(
                width = 60,
                hasMore = false,
            )

            // ----- Act -----
            val result = breaker.place(
                width = 30,
                hasMore = false,
            )

            // ----- Assert -----
            result shouldBe 0
        }

        @Test
        fun `wraps to a new line when the gap pushes the item over the width`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 2)

            breaker.place(
                width = 60,
                hasMore = false,
            )

            // ----- Act -----
            val result = breaker.place(
                width = 31,
                hasMore = false,
            )

            // ----- Assert -----
            result shouldBe 1
        }

        @Test
        fun `measures the next item against the new line after wrapping`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 3)

            breaker.place(
                width = 60,
                hasMore = false,
            )

            breaker.place(
                width = 50,
                hasMore = false,
            )

            // ----- Act -----
            val result = breaker.place(
                width = 40,
                hasMore = false,
            )

            // ----- Assert -----
            result shouldBe 1
        }

        @Test
        fun `returns NOT_PLACED when the last line is full`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 1)

            breaker.place(
                width = 60,
                hasMore = false,
            )

            // ----- Act -----
            val result = breaker.place(
                width = 50,
                hasMore = false,
            )

            // ----- Assert -----
            result shouldBe NOT_PLACED
        }

        @Test
        fun `places an item on the last line when nothing follows it`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 1)

            breaker.place(
                width = 50,
                hasMore = false,
            )

            breaker.place(
                width = 10,
                hasMore = false,
            )

            // ----- Act -----
            val result = breaker.place(
                width = 1,
                hasMore = false,
            )

            // ----- Assert -----
            result shouldBe 0
        }

        @Test
        fun `does not place an item that would crowd out the indicator on the last line`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 1)

            breaker.place(
                width = 50,
                hasMore = true,
            )

            breaker.place(
                width = 10,
                hasMore = true,
            )

            // ----- Act -----
            val result = breaker.place(
                width = 1,
                hasMore = true,
            )

            // ----- Assert -----
            result shouldBe NOT_PLACED
        }

        @Test
        fun `places an item that leaves exactly the indicator width free`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 1)

            breaker.place(
                width = 50,
                hasMore = true,
            )

            // ----- Act -----
            val result = breaker.place(
                width = 10,
                hasMore = true,
            )

            // ----- Assert -----
            result shouldBe 0
        }

        @Test
        fun `does not start the last line with an item that leaves no room for the indicator`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 2)

            breaker.place(
                width = 60,
                hasMore = true,
            )

            // ----- Act -----
            val result = breaker.place(
                width = 75,
                hasMore = true,
            )

            // ----- Assert -----
            result shouldBe NOT_PLACED
        }

        @Test
        fun `starts the last line with an item that leaves room for the indicator`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 2)

            breaker.place(
                width = 60,
                hasMore = true,
            )

            // ----- Act -----
            val result = breaker.place(
                width = 50,
                hasMore = true,
            )

            // ----- Assert -----
            result shouldBe 1
        }

        @Test
        fun `always places an oversized first item on the first line`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 2)

            // ----- Act -----
            val result = breaker.place(
                width = 150,
                hasMore = true,
            )

            // ----- Assert -----
            result shouldBe 0
        }

        @Test
        fun `wraps the item after an oversized first item`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 2)

            breaker.place(
                width = 150,
                hasMore = false,
            )

            // ----- Act -----
            val result = breaker.place(
                width = 10,
                hasMore = false,
            )

            // ----- Assert -----
            result shouldBe 1
        }

        @Test
        fun `stops placing after an oversized first item on a single line`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 1)

            breaker.place(
                width = 150,
                hasMore = true,
            )

            // ----- Act -----
            val result = breaker.place(
                width = 10,
                hasMore = true,
            )

            // ----- Assert -----
            result shouldBe NOT_PLACED
        }
    }

    @Nested
    inner class PlaceTrailing {
        @Test
        fun `joins the current line when it fits`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 2)

            breaker.place(
                width = 50,
                hasMore = false,
            )

            // ----- Act -----
            val result = breaker.placeTrailing(width = 20)

            // ----- Assert -----
            result shouldBe 0
        }

        @Test
        fun `wraps to a new line when it does not fit`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 2)

            breaker.place(
                width = 50,
                hasMore = false,
            )

            // ----- Act -----
            val result = breaker.placeTrailing(width = 45)

            // ----- Assert -----
            result shouldBe 1
        }

        @Test
        fun `lands on the first line when nothing was placed`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 2)

            // ----- Act -----
            val result = breaker.placeTrailing(width = 20)

            // ----- Assert -----
            result shouldBe 0
        }

        @Test
        fun `ignores the line limit`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 1)

            breaker.place(
                width = 95,
                hasMore = false,
            )

            // ----- Act -----
            val result = breaker.placeTrailing(width = 20)

            // ----- Assert -----
            result shouldBe 1
        }
    }

    @Nested
    inner class FittingCount {
        @Test
        fun `returns zero for an empty line`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 1)

            // ----- Act -----
            val result = breaker.fittingCount(emptyList())

            // ----- Assert -----
            result shouldBe 0
        }

        @Test
        fun `keeps every item when they all fit beside the indicator`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 1)

            // ----- Act -----
            val result = breaker.fittingCount(listOf(20, 20))

            // ----- Assert -----
            result shouldBe 2
        }

        @Test
        fun `drops one trailing item when it crowds out the indicator`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 1)

            // ----- Act -----
            val result = breaker.fittingCount(listOf(30, 40))

            // ----- Assert -----
            result shouldBe 1
        }

        @Test
        fun `drops several trailing items until the indicator fits`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 1)

            // ----- Act -----
            val result = breaker.fittingCount(listOf(50, 40, 30))

            // ----- Assert -----
            result shouldBe 1
        }

        @Test
        fun `keeps the items when they plus gaps and indicator equal the max width`() {
            // ----- Arrange -----
            val breaker = breaker(maxLines = 1)

            // ----- Act -----
            val result = breaker.fittingCount(listOf(30, 30))

            // ----- Assert -----
            result shouldBe 2
        }

        @Test
        fun `returns zero when the indicator is wider than the line`() {
            // ----- Arrange -----
            val breaker = breaker(
                maxLines = 1,
                indicatorWidth = 101,
            )

            // ----- Act -----
            val result = breaker.fittingCount(listOf(10))

            // ----- Assert -----
            result shouldBe 0
        }
    }
}
