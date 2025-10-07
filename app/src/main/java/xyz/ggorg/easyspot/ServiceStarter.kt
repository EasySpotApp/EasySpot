package xyz.ggorg.easyspot

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ServiceStarter : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(this.toString(), "Received intent: ${intent.action} - starting service")

        BleService.tryStartForeground(context)
    }
}