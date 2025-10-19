package xyz.ggorg.easyspot.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import xyz.ggorg.easyspot.ui.components.settings.SettingsDataStore

class ServiceStarter : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Timber.d("Received intent: ${intent.action} - starting service")

        if (ServiceState.BluetoothState.PERMISSIONS.none {
                ContextCompat.checkSelfPermission(
                    context,
                    it,
                ) == PackageManager.PERMISSION_GRANTED
            }
        ) {
            Timber.w("Missing Bluetooth permissions - not starting service")
            return
        }

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val dataStore = SettingsDataStore(context)
            val startOnBoot = runBlocking { dataStore.startOnBootFlow.first() }

            if (!startOnBoot) {
                Timber.d("Start on boot is disabled - not starting service")
                return
            }
        }

        ContextCompat.startForegroundService(context, Intent(context, BleService::class.java))
    }
}
