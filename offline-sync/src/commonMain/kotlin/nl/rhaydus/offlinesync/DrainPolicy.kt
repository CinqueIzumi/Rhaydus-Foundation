package nl.rhaydus.offlinesync

/**
 * Tunes the drain engine's retry behaviour.
 *
 * - [maxAttempts] is the poison cap passed to [PendingWriteStore.getPending]: a write that has failed
 *   transiently this many times is no longer returned for replay.
 * - [inDrainRetries] is how many times a single write is replayed within one drain pass before its failure
 *   is classified (1 = no in-drain retry).
 * - [initialBackoffMs] / [backoffMultiplier] / [backoffCapMs] shape the exponential delay between those
 *   in-drain retries (the delay starts at [initialBackoffMs] and multiplies by [backoffMultiplier] each
 *   retry, capped at [backoffCapMs]).
 */
data class DrainPolicy(
    val maxAttempts: Int = 5,
    val inDrainRetries: Int = 1,
    val initialBackoffMs: Long = 250,
    val backoffMultiplier: Long = 2,
    val backoffCapMs: Long = Long.MAX_VALUE,
)
