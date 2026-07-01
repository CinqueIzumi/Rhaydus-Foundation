package nl.rhaydus.offlinesync

/**
 * The result of successfully replaying a pending write against the server. [SYNCED] means the write was
 * applied and its entity may need reconciling (it contributes a hint, see [OfflineWriteDrainer.drain]);
 * [DISCARDED] means the write was intentionally dropped without a server change (e.g. an unprocessable or
 * now-moot payload) — the row is deleted but no reconciliation hint is recorded. A replay that instead
 * *throws* is classified by the drainer as transient (halt-and-retry) or terminal (discard).
 */
enum class ReplayOutcome {
    SYNCED,
    DISCARDED,
}
