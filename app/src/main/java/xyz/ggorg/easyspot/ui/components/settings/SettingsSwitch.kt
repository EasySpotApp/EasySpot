package xyz.ggorg.easyspot.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import xyz.ggorg.easyspot.ui.theme.EasySpotTheme

@Composable
fun SettingsSwitch(
    label: String,
    isChecked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label)

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Preview
@Composable
private fun SettingsSwitchPreview() {
    EasySpotTheme {
        SettingsSwitch(
            label = "Example Switch",
            isChecked = true,
        )
    }
}
