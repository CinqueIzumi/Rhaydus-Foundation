package nl.rhaydus.designsystem.layout

import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BottomBarScaffoldTest {
    @Nested
    inner class BottomBarContentPadding {
        @Test
        fun `overlay placement adds measured footprint plus spacing — inset counted once`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = bottomBarContentPadding(
                barFootprint = 56.dp,
                barSpacing = 16.dp,
            )

            // ----- Assert -----
            result shouldBe 72.dp
        }

        @Test
        fun `docked placement reserves only the breathing gap — scaffold innerPadding covers the bar itself`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = bottomBarContentPadding(
                barFootprint = 0.dp,
                barSpacing = 16.dp,
            )

            // ----- Assert -----
            result shouldBe 16.dp
        }

        @Test
        fun `overlay placement with no breathing gap reserves the footprint alone — bottom-anchored chrome such as a player strip`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = bottomBarContentPadding(
                barFootprint = 80.dp,
                barSpacing = 0.dp,
            )

            // ----- Assert -----
            result shouldBe 80.dp
        }
    }
}
