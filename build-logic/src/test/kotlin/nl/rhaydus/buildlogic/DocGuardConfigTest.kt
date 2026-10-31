package nl.rhaydus.buildlogic

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class DocGuardConfigTest {
    private lateinit var directory: File

    @BeforeEach
    fun setUp() {
        directory = createTempDirectory("doc-guard-config-").toFile()
    }

    @AfterEach
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun configWith(json: String): File = directory.resolve("rhaydus.json").apply { writeText(json) }

    @Nested
    inner class Load {
        @Test
        fun `switches the gate off when the file is missing`() {
            // ----- Arrange -----
            val missing = directory.resolve("absent.json")

            // ----- Act -----
            val config = DocGuardConfig.load(missing)

            // ----- Assert -----
            config.isEnabled.shouldBeFalse()
        }

        @Test
        fun `switches the gate off when the docGuard key is missing`() {
            // ----- Arrange -----
            val file = configWith("""{ "other": { "budgetsFile": "docs/budgets.txt" } }""")

            // ----- Act -----
            val config = DocGuardConfig.load(file)

            // ----- Assert -----
            config.isEnabled.shouldBeFalse()
        }

        @Test
        fun `switches the gate off when the root is not an object`() {
            // ----- Arrange -----
            val file = configWith("""["docGuard"]""")

            // ----- Act -----
            val config = DocGuardConfig.load(file)

            // ----- Assert -----
            config.isEnabled.shouldBeFalse()
        }

        @Test
        fun `parses every docGuard field`() {
            // ----- Arrange -----
            val file = configWith(
                """
                {
                  "docGuard": {
                    "budgetsFile": "docs/doc-budgets.txt",
                    "activeFile": "docs/working/ACTIVE.md",
                    "workingDocs": ["docs/working/", "notes/plan.md"],
                    "permanentDocs": ["docs/reference/"],
                    "decisionPattern": "\\bDEC-[0-9]+\\b",
                    "stepPattern": "\\bSTEP-[0-9]+\\b"
                  }
                }
                """.trimIndent(),
            )

            // ----- Act -----
            val config = DocGuardConfig.load(file)

            // ----- Assert -----
            config shouldBe DocGuardConfig(
                budgetsFile = "docs/doc-budgets.txt",
                activeFile = "docs/working/ACTIVE.md",
                workingDocs = listOf("docs/working/", "notes/plan.md"),
                permanentDocs = listOf("docs/reference/"),
                decisionPattern = """\bDEC-[0-9]+\b""",
                stepPattern = """\bSTEP-[0-9]+\b""",
            )

            config.isEnabled.shouldBeTrue()
        }

        @Test
        fun `ignores non-string entries in a doc list`() {
            // ----- Arrange -----
            val file = configWith("""{ "docGuard": { "permanentDocs": ["docs/", 7, null, "README.md"] } }""")

            // ----- Act -----
            val config = DocGuardConfig.load(file)

            // ----- Assert -----
            config.permanentDocs shouldBe listOf("docs/", "README.md")
        }

        @Test
        fun `switches the gate on for an empty docGuard object`() {
            // ----- Arrange -----
            val file = configWith("""{ "docGuard": {} }""")

            // ----- Act -----
            val config = DocGuardConfig.load(file)

            // ----- Assert -----
            config.isEnabled.shouldBeTrue()
        }

        @Test
        fun `keeps the gate on when only some fields are present`() {
            // ----- Arrange -----
            val file = configWith("""{ "docGuard": { "budgetsFile": "b.txt" } }""")

            // ----- Act -----
            val config = DocGuardConfig.load(file)

            // ----- Assert -----
            config.isEnabled.shouldBeTrue()
            config.budgetsFile shouldBe "b.txt"
            config.workingDocs shouldBe emptyList()
        }
    }
}
