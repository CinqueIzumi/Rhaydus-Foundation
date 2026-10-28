package nl.rhaydus.offlinesync

/**
 * The persistence seam for an offline write queue, implemented by the app (e.g. over Room / SQLite /
 * DataStore). It is the only thing the drain engine touches, so all storage concerns — the concrete table,
 * the payload↔row codec, and any coalescing policy (e.g. upsert-replacing writes of the same kind for the
 * same entity) — stay app-side.
 *
 * Contract the engine relies on:
 * - [getPending] returns every row **oldest-first** (replay preserves enqueue order), whatever its attempts;
 *   the engine applies the poison cap [DrainPolicy.maxAttempts] itself.
 * - [delete] removes a row that synced, was discarded or was terminally rejected.
 * - [incrementAttempts] bumps a row's failure count when a transient failure halts the drain, so it
 *   eventually crosses the poison cap.
 * - [abandon] is called for a write that reached the poison cap. It settles or undoes what the app tied to
 *   the write, then removes the row. The engine logs a failure and abandons the row again on a later pass,
 *   so the settle must be idempotent.
 */
interface PendingWriteStore<P> : WriteQueue<P> {
    suspend fun getPending(): List<PendingWrite<P>>

    suspend fun delete(localId: Long)

    suspend fun incrementAttempts(localId: Long)

    suspend fun abandon(write: PendingWrite<P>)
}
