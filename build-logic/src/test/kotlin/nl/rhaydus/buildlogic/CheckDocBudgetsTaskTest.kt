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

class CheckDocBudgetsTaskTest {
    private lateinit var repoRoot: File

    @BeforeEach
    fun setUp() {
        repoRoot = createTempDirectory("check-doc-budgets-").toFile()
    }

    @AfterEach
    fun tearDown() {
        repoRoot.deleteRecursively()
    }

    private fun git(vararg args: String) {
        val process = ProcessBuilder(listOf("git") + args)
            .directory(repoRoot)
            .redirectErrorStream(true)
            .start()
        process.inputStream.readBytes()
        check(process.waitFor() == 0) { "git ${args.joinToString(" ")} failed in $repoRoot" }
    }

    private fun initRepo(branch: String = "main") {
        git("init", "-b", branch)
        git("config", "user.email", "test@example.com")
        git("config", "user.name", "Test")
        git("config", "commit.gpgsign", "false")
    }

    private fun commitAll() {
        git("add", "-A")
        git("commit", "-m", "baseline")
    }

    private fun write(
        relativePath: String,
        content: String,
    ): File = repoRoot.resolve(relativePath).apply {
        parentFile.mkdirs()
        writeText(content)
    }

    private fun repoWith(vararg files: Pair<String, String>) {
        initRepo()
        write(".gitkeep", "")
        files.forEach { (path, content) -> write(path, content) }
        commitAll()
    }

    private fun runCheck(
        budgets: String? = null,
        markdown: List<String> = emptyList(),
        active: String? = null,
        config: List<String> = emptyList(),
        permanentDocs: List<String> = listOf("docs/reference/", ".claude/agent-memory/"),
    ) {
        val project = ProjectBuilder.builder().withProjectDir(repoRoot).build()
        val task = project.tasks.create("checkDocBudgets", CheckDocBudgetsTask::class.java)
        if (budgets != null) task.budgetsFile.set(write("docs/doc-budgets.txt", budgets))
        if (active != null) task.activeFile.set(write("docs/working/ACTIVE.md", active))
        task.workingDocs.set(listOf("docs/working/"))
        task.permanentDocs.set(permanentDocs)
        task.decisionPattern.set(DocGuardConfig.DEFAULT_DECISION_PATTERN)
        task.stepPattern.set(DocGuardConfig.DEFAULT_STEP_PATTERN)
        task.defaultBranch.set("main")
        task.repoRoot.set(repoRoot)
        task.markdownFiles.setFrom(markdown.map { repoRoot.resolve(it) })
        task.configFiles.setFrom(config.map { repoRoot.resolve(it) })
        task.check()
    }

    @Nested
    inner class FileBudgets {
        @Test
        fun `fails a new file over its byte budget`() {
            // ----- Arrange -----
            repoWith()
            write("fixture/new.md", "12345678901")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(budgets = "fixture/new.md 10B", markdown = listOf("fixture/new.md"))
            }
            exception.message shouldContain "new file over budget"
        }

        @Test
        fun `passes a file exactly at its byte budget`() {
            // ----- Arrange -----
            repoWith("fixture/kb.md" to "a".repeat(1024))

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(budgets = "fixture/kb.md 1KB", markdown = listOf("fixture/kb.md"))
            }
        }

        @Test
        fun `passes a file exactly at its line budget`() {
            // ----- Arrange -----
            repoWith("fixture/lines.md" to "one\ntwo\nthree\n")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(budgets = "fixture/lines.md 3L", markdown = listOf("fixture/lines.md"))
            }
        }

        @Test
        fun `fails a file one line over its line budget`() {
            // ----- Arrange -----
            repoWith()
            write("fixture/lines.md", "one\ntwo\nthree\nfour\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(budgets = "fixture/lines.md 3L", markdown = listOf("fixture/lines.md"))
            }
            exception.message shouldContain "fixture/lines.md"
        }

        @Test
        fun `applies the double-star rule rather than the single-star rule to a nested path`() {
            // ----- Arrange -----
            repoWith()
            write("docs/reference/sub/deep.md", "x".repeat(25))
            val budgets = "docs/reference/*.md 100B\ndocs/reference/**/*.md 20B"

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(budgets = budgets, markdown = listOf("docs/reference/sub/deep.md"))
            }
            exception.message shouldContain "docs/reference/sub/deep.md"
        }

        @Test
        fun `applies the first matching rule when several globs match`() {
            // ----- Arrange -----
            repoWith()
            write("fixture/special/exact.md", "1234567890")
            val budgets = "fixture/special/exact.md 8B\nfixture/special/*.md 1000B"

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(budgets = budgets, markdown = listOf("fixture/special/exact.md"))
            }
            exception.message shouldContain "fixture/special/exact.md"
        }

        @Test
        fun `passes a file no rule matches`() {
            // ----- Arrange -----
            repoWith()
            write("fixture/other.md", "x".repeat(5000))

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(budgets = "fixture/tiny.md 10B", markdown = listOf("fixture/other.md"))
            }
        }

        @Test
        fun `skips the budget checks when no budgets file is configured`() {
            // ----- Arrange -----
            repoWith("fixture/big.md" to "x".repeat(5000))

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(budgets = null, markdown = listOf("fixture/big.md"))
            }
        }
    }

    @Nested
    inner class Ratchet {
        @Test
        fun `passes a file over budget but unchanged since the merge-base`() {
            // ----- Arrange -----
            repoWith("fixture/tiny.md" to "12345678901234")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(budgets = "fixture/tiny.md 10B", markdown = listOf("fixture/tiny.md"))
            }
        }

        @Test
        fun `passes a file over budget that shrank since the merge-base`() {
            // ----- Arrange -----
            repoWith("fixture/tiny.md" to "12345678901234")
            write("fixture/tiny.md", "123456789012")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(budgets = "fixture/tiny.md 10B", markdown = listOf("fixture/tiny.md"))
            }
        }

        @Test
        fun `fails a file over budget that grew since the merge-base`() {
            // ----- Arrange -----
            repoWith("fixture/tiny.md" to "12345678901234")
            write("fixture/tiny.md", "1234567890123456789")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(budgets = "fixture/tiny.md 10B", markdown = listOf("fixture/tiny.md"))
            }
            exception.message shouldContain "grew since merge-base"
        }

        @Test
        fun `measures growth in lines for a line budget`() {
            // ----- Arrange -----
            repoWith("fixture/lines.md" to "1\n2\n3\n4\n")
            write("fixture/lines.md", "1\n2\n3\n4\n5\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(budgets = "fixture/lines.md 3L", markdown = listOf("fixture/lines.md"))
            }
            exception.message shouldContain "4 L -> 5 L"
        }

        @Test
        fun `fails when no merge-base can be resolved`() {
            // ----- Arrange -----
            initRepo(branch = "trunk")
            write("fixture/tiny.md", "x")
            commitAll()

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(budgets = "fixture/tiny.md 10B", markdown = listOf("fixture/tiny.md"))
            }
            exception.message shouldContain "cannot resolve a git merge-base"
        }
    }

    @Nested
    inner class NowSection {
        private val tracker = "docs/working/tracker.md"

        @Test
        fun `fails when the Now section grows past its budget`() {
            // ----- Arrange -----
            repoWith(tracker to "# Tracker\n\n## Now\n- a\n\n## Later\n- z\n")
            write(tracker, "# Tracker\n\n## Now\n- a\n- b\n- c\n\n## Later\n- z\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(budgets = "## Now 3L", active = "$tracker\n")
            }
            exception.message shouldContain "$tracker ## Now section"
        }

        @Test
        fun `passes when the Now section is over budget but unchanged`() {
            // ----- Arrange -----
            repoWith(tracker to "# Tracker\n\n## Now\n- a\n- b\n- c\n- d\n\n## Later\n- z\n")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(budgets = "## Now 3L", active = "$tracker\n")
            }
        }

        @Test
        fun `ignores growth outside the Now section`() {
            // ----- Arrange -----
            repoWith(tracker to "# Tracker\n\n## Now\n- a\n\n## Later\n- z\n")
            write(tracker, "# Tracker\n\n## Now\n- a\n\n## Later\n- z\n- y\n- x\n- w\n- v\n")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(budgets = "## Now 3L", active = "$tracker\n")
            }
        }

        @Test
        fun `fails a new tracker whose Now section is over budget`() {
            // ----- Arrange -----
            repoWith()
            write(tracker, "## Now\n- a\n- b\n- c\n- d\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(budgets = "## Now 3L", active = "$tracker\n")
            }
            exception.message shouldContain "$tracker ## Now section"
        }
    }

    @Nested
    inner class MarkdownCitations {
        @Test
        fun `flags a permanent doc that cites a working-doc plan directory`() {
            // ----- Arrange -----
            repoWith("docs/reference/foo.md" to "# Foo\n\nSee docs/working/275-foo/step-1.md for details.\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(markdown = listOf("docs/reference/foo.md"))
            }
            exception.message shouldContain "docs/reference/foo.md:3: cites a plan directory"
        }

        @Test
        fun `flags a permanent doc that cites a decision number`() {
            // ----- Arrange -----
            repoWith("docs/reference/foo.md" to "# Foo\n\nChosen in D17.\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(markdown = listOf("docs/reference/foo.md"))
            }
            exception.message shouldContain "cites a decision number"
        }

        @Test
        fun `flags a permanent doc that cites a plan step`() {
            // ----- Arrange -----
            repoWith("docs/reference/foo.md" to "# Foo\n\nThis implements S4-5b.\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(markdown = listOf("docs/reference/foo.md"))
            }
            exception.message shouldContain "cites a plan step"
        }

        @Test
        fun `flags an entry under a listed prefix outside docs`() {
            // ----- Arrange -----
            val note = ".claude/agent-memory/reviewer/new_note.md"
            repoWith(note to "# Notes\n\nSee S5-4 for details.\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(markdown = listOf(note))
            }
            exception.message shouldContain "cites a plan step"
        }

        @Test
        fun `flags a permanent doc that cites a decision number or a plan step on one line`() {
            // ----- Arrange -----
            repoWith("docs/reference/foo.md" to "# Foo\n\nD3 and S1-2.\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(markdown = listOf("docs/reference/foo.md"))
            }
            exception.message shouldContain "cites a decision number"
            exception.message shouldContain "cites a plan step"
        }

        @Test
        fun `passes a token that only resembles a step id`() {
            // ----- Arrange -----
            repoWith("docs/reference/foo.md" to "# Foo\n\nThis references S3 elsewhere.\n")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(markdown = listOf("docs/reference/foo.md"))
            }
        }

        @Test
        fun `passes a permanent doc that references the top-level tracker file`() {
            // ----- Arrange -----
            repoWith("docs/reference/foo.md" to "# Foo\n\nSee docs/working/ACTIVE.md for state.\n")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(markdown = listOf("docs/reference/foo.md"))
            }
        }

        @Test
        fun `passes a doc that is not listed as permanent`() {
            // ----- Arrange -----
            repoWith("docs/working/notes.md" to "See docs/working/275-foo/step-1.md, D3 and S4-1.\n")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(markdown = listOf("docs/working/notes.md"))
            }
        }

        @Test
        fun `switches the citation checks off when no permanent docs are listed`() {
            // ----- Arrange -----
            repoWith("docs/reference/foo.md" to "# Foo\n\nChosen in D17.\n")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(markdown = listOf("docs/reference/foo.md"), permanentDocs = emptyList())
            }
        }

        @Test
        fun `matches a single-file permanent entry by exact path only`() {
            // ----- Arrange -----
            repoWith(
                "README.md" to "Chosen in D17.\n",
                "README.md.bak.md" to "Chosen in D17.\n",
            )

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(
                    markdown = listOf("README.md", "README.md.bak.md"),
                    permanentDocs = listOf("README.md"),
                )
            }
            exception.message shouldContain "README.md:1"
            exception.message shouldNotContain "README.md.bak.md"
        }
    }

    @Nested
    inner class ConfigComments {
        @Test
        fun `flags a yml comment citing working docs`() {
            // ----- Arrange -----
            val path = "config/detekt/detekt.yml"
            repoWith(path to "rule:\n  active: true  # see docs/working/275-foo/notes.md\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(config = listOf(path))
            }
            exception.message shouldContain "$path:2: comment cites working docs"
        }

        @Test
        fun `flags a yml comment citing a decision number`() {
            // ----- Arrange -----
            val path = "config/detekt/detekt.yml"
            repoWith(path to "rule:\n  active: true  # per D17\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(config = listOf(path))
            }
            exception.message shouldContain "comment cites a decision number"
        }

        @Test
        fun `flags a yaml comment citing a plan step`() {
            // ----- Arrange -----
            val path = "ci/workflow.yaml"
            repoWith(path to "# implements S4-1\njobs: {}\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(config = listOf(path))
            }
            exception.message shouldContain "comment cites a plan step"
        }

        @Test
        fun `flags a kts line comment citing a plan step`() {
            // ----- Arrange -----
            repoWith("build.gradle.kts" to "val x = 1 // implements S4-1\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(config = listOf("build.gradle.kts"))
            }
            exception.message shouldContain "comment cites a plan step"
        }

        @Test
        fun `flags a kts block comment citing a decision number`() {
            // ----- Arrange -----
            repoWith("build.gradle.kts" to "val x = 1 /* per D17 */\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(config = listOf("build.gradle.kts"))
            }
            exception.message shouldContain "comment cites a decision number"
        }

        @Test
        fun `flags a kts KDoc line citing a working doc`() {
            // ----- Arrange -----
            repoWith("build.gradle.kts" to "/**\n * See docs/working/ACTIVE.md\n */\nval x = 1\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(config = listOf("build.gradle.kts"))
            }
            exception.message shouldContain "build.gradle.kts:2: comment cites working docs"
        }

        @Test
        fun `passes a kts string literal holding a decision-number-like token`() {
            // ----- Arrange -----
            repoWith("build.gradle.kts" to "val doc = \"https://example.com/D17\"\nval note = \"see S4-1\"\n")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(config = listOf("build.gradle.kts"))
            }
        }

        @Test
        fun `flags a real trailing comment after a kts string literal`() {
            // ----- Arrange -----
            repoWith("build.gradle.kts" to "val label = \"https://example.com/x\" // implements S4-1\n")

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> {
                runCheck(config = listOf("build.gradle.kts"))
            }
            exception.message shouldContain "comment cites a plan step"
        }

        @Test
        fun `passes a yml hash inside a quoted value`() {
            // ----- Arrange -----
            repoWith("config/a.yml" to "key: \"a # D17\"\nother: 'it''s # D17'\n")

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(config = listOf("config/a.yml"))
            }
        }

        @Test
        fun `passes a config file with an extension the scan does not recognise`() {
            // ----- Arrange -----
            repoWith(
                "config/notes.txt" to "Plan D17 lives here\n",
                "config/tool/Extra.kt" to "val x = 1 // implements S4-1\n",
            )

            // ----- Act & Assert -----
            shouldNotThrowAny {
                runCheck(config = listOf("config/notes.txt", "config/tool/Extra.kt"))
            }
        }
    }

    @Nested
    inner class PluginWiring {
        @Test
        fun `reads the gate from the project config file`() {
            // ----- Arrange -----
            repoWith(
                ".claude/rhaydus.json" to """
                    {
                      "docGuard": {
                        "budgetsFile": "docs/doc-budgets.txt",
                        "workingDocs": ["docs/working/"],
                        "permanentDocs": ["docs/reference/"]
                      }
                    }
                """.trimIndent(),
                "docs/doc-budgets.txt" to "docs/reference/*.md 1KB",
                "docs/reference/foo.md" to "# Foo\n\nChosen in D17.\n",
            )
            val project = ProjectBuilder.builder().withProjectDir(repoRoot).build()
            project.pluginManager.apply("rhaydus.doc-budgets")
            val task = project.tasks.getByName("checkDocBudgets") as CheckDocBudgetsTask

            // ----- Act & Assert -----
            val exception = shouldThrow<GradleException> { task.check() }
            exception.message shouldContain "docs/reference/foo.md:3: cites a decision number"
        }

        @Test
        fun `does nothing when the project has no config file`() {
            // ----- Arrange -----
            repoWith("docs/reference/foo.md" to "# Foo\n\nChosen in D17.\n")
            val project = ProjectBuilder.builder().withProjectDir(repoRoot).build()
            project.pluginManager.apply("rhaydus.doc-budgets")
            val task = project.tasks.getByName("checkDocBudgets") as CheckDocBudgetsTask

            // ----- Act & Assert -----
            shouldNotThrowAny { task.check() }
        }
    }
}
