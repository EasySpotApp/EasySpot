package xyz.ggorg.easyspot.ui.main

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rikka.shizuku.Shizuku
import xyz.ggorg.easyspot.service.ServiceState
import xyz.ggorg.easyspot.shizuku.ShizukuUtils
import xyz.ggorg.easyspot.ui.components.ServiceStatusIcon
import xyz.ggorg.easyspot.ui.components.StatusList
import xyz.ggorg.easyspot.ui.components.Warning

@Composable
fun MainUi(
    mainVm: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val serviceConnectionState by mainVm.serviceConnectionState.collectAsStateWithLifecycle()
    if (!serviceConnectionState) {
        Warning("Service disconnected", modifier = Modifier.fillMaxSize())

        return
    }

    val serviceState by mainVm.serviceState.collectAsStateWithLifecycle()
    val isRunning by mainVm.isRunning.collectAsStateWithLifecycle()

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

    val errorToast = Toast.makeText(context, "How did you get here?", Toast.LENGTH_LONG)

    Column(modifier = modifier.padding(12.dp)) {
        StatusList(
            serviceState = serviceState,
            onFixBluetooth = {
                when (serviceState.bluetooth) {
                    ServiceState.BluetoothState.Off -> {
                        bluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }

                    ServiceState.BluetoothState.NoPermission -> {
                        permissionLauncher.launch(ServiceState.BluetoothState.PERMISSIONS)
                    }

                    else -> {
                        errorToast.show()
                    }
                }
            },
            onFixShizuku = {
                when (serviceState.shizuku) {
                    ServiceState.ShizukuState.NotInstalled -> {
                        ShizukuUtils.openPlayStoreListing(context)
                    }

                    ServiceState.ShizukuState.NotRunning -> {
                        ShizukuUtils.startShizukuActivity(context)
                    }

                    ServiceState.ShizukuState.NoPermission -> {
                        Shizuku.requestPermission(System.currentTimeMillis().toInt())
                    }

                    else -> {
                        errorToast.show()
                    }
                }
            },
            onFixNotification = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                }
            },
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ServiceStatusIcon(
                status = isRunning,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Text(
                text = if (isRunning) "All good!" else "Requirements not met",
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
            )

            AnimatedVisibility(isRunning) {
                Text("You can now use the EasySpot client.")
            }
        }
    }
}
