package nl.rhaydus.detekt

import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.KotlinEnvironmentContainer
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NoReflectionTest {
    private val rule = NoReflection(Config.empty)

    private fun lintWithTypes(body: String) = rule.lintWithContext(
        env,
        body,
    )

    @Nested
    inner class ResolvedLookups {
        @Test
        fun `flags a Class forName call`() {
            // ----- Arrange -----
            val code = """
            fun load() = Class.forName("a.B")
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }

        @Test
        fun `flags a ClassLoader loadClass call`() {
            // ----- Arrange -----
            val code = """
            fun load(loader: ClassLoader) = loader.loadClass("a.B")
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }

        @Test
        fun `flags getDeclaredMethod on a Class value`() {
            // ----- Arrange -----
            val code = """
            class Foo
            fun look(type: Class<Foo>) = type.getDeclaredMethod("bar")
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }

        @Test
        fun `flags getMethod on a Class value`() {
            // ----- Arrange -----
            val code = """
            class Foo
            fun look(type: Class<Foo>) = type.getMethod("bar")
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }

        @Test
        fun `flags getDeclaredField on a Class value`() {
            // ----- Arrange -----
            val code = """
            class Foo
            fun look(type: Class<Foo>) = type.getDeclaredField("bar")
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }

        @Test
        fun `flags getField on a Class value`() {
            // ----- Arrange -----
            val code = """
            class Foo
            fun look(type: Class<Foo>) = type.getField("bar")
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }

        @Test
        fun `flags getDeclaredConstructor on a Class value`() {
            // ----- Arrange -----
            val code = """
            class Foo
            fun look(type: Class<Foo>) = type.getDeclaredConstructor()
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }

        @Test
        fun `flags getConstructor on a Class value`() {
            // ----- Arrange -----
            val code = """
            class Foo
            fun look(type: Class<Foo>) = type.getConstructor()
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }

        @Test
        fun `flags a lookup through an aliased Class import`() {
            // ----- Arrange -----
            val code = """
            import java.lang.Class as C
            
            fun load() = C.forName("a.B")
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }

        @Test
        fun `flags a receiver-less lookup on an implicit Class receiver`() {
            // ----- Arrange -----
            val code = """
            class Foo
            fun Class<Foo>.look() = getDeclaredMethod("bar")
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }
    }

    @Nested
    inner class ProjectMembersWithReflectiveNames {
        @Test
        fun `ignores a project method named getField`() {
            // ----- Arrange -----
            val code = """
            class Holder {
                fun getField(name: String) = name
            }
            fun look() = Holder().getField("a")
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertTrue(findings.isEmpty())
        }

        @Test
        fun `ignores a project method named getMethod`() {
            // ----- Arrange -----
            val code = """
            class Holder {
                fun getMethod(name: String) = name
            }
            fun look() = Holder().getMethod("a")
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertTrue(findings.isEmpty())
        }

        @Test
        fun `ignores a project method named loadClass`() {
            // ----- Arrange -----
            val code = """
            class Loader {
                fun loadClass(name: String) = name
            }
            fun look() = Loader().loadClass("a")
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertTrue(findings.isEmpty())
        }
    }

    @Nested
    inner class ReceiverMatching {
        @Test
        fun `ignores forName on an unrelated receiver`() {
            // ----- Arrange -----
            val code = """
            fun load(registry: Registry) = registry.forName("a")
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertTrue(findings.isEmpty())
        }
    }

    @Nested
    inner class KotlinReflectPackages {
        @Test
        fun `flags a kotlin reflect full import`() {
            // ----- Arrange -----
            val code = """
            import kotlin.reflect.full.memberProperties
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }

        @Test
        fun `flags a kotlin reflect jvm import`() {
            // ----- Arrange -----
            val code = """
            import kotlin.reflect.jvm.javaMethod
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }

        @Test
        fun `flags a fully qualified kotlin reflect full use without an import`() {
            // ----- Arrange -----
            val code = """
            fun look() = kotlin.reflect.full.foo()
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertEquals(
                1,
                findings.size,
            )
        }

        @Test
        fun `ignores a kotlin reflect import outside full and jvm`() {
            // ----- Arrange -----
            val code = """
            import kotlin.reflect.KClass
            """.trimIndent()

            // ----- Act -----
            val findings = lintWithTypes(code)

            // ----- Assert -----
            assertTrue(findings.isEmpty())
        }
    }

    companion object {
        private lateinit var env: KotlinEnvironmentContainer

        @BeforeAll
        @JvmStatic
        fun setUp() {
            env = createEnvironment()
        }
    }
}
