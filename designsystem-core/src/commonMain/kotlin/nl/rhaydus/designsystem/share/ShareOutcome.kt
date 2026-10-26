package nl.rhaydus.designsystem.share

/**
 * Result of a [ShareCardCapture] share. [Shared] means the platform share surface completed (a share sheet
 * destination was picked on Android/iOS); [CopiedToClipboard] means the image went to the clipboard
 * instead (desktop has no share surface); [Cancelled]
 * means the user dismissed the share surface without sharing (only platforms that report it — currently
 * iOS — return this; treat it as a no-op, not an error); [Failure] carries a human-readable [Failure.reason].
 */
sealed interface ShareOutcome {

    data object Shared : ShareOutcome

    data object CopiedToClipboard : ShareOutcome

    data object Cancelled : ShareOutcome

    data class Failure(
        val reason: String,
    ) : ShareOutcome
}
