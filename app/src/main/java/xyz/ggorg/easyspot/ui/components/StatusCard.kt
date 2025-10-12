package xyz.ggorg.easyspot.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.ggorg.easyspot.R
import xyz.ggorg.easyspot.ui.theme.EasySpotTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StatusCard(
    icon: Painter,
    text: String,
    ok: Boolean,
    status: String,
    modifier: Modifier = Modifier,
    iconPadding: Boolean = true,
    fixable: Boolean = !ok,
    onClick: () -> Unit = {},
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialShapes.Cookie6Sided.toShape(),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .let { if (iconPadding) it.padding(8.dp) else it },
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Text(
                    text = text,
                    modifier = Modifier.padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val color =
                    if (ok) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }

                Icon(
                    painterResource(
                        if (ok) {
                            R.drawable.rounded_check_circle_24
                        } else {
                            R.drawable.rounded_warning_24px
                        },
                    ),
                    contentDescription = null,
                    tint = color,
                )

                Text(status, color = color)

                if (fixable) {
                    Button(
                        onClick = onClick,
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Icon(
                            painterResource(R.drawable.rounded_build_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp).padding(end = 4.dp),
                        )

                        Text("Fix")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun StatusCardPreview() {
    EasySpotTheme(darkTheme = true) {
        StatusCard(
            icon = painterResource(R.drawable.rounded_bluetooth_24),
            text = "Bluetooth",
            ok = true,
            status = "On",
        )
    }
}
