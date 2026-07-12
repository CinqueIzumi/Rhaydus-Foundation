package nl.rhaydus.offlinesync

/**
 * The enqueue side of an offline write queue: the narrow seam an optimistic-write path depends on. When a
 * mutation can't reach the server, the app writes locally and [enqueue]s a [payload] describing the write
 * so it can be replayed later (see [OfflineWriteDrainer]). [payload] is opaque to the queue — the app owns
 * its shape. Backed by a [PendingWriteStore].
 */
interface WriteQueue<P> {
    suspend fun enqueue(payload: P)
}
