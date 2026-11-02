package nl.rhaydus.toad.navigation

/**
 * Marks a Voyager `Screen` constructor parameter that must be **excluded** from the back-stack's Java
 * serialization.
 *
 * On the JVM targets (Android and desktop), Voyager's `Screen` extends `java.io.Serializable` and the
 * default `Navigator` persists the screen stack across configuration change and process death via
 * `rememberSaveable`. A non-serializable nav argument, such as a hint that holds domain models, would
 * then throw `NotSerializableException`. There this annotation actuals to `kotlin.jvm.Transient`, so
 * the field is dropped from serialization and restores as `null`. It is erased on iOS, where `Screen`
 * is not serializable.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
@Target(AnnotationTarget.FIELD)
expect annotation class TransientNavArg()
