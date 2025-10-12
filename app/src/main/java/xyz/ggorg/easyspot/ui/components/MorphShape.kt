package xyz.ggorg.easyspot.ui.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.toPath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph

class MorphShape(
    private val morph: Morph,
    private val percentage: () -> Float,
    private val rotation: () -> Float = { 0f },
) : Shape {
    private val matrix = Matrix()

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        matrix.reset()

        val pivotX = size.width / 2
        val pivotY = size.height / 2

        matrix.translate(-pivotX, -pivotY)
        matrix *= Matrix().apply { rotateZ(rotation()) }
        matrix *= Matrix().apply { translate(pivotX, pivotY) }

        matrix.scale(size.width, size.height)

        val path = morph.toPath(progress = percentage())
        path.transform(matrix)
        return Outline.Generic(path)
    }
}
