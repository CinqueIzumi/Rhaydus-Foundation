package nl.rhaydus.offlinesync

import kotlinx.coroutines.CoroutineScope

/**
 * Replays queued offline writes against the server. [start] launches a long-lived job on [scope] that drains
 * whenever connectivity returns (and once immediately if already online); [drain] runs one drain pass on
 * demand (e.g. before a fresh server fetch) and returns the **reconciliation hints**: for each affected
 * entity id, the set of write kinds that were successfully synced. Callers use the hints to preserve only
 * the fields each replayed write owns when merging against a fresh fetch, so a value the server changed
 * underneath — that no pending write touched — is not clobbered by the local optimistic copy.
 *
 * `I` is the entity-id type, `K` the write-kind type; both are the app's — the engine only groups them.
 */
interface OfflineWriteDrainer<I, K> {
    fun start(scope: CoroutineScope)

    suspend fun drain(): Map<I, Set<K>>
}
