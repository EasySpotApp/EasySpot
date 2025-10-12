package xyz.ggorg.easyspot.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.ggorg.easyspot.R
import xyz.ggorg.easyspot.service.ServiceState
import xyz.ggorg.easyspot.ui.theme.EasySpotTheme

@Composable
fun StatusList(
    serviceState: ServiceState,
    isRunning: Boolean,
    modifier: Modifier = Modifier,
    onFixBluetooth: () -> Unit = {},
    onFixShizuku: () -> Unit = {},
    onFixNotification: () -> Unit = {},
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        serviceState.bluetooth.let {
            StatusCard(
                icon = painterResource(R.drawable.rounded_bluetooth_24),
                text = "Bluetooth",
                ok = it == ServiceState.BluetoothState.On,
                fixable =
                    it >= ServiceState.BluetoothState.NoPermission &&
                        it != ServiceState.BluetoothState.On,
                status = it.name,
                onClick = onFixBluetooth,
            )
        }

        serviceState.shizuku.let {
            StatusCard(
                icon = painterResource(R.drawable.shizuku_logo_mono),
                iconPadding = false,
                text = "Shizuku",
                ok = it == ServiceState.ShizukuState.Running,
                status = it.name,
                onClick = onFixShizuku,
            )
        }

        serviceState.notificationPermission.let {
            StatusCard(
                icon = painterResource(R.drawable.rounded_notifications_active_24),
                text = "Notifications",
                ok = it,
                status = if (it) "Granted" else "Denied",
                onClick = onFixNotification,
            )
        }

        isRunning.let {
            StatusCard(
                icon = painterResource(R.drawable.rounded_mobile_gear_24),
                text = "Service",
                ok = it,
                status = if (it) "Running" else "Not running",
                fixable = false,
            )
        }
    }
}

@Preview
@Composable
private fun StatusListPreview() {
    EasySpotTheme {
        StatusList(
            ServiceState(
                bluetooth = ServiceState.BluetoothState.NoAdapter,
                shizuku = ServiceState.ShizukuState.NotInstalled,
                notificationPermission = false,
            ),
            isRunning = false,
        )
    }
}
