package xyz.ggorg.easyspot.ui.components.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import xyz.ggorg.easyspot.R
import xyz.ggorg.easyspot.ui.theme.EasySpotTheme

@Composable
fun SettingsDialogWrapper(
    onDismiss: () -> Unit,
    restartService: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val settingsDataStore = SettingsDataStore(context)

    val startOnBoot by settingsDataStore.startOnBootFlow.collectAsStateWithLifecycle(
        initialValue = true,
    )
    val bleEncryption by settingsDataStore.bleEncryptionFlow.collectAsStateWithLifecycle(
        initialValue = true,
    )
    val bleMitmProtection by settingsDataStore.bleMitmProtectionFlow.collectAsStateWithLifecycle(
        initialValue = false,
    )
    val advertisingPowerMode by settingsDataStore.advertisingPowerModeFlow
        .collectAsStateWithLifecycle(
            initialValue = 0,
        )
    val advertisingTxPower by settingsDataStore.advertisingTxPowerFlow.collectAsStateWithLifecycle(
        initialValue = 1,
    )

    SettingsDialog(
        modifier = modifier,
        onDismiss = onDismiss,
        startOnBoot,
        onChangeStartOnBoot = { enabled ->
            scope.launch { settingsDataStore.setStartOnBoot(enabled) }
        },
        bleEncryption,
        onChangeBleEncryption = { enabled ->
            scope.launch {
                settingsDataStore.setBleEncryption(enabled)
                restartService()
            }
        },
        bleMitmProtection,
        onChangeBleMitmProtection = { enabled ->
            scope.launch {
                settingsDataStore.setBleMitmProtection(enabled)
                restartService()
            }
        },
        advertisingPowerMode,
        onChangeAdvertisingPowerMode = { mode ->
            scope.launch {
                settingsDataStore.setAdvertisingPowerMode(mode)
                restartService()
            }
        },
        advertisingTxPower,
        onChangeAdvertisingTxPower = { txPower ->
            scope.launch {
                settingsDataStore.setAdvertisingTxPower(txPower)
                restartService()
            }
        },
    )
}

@Composable
fun SettingsDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
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
    AlertDialog(
        modifier = modifier,
        onDismissRequest = { onDismiss() },
        title = { Text(stringResource(R.string.settings)) },
        text = {
            SettingsPanel(
                startOnBoot = startOnBoot,
                onChangeStartOnBoot = onChangeStartOnBoot,
                bleEncryption = bleEncryption,
                onChangeBleEncryption = onChangeBleEncryption,
                bleMitmProtection = bleMitmProtection,
                onChangeBleMitmProtection = onChangeBleMitmProtection,
                advertisingPowerMode = advertisingPowerMode,
                onChangeAdvertisingPowerMode = onChangeAdvertisingPowerMode,
                advertisingTxPower = advertisingTxPower,
                onChangeAdvertisingTxPower = onChangeAdvertisingTxPower,
            )
        },
        confirmButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(stringResource(R.string.settings_confirm))
            }
        },
    )
}

@Preview
@Composable
private fun SettingsDialogPreview() {
    EasySpotTheme {
        SettingsDialog()
    }
}
