package xyz.ggorg.easyspot.ui.components.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import xyz.ggorg.easyspot.ui.theme.EasySpotTheme

@Composable
fun SettingsSlider(
    label: String,
    position: Int,
    values: Int,
    modifier: Modifier = Modifier,
    onPositionChange: (Int) -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(text = label)

        Slider(
            value = position.toFloat(),
            onValueChange = { newValue -> onPositionChange(newValue.toInt()) },
            valueRange = 0f..values.toFloat() - 1,
            steps = values - 2,
        )
    }
}

@Preview
@Composable
private fun SettingsSliderPreview() {
    EasySpotTheme {
        SettingsSlider(
            label = "Example Slider",
            position = 1,
            values = 3,
        )
    }
}
