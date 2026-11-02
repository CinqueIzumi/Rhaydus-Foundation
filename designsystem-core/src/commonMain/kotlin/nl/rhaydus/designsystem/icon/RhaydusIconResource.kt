package nl.rhaydus.designsystem.icon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * A brand-agnostic, paintable icon source plus its content description, so components can take an
 * icon without caring whether it comes from a multiplatform `DrawableResource` or a Compose
 * `ImageVector`.
 *
 * Each app keeps its own typed icon catalog (an enum of named icons mapping to its own
 * `DrawableResource`s) and constructs a [Drawable] from it; the assets stay per app, only this
 * mechanism is shared.
 */
@Immutable
sealed class RhaydusIconResource(
    open val contentDescription: String,
) {
    data class Drawable(
        override val contentDescription: String,
        internal val resource: DrawableResource,
    ) : RhaydusIconResource(contentDescription = contentDescription)

    data class Vector(
        override val contentDescription: String,
        internal val vector: ImageVector,
    ) : RhaydusIconResource(contentDescription = contentDescription)

    @Composable
    fun getIconPainter(): Painter {
        return when (this) {
            is Drawable -> painterResource(resource)
            is Vector -> rememberVectorPainter(image = vector)
        }
    }
}
