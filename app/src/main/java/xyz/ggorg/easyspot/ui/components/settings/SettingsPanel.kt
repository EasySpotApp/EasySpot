package xyz.ggorg.easyspot.ui.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xyz.ggorg.easyspot.R
import xyz.ggorg.easyspot.ui.theme.EasySpotTheme

@Composable
fun SettingsPanel(
    modifier: Modifier = Modifier,
    startOnBoot: Boolean = true,
    onChangeStartOnBoot: (Boolean) -> Unit = {},
    bleEncryption: Boolean = true,
    onChangeBleEncryption: (Boolean) -> Unit = {},
    bleMitmProtection: Boolean = false,
    onChangeBleMitmProtection: (Boolean) -> Unit = {},
    advertisingPowerMode: Int = 0,
    onChangeAdvertisingPowerMode: (Int) -> Unit = {},
    advertisingTxPower: Int = 1,
    onChangeAdvertisingTxPower: (Int) -> Unit = {},
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        SettingsSwitch(
            label = stringResource(R.string.settings_start_on_boot),
            isChecked = startOnBoot,
            onCheckedChange = onChangeStartOnBoot,
        )

        SettingsSwitch(
            label = stringResource(R.string.settings_ble_encryption),
            isChecked = bleEncryption,
            onCheckedChange = onChangeBleEncryption,
        )

        AnimatedVisibility(bleEncryption) {
            SettingsSwitch(
                label = stringResource(R.string.settings_ble_mitm_protection),
                isChecked = bleMitmProtection,
                onCheckedChange = onChangeBleMitmProtection,
            )
        }

        SettingsSlider(
            label = stringResource(R.string.settings_advertising_power_mode),
            position = advertisingPowerMode,
            values = 3,
            onPositionChange = onChangeAdvertisingPowerMode,
        )

        SettingsSlider(
            label = stringResource(R.string.settings_advertising_tx_power),
            position = advertisingTxPower,
            values = 4,
            onPositionChange = onChangeAdvertisingTxPower,
        )
    }
}

@Preview
@Composable
private fun SettingsPanelPreview() {
    EasySpotTheme {
        SettingsPanel()
    }
}
