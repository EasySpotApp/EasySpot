package xyz.ggorg.easyspot

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class GattServer(
    private val context: Context,
) {
    companion object {
        const val EVENT_CHANNEL_ID = "HotspotEventChannel"
    }

    private var gattServer: BluetoothGattServer? = null

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    init {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val eventChannel = NotificationChannel(
            EVENT_CHANNEL_ID,
            "Hotspot Event Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(eventChannel)
    }

    private val callback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                Log.d(this.toString(), "Device connected: ${device?.address}")
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(this.toString(), "Device disconnected: ${device?.address}")
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )

            Log.d(
                this.toString(),
                "Characteristic ${characteristic?.uuid} write ${value.toString()} by ${device?.address}"
            )

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                with(NotificationManagerCompat.from(context)) {
                    notify(
                        1, NotificationCompat.Builder(
                            context,
                            EVENT_CHANNEL_ID
                        )
                            .apply {
                                setSmallIcon(R.drawable.ic_launcher_foreground)
                                setContentTitle("Characteristic write")
                                setContentText("Characteristic ${characteristic?.uuid} write ${value.toString()} by ${device?.address}")
                                setPriority(NotificationCompat.PRIORITY_HIGH)
                                setAutoCancel(true)
                            }.build()
                    )
                }
            }

            if (responseNeeded) {
                gattServer!!.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    null
                )
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun start() {
        gattServer =
            bluetoothManager.openGattServer(context, callback)
        gattServer?.addService(HotspotProfile.createHotspotService())

        Log.d(this.toString(), "GATT Server started")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun stop() {
        if (gattServer == null) return

        gattServer?.close()
        gattServer = null

        Log.d(this.toString(), "GATT Server stopped")
    }
}