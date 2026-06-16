package nl.rhaydus.designsystem.editorial

import androidx.compose.material3.Typography
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EditorialTypographyTest {
    @Nested
    inner class BuildEditorialTypography {
        private lateinit var base: Typography

        @BeforeEach
        fun setUp() {
            base = Typography()
        }

        @Test
        fun `eyebrow maps to labelMedium`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = buildEditorialTypography(base)

            // ----- Assert -----
            result.eyebrow shouldBe base.labelMedium
        }

        @Test
        fun `eyebrowSmall maps to labelSmall`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = buildEditorialTypography(base)

            // ----- Assert -----
            result.eyebrowSmall shouldBe base.labelSmall
        }

        @Test
        fun `pageTitle maps to displaySmall`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = buildEditorialTypography(base)

            // ----- Assert -----
            result.pageTitle shouldBe base.displaySmall
        }

        @Test
        fun `headline maps to headlineMedium`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = buildEditorialTypography(base)

            // ----- Assert -----
            result.headline shouldBe base.headlineMedium
        }

        @Test
        fun `title maps to titleLarge`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = buildEditorialTypography(base)

            // ----- Assert -----
            result.title shouldBe base.titleLarge
        }

        @Test
        fun `body maps to bodyMedium`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = buildEditorialTypography(base)

            // ----- Assert -----
            result.body shouldBe base.bodyMedium
        }

        @Test
        fun `bodySmall maps to bodySmall`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = buildEditorialTypography(base)

            // ----- Assert -----
            result.bodySmall shouldBe base.bodySmall
        }

        @Test
        fun `statLarge equals displayMedium with tabular figures applied`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = buildEditorialTypography(base)

            // ----- Assert -----
            result.statLarge shouldBe base.displayMedium.copy(fontFeatureSettings = TABULAR_FIGURES)
        }

        @Test
        fun `statHero equals displayLarge with tabular figures applied`() {
            // ----- Arrange -----

            // ----- Act -----
            val result = buildEditorialTypography(base)

            // ----- Assert -----
            result.statHero shouldBe base.displayLarge.copy(fontFeatureSettings = TABULAR_FIGURES)
        }
    }
}
