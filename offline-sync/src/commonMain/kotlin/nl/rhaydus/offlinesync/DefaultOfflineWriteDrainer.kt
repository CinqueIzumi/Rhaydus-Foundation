package nl.rhaydus.offlinesync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.rhaydus.common.AppDispatchers
import nl.rhaydus.common.AppLog
import nl.rhaydus.common.runCatchingCancellable
import nl.rhaydus.common.runCatchingLogged
import nl.rhaydus.platform.NetworkAvailabilityProvider

/**
 * The reusable offline drain engine. It reads pending writes from [store] oldest-first and replays each via
 * the app-supplied [replay] lambda, deleting a row once it is [ReplayOutcome.SYNCED] / [ReplayOutcome.DISCARDED]
 * and recording a reconciliation hint (via [hintKey]) for synced writes. A replay that throws is classified
 * by [isTransient]: a **transient** failure bumps the row's attempt count and **halts the whole drain**
 * (preserving enqueue order until the network settles or the row crosses the poison cap
 * [DrainPolicy.maxAttempts]); a **terminal** failure deletes the row (the server processed and rejected it).
 * In-drain retries and their exponential backoff come from [policy].
 *
 * [start] wires an online-triggered drain onto [scope] (drains on every connectivity return, and once
 * immediately if already online), serialized with the on-demand [drain] by a single mutex. Connectivity-
 * triggered drains are resilient: a failure is logged, not allowed to kill the long-lived collector.
 *
 * [isTransient] defaults to treating **every** replay failure as transient (halt-and-retry, never discard) —
 * the data-safe default. Apps that can recognise a terminal failure (e.g. a 4xx the server will always
 * reject) should override it so such writes are discarded rather than churning against the poison cap.
 */
class DefaultOfflineWriteDrainer<P, I, K>(
    private val store: PendingWriteStore<P>,
    private val networkAvailability: NetworkAvailabilityProvider,
    private val dispatchers: AppDispatchers,
    private val replay: suspend (P) -> ReplayOutcome,
    private val hintKey: (P) -> Pair<I, K>? = { null },
    private val isTransient: (Throwable) -> Boolean = { true },
    private val policy: DrainPolicy = DrainPolicy(),
) : OfflineWriteDrainer<I, K> {
    private var job: Job? = null

    private val drainMutex: Mutex = Mutex()

    private val syncedHints: MutableMap<I, MutableSet<K>> = mutableMapOf()

    override fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return

        // isOnline is a StateFlow, so collection replays the current value immediately: onEach drains
        // once right away when already online, and again on every later reconnect. No separate onStart
        // (it would double-drain the initial online state).
        job = scope.launch(dispatchers.io) {
            networkAvailability.isOnline
                .onEach { online -> if (online) drainGuarded() }
                .collect()
        }
    }

    override suspend fun drain(): Map<I, Set<K>> = drainMutex.withLock {
        drainOnce()

        val snapshot = syncedHints.mapValues { (_, kinds) -> kinds.toSet() }
        syncedHints.clear()

        snapshot
    }

    // Connectivity-triggered drains must never kill the long-lived collect loop, so a drain failure is
    // logged and swallowed (cancellation still propagates); the next connectivity emission retries.
    private suspend fun drainGuarded() {
        drainMutex.withLock {
            runCatchingLogged(context = "offline drain") { drainOnce() }
        }
    }

    private suspend fun drainOnce() {
        val pending = runCatchingLogged(context = "offline drain: read pending queue") {
            store.getPending(policy.maxAttempts)
        }.getOrElse { return }

        for (row in pending) {
            val replayed = replayWithBackoff(row.payload)

            replayed
                .onSuccess { outcome ->
                    store.delete(row.localId)

                    if (outcome == ReplayOutcome.SYNCED) {
                        // A bug in the app's hintKey must not lose the row (already deleted) nor abort the
                        // rest of the pass, so it degrades to just a missing hint for this row.
                        runCatchingLogged(context = "offline drain: record reconciliation hint") {
                            recordHint(row.payload)
                        }
                    }
                }
                .onFailure { error ->
                    if (isTransient(error)) {
                        logTransientHalt(error, row.attempts)

                        store.incrementAttempts(row.localId)

                        return
                    }

                    AppLog.w(error, "offline drain: discarding terminally-rejected write")

                    store.delete(row.localId)
                }
        }
    }

    private fun logTransientHalt(
        error: Throwable,
        attempts: Int,
    ) {
        val nextAttempts = attempts + 1

        if (nextAttempts >= policy.maxAttempts) {
            AppLog.w(error, "offline drain: write poisoned after $nextAttempts attempts; it will no longer be retried")
        } else {
            AppLog.w(error, "offline drain halted by a transient failure (attempt $nextAttempts of ${policy.maxAttempts})")
        }
    }

    private suspend fun replayWithBackoff(payload: P): Result<ReplayOutcome> {
        var backoff = policy.initialBackoffMs
        var lastError: Throwable? = null

        repeat(policy.inDrainRetries) { attempt ->
            val result = runCatchingCancellable { replay(payload) }

            result.onSuccess { return Result.success(it) }
            lastError = result.exceptionOrNull()

            if (attempt < policy.inDrainRetries - 1) {
                delay(backoff)

                backoff = (backoff * policy.backoffMultiplier).coerceAtMost(policy.backoffCapMs)
            }
        }

        return Result.failure(lastError ?: IllegalStateException("replay produced no result"))
    }

    private fun recordHint(payload: P) {
        val (id, kind) = hintKey(payload) ?: return

        syncedHints.getOrPut(id) { mutableSetOf() }.add(kind)
    }
}
