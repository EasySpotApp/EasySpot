package xyz.ggorg.easyspot.ui.main

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rikka.shizuku.Shizuku
import xyz.ggorg.easyspot.PermissionUtils

@Composable
fun TempUi(
    mainVm: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val serviceConnectionState by mainVm.serviceConnectionState.collectAsStateWithLifecycle()
    val serviceState by mainVm.serviceState.collectAsStateWithLifecycle()

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            mainVm.binder?.updateState()
        }
    val bluetoothLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) {}

    Column(
        modifier
            .padding(4.dp),
        Arrangement.Top,
        Alignment.CenterHorizontally,
    ) {
        Text("Connection: $serviceConnectionState, service: $serviceState")

        Button(onClick = {
            permissionLauncher.launch(PermissionUtils.permissionsToRequest)
        }) {
            Text("request permissions")
        }

        Button(onClick = {
            Shizuku.requestPermission(System.currentTimeMillis().toInt())
        }) {
            Text("request shizuku")
        }

        Button(
            onClick = {
                bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            },
        ) {
            Text("enable bluetooth")
        }
    }
}
