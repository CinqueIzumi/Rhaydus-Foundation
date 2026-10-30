package nl.rhaydus.designsystem.modifier

/** What a skeleton-masked leaf draws: nothing, the shimmer, or its real content. */
internal enum class SkeletonRevealPhase {
    Hidden,
    Shimmer,
    Content,
}
