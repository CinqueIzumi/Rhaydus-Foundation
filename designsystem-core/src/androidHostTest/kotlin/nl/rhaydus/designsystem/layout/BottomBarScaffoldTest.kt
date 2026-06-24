package nl.rhaydus.designsystem.layout

import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class BottomBarScaffoldTest {
    @Nested
    inner class BottomBarContentPadding {
        @Test
        fun `returns footprint plus spacing — inset counted once`() {
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
        fun `returns spacing only when footprint is zero — first frame reserve`() {
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
        fun `returns footprint only when spacing is zero`() {
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
