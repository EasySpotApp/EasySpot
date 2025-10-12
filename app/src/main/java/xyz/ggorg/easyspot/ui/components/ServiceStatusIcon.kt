package xyz.ggorg.easyspot.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import xyz.ggorg.easyspot.R
import xyz.ggorg.easyspot.ui.theme.EasySpotTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ServiceStatusIcon(
    status: Boolean,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition()
    val animatedRotation =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
        )

    val morph =
        remember {
            Morph(
                start = MaterialShapes.Circle,
                end = MaterialShapes.Cookie6Sided,
            )
        }

    val morphTime = 1000

    val percentage =
        animateFloatAsState(
            targetValue = if (status) 1f else 0f,
            animationSpec = tween(morphTime),
        )

    val shape =
        remember {
            MorphShape(
                morph = morph,
                percentage = { percentage.value },
                rotation = { animatedRotation.value },
            )
        }

    val surfaceColor =
        animateColorAsState(
            targetValue =
                MaterialTheme.colorScheme.let {
                    if (status) it.primaryContainer else it.errorContainer
                },
            animationSpec = tween(morphTime),
        )

    val iconColor =
        animateColorAsState(
            targetValue =
                MaterialTheme.colorScheme.let {
                    if (status) it.onPrimaryContainer else it.onErrorContainer
                },
            animationSpec = tween(morphTime),
        )

    Surface(
        color = surfaceColor.value,
        shape = shape,
        modifier = modifier.size(240.dp),
    ) {
        Icon(
            painter =
                painterResource(
                    if (status) {
                        R.drawable.rounded_check_24
                    } else {
                        R.drawable.rounded_warning_24px
                    },
                ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().padding(64.dp),
            tint = iconColor.value,
        )
    }
}

@Preview
@Composable
private fun ServiceStatusIconPreview() {
    EasySpotTheme {
        ServiceStatusIcon(status = true)
    }
}
