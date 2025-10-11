package xyz.ggorg.easyspot.service

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat

class ServiceStarter : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Log.d(this.toString(), "Received intent: ${intent.action} - starting service")

        if (ServiceState.BluetoothState.PERMISSIONS.none {
                ContextCompat.checkSelfPermission(
                    context,
                    it,
                ) == PackageManager.PERMISSION_GRANTED
            }
        ) {
            Log.w(this.toString(), "Missing Bluetooth permissions - not starting service")
            return
        }

        ContextCompat.startForegroundService(context, Intent(context, BleService::class.java))
    }
}
