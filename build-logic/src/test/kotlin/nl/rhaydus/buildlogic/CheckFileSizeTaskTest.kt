package nl.rhaydus.buildlogic

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class CheckFileSizeTaskTest {
    private lateinit var repoRoot: File

    @BeforeEach
    fun setUp() {
        repoRoot = createTempDirectory("check-file-size-").toFile()
    }

    @AfterEach
    fun tearDown() {
        repoRoot.deleteRecursively()
    }

    private fun lines(count: Int): String = "val x = 1\n".repeat(count)

    private fun write(
        relativePath: String,
        lineCount: Int,
    ) {
        repoRoot.resolve(relativePath).apply {
            parentFile.mkdirs()
            writeText(lines(lineCount))
        }
    }

    private fun runCheck(
        wholeModules: Set<String> = emptySet(),
        modules: List<String> = listOf("mod"),
        limit: Int = 3,
    ) {
        val root = ProjectBuilder.builder().withProjectDir(repoRoot).build()

        modules.forEach { name ->
            ProjectBuilder.builder()
                .withParent(root)
                .withName(name)
                .withProjectDir(repoRoot.resolve(name))
                .build()
        }

        root.pluginManager.apply(FileSizeConventionPlugin::class.java)

        root.extensions.getByType(FileSizeExtension::class.java).apply {
            maxLines.set(limit)
            this.wholeModules.set(wholeModules)
        }

        val task = root.tasks.getByName("checkFileSize") as CheckFileSizeTask

        task.rootDirectory.set(repoRoot)
        task.check()
    }

    @Nested
    inner class Limit {
        @Test
        fun `passes a file of exactly the limit`() {
            // ----- Arrange -----
            write(
                "mod/src/main/kotlin/presentation/A.kt",
                3,
            )

            // ----- Act & Assert -----
            shouldNotThrowAny { runCheck() }
        }

        @Test
        fun `fails a file one line over the limit`() {
            // ----- Arrange -----
            write(
                "mod/src/main/kotlin/presentation/A.kt",
                4,
            )

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> { runCheck() }

            exception.message shouldContain "mod/src/main/kotlin/presentation/A.kt  (4 lines)"
        }

        @Test
        fun `lists every offender sorted with root-relative paths and line counts`() {
            // ----- Arrange -----
            write(
                "mod/src/main/kotlin/presentation/Zed.kt",
                5,
            )

            write(
                "mod/src/main/kotlin/presentation/Alpha.kt",
                7,
            )

            write(
                "mod/src/main/kotlin/presentation/Fine.kt",
                2,
            )

            // ----- Act -----
            val exception = shouldThrow<GradleException> { runCheck() }

            // ----- Assert -----
            val message = exception.message.orEmpty()

            message shouldContain "  - mod/src/main/kotlin/presentation/Alpha.kt  (7 lines)\n" +
                "  - mod/src/main/kotlin/presentation/Zed.kt  (5 lines)"

            message shouldNotContain "Fine.kt"
            message shouldNotContain repoRoot.path
        }
    }

    @Nested
    inner class Scope {
        @Test
        fun `ignores an oversized file outside a presentation package`() {
            // ----- Arrange -----
            write(
                "mod/src/main/kotlin/data/Big.kt",
                10,
            )

            // ----- Act & Assert -----
            shouldNotThrowAny { runCheck() }
        }

        @Test
        fun `checks a file nested below a presentation segment`() {
            // ----- Arrange -----
            write(
                "mod/src/commonMain/kotlin/app/presentation/home/Big.kt",
                10,
            )

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> { runCheck() }

            exception.message shouldContain "Big.kt  (10 lines)"
        }

        @Test
        fun `does not treat a file name matching the segment as a presentation package`() {
            // ----- Arrange -----
            write(
                "mod/src/main/kotlin/data/presentation.kt",
                10,
            )

            // ----- Act & Assert -----
            shouldNotThrowAny { runCheck() }
        }

        @Test
        fun `checks every main file of a whole module`() {
            // ----- Arrange -----
            write(
                "wide/src/main/kotlin/data/Big.kt",
                10,
            )

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(
                    wholeModules = setOf(":wide"),
                    modules = listOf("wide"),
                )
            }

            exception.message shouldContain "wide/src/main/kotlin/data/Big.kt  (10 lines)"
        }

        @Test
        fun `scans both src main and src star Main source sets`() {
            // ----- Arrange -----
            write(
                "mod/src/main/kotlin/presentation/A.kt",
                10,
            )

            write(
                "mod/src/androidMain/kotlin/presentation/B.kt",
                10,
            )

            // ----- Act -----
            val exception = shouldThrow<GradleException> { runCheck() }

            // ----- Assert -----
            exception.message shouldContain "mod/src/main/kotlin/presentation/A.kt"
            exception.message shouldContain "mod/src/androidMain/kotlin/presentation/B.kt"
        }

        @Test
        fun `never scans test source sets even in a whole module`() {
            // ----- Arrange -----
            write(
                "wide/src/test/kotlin/presentation/T1.kt",
                10,
            )

            write(
                "wide/src/commonTest/kotlin/presentation/T2.kt",
                10,
            )

            write(
                "wide/src/androidHostTest/kotlin/presentation/T3.kt",
                10,
            )

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(
                    wholeModules = setOf(":wide"),
                    modules = listOf("wide"),
                )
            }
        }
    }
}
