package xyz.ggorg.easyspot.ui.main

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rikka.shizuku.Shizuku
import xyz.ggorg.easyspot.R
import xyz.ggorg.easyspot.service.ServiceState
import xyz.ggorg.easyspot.shizuku.ShizukuUtils
import xyz.ggorg.easyspot.ui.components.ServiceStatusHeader
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
        Warning(
            stringResource(R.string.home_service_disconnected),
            modifier = Modifier.fillMaxSize(),
        )

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

    val unfixableErrorToast =
        Toast.makeText(
            context,
            stringResource(R.string.home_unfixable_error),
            Toast.LENGTH_LONG,
        )

    Column(
        modifier = modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ServiceStatusHeader(status = isRunning)

        StatusList(
            modifier = Modifier.verticalScroll(rememberScrollState()),
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
                        unfixableErrorToast.show()
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
                        unfixableErrorToast.show()
                    }
                }
            },
            onFixNotification = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                }
            },
        )
    }
}
