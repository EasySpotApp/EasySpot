package xyz.ggorg.easyspot.service

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.RequiresPermission

class BluetoothStateReceiver(
    private val bleService: BleService,
) : BroadcastReceiver() {
    private var isRegistered: Boolean = false

    fun register() {
        if (isRegistered) return

        isRegistered = true

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        bleService.registerReceiver(this, filter)

        Log.d(this.toString(), "Bluetooth state receiver registered")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        bleService.updateState()
    }

    fun unregister() {
        if (!isRegistered) return

        bleService.unregisterReceiver(this)

        isRegistered = false

        Log.d(this.toString(), "Bluetooth state receiver unregistered")
    }
}
