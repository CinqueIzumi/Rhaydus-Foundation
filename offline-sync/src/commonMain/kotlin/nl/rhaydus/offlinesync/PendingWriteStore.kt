package nl.rhaydus.offlinesync

/**
 * The persistence seam for an offline write queue, implemented by the app (e.g. over Room / SQLite /
 * DataStore). It is the only thing the drain engine touches, so all storage concerns — the concrete table,
 * the payload↔row codec, and any coalescing policy (e.g. upsert-replacing writes of the same kind for the
 * same entity) — stay app-side.
 *
 * Contract the engine relies on:
 * - [getPending] returns the un-poisoned pending writes **oldest-first** (replay preserves enqueue order),
 *   excluding any whose `attempts >= maxAttempts` (the poison cap).
 * - [delete] removes a row once it is synced or terminally rejected; [incrementAttempts] bumps a row's
 *   failure count when a transient failure halts the drain, so it eventually crosses the poison cap.
 */
interface PendingWriteStore<P> : WriteQueue<P> {
    suspend fun getPending(maxAttempts: Int): List<PendingWrite<P>>

    suspend fun delete(localId: Long)

    suspend fun incrementAttempts(localId: Long)
}
