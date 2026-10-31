package nl.rhaydus.konsist

/**
 * An import that no file may use, except the files matching [allowedFiles].
 *
 * @property import the fully-qualified name, for example `androidx.compose.material3.Text`.
 * @property allowedFiles glob patterns over the project-relative path, for example a glob ending in `AppText.kt`.
 * @property reason what to use instead, shown in the failure.
 */
data class ForbiddenImport(
    val import: String,
    val allowedFiles: List<String> = emptyList(),
    val reason: String,
)
