package nl.rhaydus.offlinesync

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import nl.rhaydus.common.AppDispatchers
import nl.rhaydus.platform.NetworkAvailabilityProvider
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class DefaultOfflineWriteDrainerTest {
    private enum class TestKind { UPDATE, DELETE }

    private data class TestPayload(
        val id: Int,
        val kind: TestKind,
    )

    private class FakePendingWriteStore<P> : PendingWriteStore<P> {
        private val rows = mutableListOf<PendingWrite<P>>()
        private var nextId = 1L

        val abandonedIds = mutableListOf<Long>()
        val deletedIds = mutableListOf<Long>()
        val incrementedIds = mutableListOf<Long>()

        var abandonFailure: Throwable? = null

        var getPendingCallCount = 0
            private set

        fun snapshot(): List<PendingWrite<P>> = rows.toList()

        fun seed(vararg writes: PendingWrite<P>) {
            rows.addAll(writes)
        }

        override suspend fun enqueue(payload: P) {
            rows.add(
                PendingWrite(
                    localId = nextId++,
                    attempts = 0,
                    payload = payload,
                ),
            )
        }

        override suspend fun getPending(): List<PendingWrite<P>> {
            getPendingCallCount++

            return rows.toList()
        }

        override suspend fun delete(localId: Long) {
            deletedIds.add(localId)
            rows.removeAll { it.localId == localId }
        }

        override suspend fun abandon(write: PendingWrite<P>) {
            abandonedIds.add(write.localId)
            abandonFailure?.let { throw it }
            rows.removeAll { it.localId == write.localId }
        }

        override suspend fun incrementAttempts(localId: Long) {
            incrementedIds.add(localId)

            val index = rows.indexOfFirst { it.localId == localId }

            if (index >= 0) rows[index] = rows[index].copy(attempts = rows[index].attempts + 1)
        }
    }

    private class FakeNetworkAvailabilityProvider(
        initiallyOnline: Boolean = false,
    ) : NetworkAvailabilityProvider {
        override val isOnline = MutableStateFlow(initiallyOnline)

        override suspend fun awaitOnline() {
            isOnline.first { it }
        }
    }

    private fun TestScope.appDispatchers(): AppDispatchers {
        val dispatcher = StandardTestDispatcher(testScheduler)

        return AppDispatchers(
            main = dispatcher,
            io = dispatcher,
            default = dispatcher,
        )
    }

    @Nested
    inner class Drain {
        @Test
        fun `replays pending writes oldest-first, deletes synced rows and never abandons them`() = runTest {
            // ----- Arrange -----
            val store = FakePendingWriteStore<TestPayload>()
            store.enqueue(
                TestPayload(
                    id = 1,
                    kind = TestKind.UPDATE,
                ),
            )
            store.enqueue(
                TestPayload(
                    id = 2,
                    kind = TestKind.DELETE,
                ),
            )

            val replayedIds = mutableListOf<Int>()
            val drainer = DefaultOfflineWriteDrainer(
                store = store,
                networkAvailability = FakeNetworkAvailabilityProvider(),
                dispatchers = appDispatchers(),
                replay = { payload ->
                    replayedIds.add(payload.id)
                    ReplayOutcome.SYNCED
                },
                hintKey = { it.id to it.kind },
            )

            // ----- Act -----
            val hints = drainer.drain()

            // ----- Assert -----
            replayedIds shouldBe listOf(1, 2)
            store.snapshot() shouldBe emptyList()
            store.deletedIds shouldBe listOf(1L, 2L)
            store.abandonedIds shouldBe emptyList()
            hints shouldBe mapOf(
                1 to setOf(TestKind.UPDATE),
                2 to setOf(TestKind.DELETE),
            )
        }

        @Test
        fun `hint buffer is cleared after each drain call`() = runTest {
            // ----- Arrange -----
            val store = FakePendingWriteStore<TestPayload>()
            store.enqueue(
                TestPayload(
                    id = 1,
                    kind = TestKind.UPDATE,
                ),
            )

            val drainer = DefaultOfflineWriteDrainer(
                store = store,
                networkAvailability = FakeNetworkAvailabilityProvider(),
                dispatchers = appDispatchers(),
                replay = { ReplayOutcome.SYNCED },
                hintKey = { it.id to it.kind },
            )

            // ----- Act -----
            val firstHints = drainer.drain()
            val secondHints = drainer.drain()

            // ----- Assert -----
            firstHints shouldBe mapOf(1 to setOf(TestKind.UPDATE))
            secondHints shouldBe emptyMap()
        }

        @Test
        fun `DISCARDED outcome deletes the row without abandoning it or recording a hint`() = runTest {
            // ----- Arrange -----
            val store = FakePendingWriteStore<TestPayload>()
            store.enqueue(
                TestPayload(
                    id = 1,
                    kind = TestKind.UPDATE,
                ),
            )

            val drainer = DefaultOfflineWriteDrainer(
                store = store,
                networkAvailability = FakeNetworkAvailabilityProvider(),
                dispatchers = appDispatchers(),
                replay = { ReplayOutcome.DISCARDED },
                hintKey = { it.id to it.kind },
            )

            // ----- Act -----
            val hints = drainer.drain()

            // ----- Assert -----
            hints shouldBe emptyMap()
            store.deletedIds shouldBe listOf(1L)
            store.abandonedIds shouldBe emptyList()
            store.snapshot() shouldBe emptyList()
        }

        @Test
        fun `transient failure halts the drain and leaves later rows untouched`() = runTest {
            // ----- Arrange -----
            val store = FakePendingWriteStore<TestPayload>()
            store.enqueue(
                TestPayload(
                    id = 1,
                    kind = TestKind.UPDATE,
                ),
            )
            store.enqueue(
                TestPayload(
                    id = 2,
                    kind = TestKind.UPDATE,
                ),
            )

            val replayedIds = mutableListOf<Int>()
            val drainer = DefaultOfflineWriteDrainer(
                store = store,
                networkAvailability = FakeNetworkAvailabilityProvider(),
                dispatchers = appDispatchers(),
                replay = { payload ->
                    replayedIds.add(payload.id)
                    error("transient boom")
                },
                hintKey = { it.id to it.kind },
                isTransient = { true },
            )

            // ----- Act -----
            drainer.drain()

            // ----- Assert -----
            replayedIds shouldBe listOf(1)
            store.abandonedIds shouldBe emptyList()
            store.snapshot() shouldBe listOf(
                PendingWrite(
                    localId = 1,
                    attempts = 1,
                    payload = TestPayload(
                        id = 1,
                        kind = TestKind.UPDATE,
                    ),
                ),
                PendingWrite(
                    localId = 2,
                    attempts = 0,
                    payload = TestPayload(
                        id = 2,
                        kind = TestKind.UPDATE,
                    ),
                ),
            )
        }

        @Test
        fun `terminal failure deletes the row without abandoning it and the drain continues`() = runTest {
            // ----- Arrange -----
            val store = FakePendingWriteStore<TestPayload>()
            store.enqueue(
                TestPayload(
                    id = 1,
                    kind = TestKind.UPDATE,
                ),
            )
            store.enqueue(
                TestPayload(
                    id = 2,
                    kind = TestKind.UPDATE,
                ),
            )

            val replayedIds = mutableListOf<Int>()
            val drainer = DefaultOfflineWriteDrainer(
                store = store,
                networkAvailability = FakeNetworkAvailabilityProvider(),
                dispatchers = appDispatchers(),
                replay = { payload ->
                    replayedIds.add(payload.id)
                    if (payload.id == 1) error("terminal boom") else ReplayOutcome.SYNCED
                },
                hintKey = { it.id to it.kind },
                isTransient = { false },
            )

            // ----- Act -----
            val hints = drainer.drain()

            // ----- Assert -----
            replayedIds shouldBe listOf(1, 2)
            store.abandonedIds shouldBe emptyList()
            store.deletedIds shouldBe listOf(1L, 2L)
            store.snapshot() shouldBe emptyList()
            hints shouldBe mapOf(2 to setOf(TestKind.UPDATE))
        }

        @Test
        fun `in-drain backoff retries a failing replay and eventually succeeds`() = runTest {
            // ----- Arrange -----
            val store = FakePendingWriteStore<TestPayload>()
            store.enqueue(
                TestPayload(
                    id = 1,
                    kind = TestKind.UPDATE,
                ),
            )

            var callCount = 0
            val drainer = DefaultOfflineWriteDrainer(
                store = store,
                networkAvailability = FakeNetworkAvailabilityProvider(),
                dispatchers = appDispatchers(),
                replay = {
                    callCount++
                    if (callCount < 3) error("flaky") else ReplayOutcome.SYNCED
                },
                hintKey = { it.id to it.kind },
                policy = DrainPolicy(inDrainRetries = 3),
            )

            // ----- Act -----
            val startTime = testScheduler.currentTime
            drainer.drain()
            advanceUntilIdle()
            val elapsed = testScheduler.currentTime - startTime

            // ----- Assert -----
            callCount shouldBe 3
            store.snapshot() shouldBe emptyList()
            // Regression guard: exponential backoff between in-drain retries — 250ms before the retry
            // that follows attempt 1, then 500ms (250 * backoffMultiplier 2) before attempt 3.
            elapsed shouldBe 750L
        }

        @Test
        fun `a row already at the cap is abandoned without replay and the pass continues`() = runTest {
            // ----- Arrange -----
            val store = FakePendingWriteStore<TestPayload>()
            store.seed(
                PendingWrite(
                    localId = 1,
                    attempts = 2,
                    payload = TestPayload(
                        id = 1,
                        kind = TestKind.UPDATE,
                    ),
                ),
                PendingWrite(
                    localId = 2,
                    attempts = 0,
                    payload = TestPayload(
                        id = 2,
                        kind = TestKind.UPDATE,
                    ),
                ),
            )

            val replayedIds = mutableListOf<Int>()
            val drainer = DefaultOfflineWriteDrainer(
                store = store,
                networkAvailability = FakeNetworkAvailabilityProvider(),
                dispatchers = appDispatchers(),
                replay = { payload ->
                    replayedIds.add(payload.id)
                    ReplayOutcome.SYNCED
                },
                hintKey = { it.id to it.kind },
                policy = DrainPolicy(maxAttempts = 2),
            )

            // ----- Act -----
            val hints = drainer.drain()

            // ----- Assert -----
            store.abandonedIds shouldBe listOf(1L)
            replayedIds shouldBe listOf(2)
            store.deletedIds shouldBe listOf(2L)
            hints shouldBe mapOf(2 to setOf(TestKind.UPDATE))
        }

        @Test
        fun `a throwing abandon on an over-cap row neither stops the pass nor deletes the row`() = runTest {
            // ----- Arrange -----
            val store = FakePendingWriteStore<TestPayload>()
            store.abandonFailure = IllegalStateException("abandon boom")
            store.seed(
                PendingWrite(
                    localId = 1,
                    attempts = 2,
                    payload = TestPayload(
                        id = 1,
                        kind = TestKind.UPDATE,
                    ),
                ),
                PendingWrite(
                    localId = 2,
                    attempts = 0,
                    payload = TestPayload(
                        id = 2,
                        kind = TestKind.UPDATE,
                    ),
                ),
            )

            val replayedIds = mutableListOf<Int>()
            val drainer = DefaultOfflineWriteDrainer(
                store = store,
                networkAvailability = FakeNetworkAvailabilityProvider(),
                dispatchers = appDispatchers(),
                replay = { payload ->
                    replayedIds.add(payload.id)
                    ReplayOutcome.SYNCED
                },
                hintKey = { it.id to it.kind },
                policy = DrainPolicy(maxAttempts = 2),
            )

            // ----- Act -----
            val hints = drainer.drain()

            // ----- Assert -----
            replayedIds shouldBe listOf(2)
            store.deletedIds shouldBe listOf(2L)
            hints shouldBe mapOf(2 to setOf(TestKind.UPDATE))
            store.snapshot() shouldBe listOf(
                PendingWrite(
                    localId = 1,
                    attempts = 2,
                    payload = TestPayload(
                        id = 1,
                        kind = TestKind.UPDATE,
                    ),
                ),
            )
        }

        @Test
        fun `a transient failure reaching the cap increments the attempts, abandons the row and halts`() = runTest {
            // ----- Arrange -----
            val store = FakePendingWriteStore<TestPayload>()
            store.seed(
                PendingWrite(
                    localId = 1,
                    attempts = 1,
                    payload = TestPayload(
                        id = 1,
                        kind = TestKind.UPDATE,
                    ),
                ),
                PendingWrite(
                    localId = 2,
                    attempts = 0,
                    payload = TestPayload(
                        id = 2,
                        kind = TestKind.UPDATE,
                    ),
                ),
            )

            val replayedIds = mutableListOf<Int>()
            val drainer = DefaultOfflineWriteDrainer(
                store = store,
                networkAvailability = FakeNetworkAvailabilityProvider(),
                dispatchers = appDispatchers(),
                replay = { payload ->
                    replayedIds.add(payload.id)
                    error("transient boom")
                },
                hintKey = { it.id to it.kind },
                policy = DrainPolicy(maxAttempts = 2),
            )

            // ----- Act -----
            drainer.drain()

            // ----- Assert -----
            store.incrementedIds shouldBe listOf(1L)
            store.abandonedIds shouldBe listOf(1L)
            replayedIds shouldBe listOf(1)
            store.snapshot() shouldBe listOf(
                PendingWrite(
                    localId = 2,
                    attempts = 0,
                    payload = TestPayload(
                        id = 2,
                        kind = TestKind.UPDATE,
                    ),
                ),
            )
        }

        @Test
        fun `a throwing abandon on a cap-reaching transient failure leaves the row at the cap and never replays it again`() = runTest {
            // ----- Arrange -----
            val store = FakePendingWriteStore<TestPayload>()
            store.abandonFailure = IllegalStateException("abandon boom")
            store.seed(
                PendingWrite(
                    localId = 1,
                    attempts = 1,
                    payload = TestPayload(
                        id = 1,
                        kind = TestKind.UPDATE,
                    ),
                ),
            )

            var replayCount = 0
            val drainer = DefaultOfflineWriteDrainer(
                store = store,
                networkAvailability = FakeNetworkAvailabilityProvider(),
                dispatchers = appDispatchers(),
                replay = {
                    replayCount++
                    error("transient boom")
                },
                hintKey = { it.id to it.kind },
                policy = DrainPolicy(maxAttempts = 2),
            )

            // ----- Act -----
            drainer.drain()
            val replaysAfterFirstDrain = replayCount
            drainer.drain()

            // ----- Assert -----
            replaysAfterFirstDrain shouldBe 1
            replayCount shouldBe replaysAfterFirstDrain
            store.abandonedIds shouldBe listOf(1L, 1L)
            store.snapshot() shouldBe listOf(
                PendingWrite(
                    localId = 1,
                    attempts = 2,
                    payload = TestPayload(
                        id = 1,
                        kind = TestKind.UPDATE,
                    ),
                ),
            )
        }
    }

    @Nested
    inner class Start {
        @Test
        fun `drains immediately when already online`() = runTest {
            // ----- Arrange -----
            val store = FakePendingWriteStore<TestPayload>()
            store.enqueue(
                TestPayload(
                    id = 1,
                    kind = TestKind.UPDATE,
                ),
            )

            val drainer = DefaultOfflineWriteDrainer(
                store = store,
                networkAvailability = FakeNetworkAvailabilityProvider(initiallyOnline = true),
                dispatchers = appDispatchers(),
                replay = { ReplayOutcome.SYNCED },
                hintKey = { it.id to it.kind },
            )

            // ----- Act -----
            drainer.start(backgroundScope)
            runCurrent()

            // ----- Assert -----
            store.snapshot() shouldBe emptyList()
            // Regression guard: start() used to drain twice (a redundant onStart plus the StateFlow
            // replay both triggering a drain) when isOnline started true.
            store.getPendingCallCount shouldBe 1
        }

        @Test
        fun `drains again when isOnline flips false to true`() = runTest {
            // ----- Arrange -----
            val store = FakePendingWriteStore<TestPayload>()
            store.enqueue(
                TestPayload(
                    id = 1,
                    kind = TestKind.UPDATE,
                ),
            )

            val network = FakeNetworkAvailabilityProvider(initiallyOnline = false)
            val drainer = DefaultOfflineWriteDrainer(
                store = store,
                networkAvailability = network,
                dispatchers = appDispatchers(),
                replay = { ReplayOutcome.SYNCED },
                hintKey = { it.id to it.kind },
            )

            // ----- Act -----
            drainer.start(backgroundScope)
            runCurrent()
            val pendingWhileOffline = store.snapshot()

            network.isOnline.value = true
            runCurrent()

            // ----- Assert -----
            pendingWhileOffline shouldBe listOf(
                PendingWrite(
                    localId = 1,
                    attempts = 0,
                    payload = TestPayload(
                        id = 1,
                        kind = TestKind.UPDATE,
                    ),
                ),
            )
            store.snapshot() shouldBe emptyList()
        }

        @Test
        fun `a drain failure in the background collector does not cancel it`() = runTest {
            // ----- Arrange -----
            val store = FakePendingWriteStore<TestPayload>()
            store.enqueue(
                TestPayload(
                    id = 1,
                    kind = TestKind.UPDATE,
                ),
            )

            val network = FakeNetworkAvailabilityProvider(initiallyOnline = true)
            val replayedIds = mutableListOf<Int>()
            val drainer = DefaultOfflineWriteDrainer(
                store = store,
                networkAvailability = network,
                dispatchers = appDispatchers(),
                replay = { payload ->
                    replayedIds.add(payload.id)
                    ReplayOutcome.SYNCED
                },
                // Throws only for id 1, so the first (immediate) drain fails mid-loop while the row
                // itself is already deleted; the second drain (triggered by the later online flip)
                // must still run.
                hintKey = { payload -> if (payload.id == 1) error("boom") else payload.id to payload.kind },
            )

            // ----- Act -----
            drainer.start(backgroundScope)
            runCurrent()

            network.isOnline.value = false
            runCurrent()
            store.enqueue(
                TestPayload(
                    id = 2,
                    kind = TestKind.UPDATE,
                ),
            )
            network.isOnline.value = true
            runCurrent()

            // ----- Assert -----
            replayedIds shouldBe listOf(1, 2)
            store.snapshot() shouldBe emptyList()
        }
    }
}
