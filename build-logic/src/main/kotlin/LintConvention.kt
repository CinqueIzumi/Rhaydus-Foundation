import com.android.build.api.dsl.Lint
import org.gradle.api.Project

/**
 * The shared Android lint policy applied by both library convention plugins: treat every warning as an
 * error and abort on error, reading the single repo-root `lint.xml` for the per-issue overrides (the
 * version-freshness checks held at `informational`, the noise suppressions). Keeping it here means the two
 * plugins configure lint identically from one place instead of duplicating the block.
 */
internal fun Lint.applyRhaydusLintPolicy(project: Project) {
    warningsAsErrors = true
    abortOnError = true
    lintConfig = project.rootProject.file("lint.xml")
}
