package nl.rhaydus.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.compose.resources.ResourcesExtension

/**
 * `rhaydus.resource-packaging`: a build-time gate, applied to the root project, that fails when a module
 * declares Compose Multiplatform resources but leaves `androidResources.enable` off in its `androidLibrary`
 * block. The KMP Android library plugin ships that flag OFF, and CMP resources ride on it as Android assets,
 * so the module compiles cleanly, generates its `Res` accessor, and throws `MissingResourceException` on the
 * first read at runtime. Nothing in the source or the dependency graph is wrong, so no other gate sees it.
 *
 * Registers `checkResourcePackaging` and wires it into the `check` of every subproject.
 */
class ResourcePackagingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "rhaydus.resource-packaging must be applied to the root project (it inspects all subprojects)."
        }

        target.tasks.register("checkResourcePackaging") {
            group = "verification"
            description = "Fails when a module declares Compose Multiplatform resources but does not enable " +
                "Android resources, which silently omits them from the APK."

            doLast {
                val violations = mutableListOf<String>()
                var checked = 0

                target.subprojects.forEach { module ->
                    // `packageOfResClass` is the discriminator, not the presence of the Compose plugin (which
                    // every UI module applies) nor a `composeResources/` directory on disk (which the
                    // `customDirectory` case does not have). Declaring a resource package IS the act of saying
                    // "this module owns resources"; the plugin leaves it empty for everyone else.
                    val compose = module.extensions.findByName("compose") as? ExtensionAware
                        ?: return@forEach
                    val resources = compose.extensions.findByType(ResourcesExtension::class.java)
                        ?: return@forEach

                    if (resources.packageOfResClass.isEmpty()) return@forEach

                    val kotlin = module.extensions.findByName("kotlin") as? ExtensionAware
                        ?: return@forEach
                    val androidLibrary = kotlin.extensions
                        .findByName("androidLibrary") as? KotlinMultiplatformAndroidLibraryExtension
                        ?: return@forEach

                    checked++

                    if (androidLibrary.androidResources.enable.not()) {
                        violations += "${module.path}  (declares Compose resources but leaves " +
                            "`androidResources.enable` off, so nothing is packaged into the APK and the " +
                            "first read throws MissingResourceException at runtime - add " +
                            "`androidResources.enable = true` to its `androidLibrary` block)"
                    }
                }

                if (violations.isNotEmpty()) {
                    throw GradleException(
                        "Compose resources declared but not packaged:\n" +
                            violations.sorted().joinToString("\n") { "  - $it" },
                    )
                }

                logger.lifecycle(
                    "checkResourcePackaging: $checked module(s) with Compose resources, all packaged.",
                )
            }
        }

        target.subprojects {
            tasks.matching { it.name == "check" }.configureEach {
                dependsOn(target.tasks.named("checkResourcePackaging"))
            }
        }
    }
}
