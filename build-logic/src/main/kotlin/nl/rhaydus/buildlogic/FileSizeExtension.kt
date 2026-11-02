package nl.rhaydus.buildlogic

import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * Configuration for the `rhaydus.file-size` gate ([FileSizeConventionPlugin]).
 *
 * Only main source sets are scanned: the `main` source set and every source set whose name ends in `Main`,
 * never a test source set.
 *
 * - [maxLines] is the longest a file in scope may be. Defaults to 600.
 * - [packageSegments] puts a `.kt` file in scope when any directory segment of its path relative to the
 *   source-set root equals one of these names. Defaults to `presentation`.
 * - [wholeModules] holds project paths (such as `:core:ui`) whose every main-source `.kt` file is in scope.
 *   Defaults to empty.
 */
abstract class FileSizeExtension {
    abstract val maxLines: Property<Int>

    abstract val packageSegments: SetProperty<String>

    abstract val wholeModules: SetProperty<String>
}
