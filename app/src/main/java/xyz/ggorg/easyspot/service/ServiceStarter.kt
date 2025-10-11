package xyz.ggorg.easyspot.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import timber.log.Timber

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

        ContextCompat.startForegroundService(context, Intent(context, BleService::class.java))
    }
}
