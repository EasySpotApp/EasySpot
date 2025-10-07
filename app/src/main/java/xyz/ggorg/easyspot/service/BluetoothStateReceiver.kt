package xyz.ggorg.easyspot.service

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.RequiresPermission
import xyz.ggorg.easyspot.PermissionUtils

class BluetoothStateReceiver(private val bleService: BleService) : BroadcastReceiver() {
    fun register(context: Context) {
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(this, filter)

        Log.d(context.toString(), "Bluetooth state receiver registered")
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(this)

        Log.d(context.toString(), "Bluetooth state receiver unregistered")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun onReceive(context: Context, intent: Intent) {
        if (!PermissionUtils.arePermissionsGranted(context)) {
            return
        }

        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        when (state) {
            BluetoothAdapter.STATE_OFF -> {
                Log.d(context.toString(), "Bluetooth is off")
                bleService.stop()
            }

            BluetoothAdapter.STATE_TURNING_OFF -> {
                Log.d(context.toString(), "Bluetooth is turning off")
                bleService.stop()
            }

            BluetoothAdapter.STATE_ON -> {
                Log.d(context.toString(), "Bluetooth is on")
                bleService.start()
            }
        }
    }
}