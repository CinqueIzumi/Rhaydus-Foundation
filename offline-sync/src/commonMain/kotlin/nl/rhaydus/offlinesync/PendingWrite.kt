package nl.rhaydus.offlinesync

/**
 * One durably-stored pending write, as read back from a [PendingWriteStore] for replay. [localId] is the
 * store's stable row identity (used to delete the row or bump its [attempts]); [attempts] is how many times
 * replay has already failed transiently (the store filters out rows at or above the poison cap); [payload]
 * is the app's opaque write description.
 */
data class PendingWrite<P>(
    val localId: Long,
    val attempts: Int,
    val payload: P,
)
