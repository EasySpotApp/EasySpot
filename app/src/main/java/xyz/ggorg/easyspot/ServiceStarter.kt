package xyz.ggorg.easyspot

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import rikka.shizuku.Shizuku

class ServiceStarter : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(this.toString(), "Received intent: ${intent.action} - starting service")

        val permissionsGranted = MainActivity.essentialPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!permissionsGranted || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Log.w(this.toString(), "Not all permissions granted - not starting service")
            return
        }

        val serviceIntent = Intent(context, BleService::class.java)
        context.startForegroundService(serviceIntent)
    }
}