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
    modifier: Modifier = Modifier,
    onFixBluetooth: () -> Unit = {},
    onFixShizuku: () -> Unit = {},
    onFixNotification: () -> Unit = {},
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        serviceState.bluetooth.let {
            StatusCard(
                icon = painterResource(R.drawable.rounded_bluetooth_24),
                textResource = R.string.home_statuslist_bluetooth,
                ok = it == ServiceState.BluetoothState.On,
                fixable =
                    it >= ServiceState.BluetoothState.NoPermission &&
                        it != ServiceState.BluetoothState.On,
                statusResource =
                    when (it) {
                        ServiceState.BluetoothState.NoAdapter -> {
                            R.string.home_statuslist_bluetooth_noadapter
                        }

                        ServiceState.BluetoothState.NoBle -> {
                            R.string.home_statuslist_bluetooth_noble
                        }

                        ServiceState.BluetoothState.NoAdvertising -> {
                            R.string.home_statuslist_bluetooth_noadvertising
                        }

                        ServiceState.BluetoothState.NoPermission -> {
                            R.string.home_statuslist_bluetooth_nopermission
                        }

                        ServiceState.BluetoothState.Off -> {
                            R.string.home_statuslist_bluetooth_off
                        }

                        ServiceState.BluetoothState.On -> {
                            R.string.home_statuslist_bluetooth_on
                        }
                    },
                onClick = onFixBluetooth,
            )
        }

        serviceState.shizuku.let {
            StatusCard(
                icon = painterResource(R.drawable.shizuku_logo_mono),
                iconPadding = false,
                textResource = R.string.home_statuslist_shizuku,
                ok = it == ServiceState.ShizukuState.Running,
                statusResource =
                    when (it) {
                        ServiceState.ShizukuState.NotInstalled -> {
                            R.string.home_statuslist_shizuku_notinstalled
                        }

                        ServiceState.ShizukuState.NotRunning -> {
                            R.string.home_statuslist_shizuku_notrunning
                        }

                        ServiceState.ShizukuState.NoPermission -> {
                            R.string.home_statuslist_shizuku_nopermission
                        }

                        ServiceState.ShizukuState.Running -> {
                            R.string.home_statuslist_shizuku_running
                        }
                    },
                onClick = onFixShizuku,
            )
        }

        serviceState.notificationPermission.let {
            StatusCard(
                icon = painterResource(R.drawable.rounded_notifications_active_24),
                textResource = R.string.home_statuslist_notifications,
                ok = it,
                statusResource =
                    if (it) {
                        R.string.home_statuslist_notifications_granted
                    } else {
                        R.string.home_statuslist_notifications_denied
                    },
                onClick = onFixNotification,
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
        )
    }
}
