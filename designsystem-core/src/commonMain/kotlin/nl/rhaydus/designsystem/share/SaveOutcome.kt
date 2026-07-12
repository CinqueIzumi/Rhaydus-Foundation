package nl.rhaydus.designsystem.share

/**
 * Result of a [ShareCardCapture] save. [Cached] is a private-cache write (its [Cached.identifier] is a
 * shareable handle — a content URI on Android, a file path elsewhere); [Saved] is a gallery write with a
 * user-visible [Saved.displayPath]; [Failure] carries a human-readable [Failure.reason].
 */
sealed interface SaveOutcome {

    data class Cached(
        val identifier: String,
    ) : SaveOutcome

    data class Saved(
        val identifier: String,
        val displayPath: String,
    ) : SaveOutcome

    data class Failure(
        val reason: String,
    ) : SaveOutcome
}
