package nl.rhaydus.buildlogic

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * The `r8KeepContract { … }` block of `rhaydus.android.r8-keep-contract`: what the contract needs to know
 * about the consuming project that no library tells it.
 */
abstract class R8KeepContractExtension {
    /** The package the project's own classes live under, without a trailing dot. Required. */
    abstract val projectPackage: Property<String>

    /**
     * The simple name suffix of the project's generated Room database class, such as `AppDatabase_Impl`.
     * Leave unset when the project has no Room database.
     */
    abstract val databaseImplSuffix: Property<String>

    /**
     * Supertypes whose project implementors must survive R8 renamed but intact. Defaults to Voyager's `Screen`
     * and Glance's `GlanceAppWidget`.
     */
    abstract val renamedOnlySupertypes: ListProperty<String>

    /**
     * Root-relative directories whose `keepRules/` are skipped, for modules whose rules apply to a different
     * R8 run than the application's, such as an instrumented test APK.
     */
    abstract val excludedProjectDirs: ListProperty<String>
}
