package nl.rhaydus.common

import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Message
import co.touchlab.kermit.MessageStringFormatter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Tag
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import kotlin.concurrent.Volatile

/**
 * Brand-agnostic, app-wide logging facade. Backed by Kermit so it logs on every Kotlin target (Logcat
 * on Android, os_log on iOS, stdout on JVM); call sites stay platform-agnostic.
 *
 * Named `AppLog` rather than `Log` to avoid colliding with `android.util.Log` in reading and review.
 * Call [install] once at startup with the app's tag to enable output (typically debug builds only,
 * mirroring a Timber-debug-tree setup); until then — and in release, when `debug = false` — no writer
 * is installed, so every call is a no-op. The tag and the line [prefix] are caller-supplied, so this
 * carries no app branding.
 */
object AppLog {
    private const val DEFAULT_TAG = "App"
    private const val DEFAULT_PREFIX = "-=-"

    @Volatile
    private var logger: Logger = Logger(
        config = loggerConfigInit(),
        tag = DEFAULT_TAG,
    )

    /**
     * Enables logging output under [tag]. When [debug] is false no writer is installed, so every call
     * is a no-op (release behaviour). [prefix] is prepended to each emitted line for grep-ability.
     */
    fun install(
        tag: String,
        debug: Boolean,
        prefix: String = DEFAULT_PREFIX,
    ) {
        logger = buildLogger(
            tag = tag,
            prefix = prefix,
            debug = debug,
        )
    }

    fun i(message: String) = logger.i { message }

    fun i(
        throwable: Throwable,
        message: String,
    ) = logger.i(throwable) { message }

    fun w(message: String) = logger.w { message }

    fun w(
        throwable: Throwable,
        message: String,
    ) = logger.w(throwable) { message }

    fun e(message: String) = logger.e { message }

    fun e(
        throwable: Throwable,
        message: String,
    ) = logger.e(throwable) { message }

    fun e(throwable: Throwable) = logger.e(throwable) { throwable.message ?: throwable.toString() }

    private fun buildLogger(
        tag: String,
        prefix: String,
        debug: Boolean,
    ): Logger = Logger(
        config = if (debug) {
            loggerConfigInit(platformLogWriter(PrefixFormatter(prefix)))
        } else {
            loggerConfigInit()
        },
        tag = tag,
    )

    private class PrefixFormatter(private val prefix: String) : MessageStringFormatter {
        override fun formatSeverity(severity: Severity): String =
            DefaultFormatter.formatSeverity(severity)

        override fun formatTag(tag: Tag): String = DefaultFormatter.formatTag(tag)

        override fun formatMessage(
            severity: Severity?,
            tag: Tag?,
            message: Message,
        ): String {
            val formatted = DefaultFormatter.formatMessage(
                severity,
                tag,
                message,
            )

            return formatted
                .lineSequence()
                .joinToString(separator = "\n") { line -> "$prefix $line" }
        }
    }
}
