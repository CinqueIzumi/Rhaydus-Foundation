package nl.rhaydus.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope

private const val FIXTURES = "konsist-rules/src/test/resources"

internal fun files(vararg names: String): KoScope {
    return Konsist.scopeFromFiles(names.map { "$FIXTURES/$it" })
}

internal fun directory(name: String): KoScope = Konsist.scopeFromDirectory("$FIXTURES/$name")
