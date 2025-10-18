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
        StatusCard(
            icon = painterResource(R.drawable.rounded_bluetooth_24),
            statusObject = serviceState.bluetooth,
            onClick = onFixBluetooth,
        )

        StatusCard(
            icon = painterResource(R.drawable.shizuku_logo_mono),
            iconPadding = false,
            statusObject = serviceState.shizuku,
            onClick = onFixShizuku,
        )

        StatusCard(
            icon = painterResource(R.drawable.rounded_notifications_active_24),
            statusObject = serviceState.notificationPermission,
            onClick = onFixNotification,
        )
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
                notificationPermission = ServiceState.NotificationState.Denied,
            ),
        )
    }
}
