package xyz.ggorg.easyspot.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import xyz.ggorg.easyspot.HotspotProfile
import xyz.ggorg.easyspot.R
import xyz.ggorg.easyspot.shizuku.ShizukuTetherHelper

class GattServer(private val context: Context) {
    companion object {
        const val EVENT_CHANNEL_ID = "HotspotEventChannel"
    }

    private var gattServer: BluetoothGattServer? = null

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    init {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val eventChannel =
            NotificationChannel(
                EVENT_CHANNEL_ID,
                "Hotspot Event Channel",
                NotificationManager.IMPORTANCE_HIGH,
            )
        notificationManager.createNotificationChannel(eventChannel)
    }

    private val callback =
        object : BluetoothGattServerCallback() {
            override fun onConnectionStateChange(
                device: BluetoothDevice?,
                status: Int,
                newState: Int,
            ) {
                super.onConnectionStateChange(device, status, newState)
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(this.toString(), "Device connected: ${device?.address}")
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
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
                value: ByteArray?,
            ) {
                super.onCharacteristicWriteRequest(
                    device,
                    requestId,
                    characteristic,
                    preparedWrite,
                    responseNeeded,
                    offset,
                    value,
                )

                val formattedValue =
                    value?.joinToString(separator = " ") { String.format("%02X", it) }

                Log.d(
                    this.toString(),
                    "Characteristic ${characteristic?.uuid} write $formattedValue by ${device?.address} (${device?.name})",
                )

                val newHotspotState =
                    when (value?.firstOrNull()) {
                        0x00.toByte() -> false
                        0x01.toByte() -> true
                        else -> null
                    }

                if (newHotspotState != null) {
                    if (
                        ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        with(NotificationManagerCompat.from(context)) {
                            notify(
                                System.currentTimeMillis().toInt(),
                                NotificationCompat.Builder(context, EVENT_CHANNEL_ID)
                                    .apply {
                                        setSmallIcon(R.drawable.ic_launcher_foreground)
                                        setContentTitle("EasySpot")
                                        setContentText(
                                            "Hotspot got ${
                                            if (newHotspotState) {
                                                "enabled"
                                            } else {
                                                "disabled"
                                            }
                                        } by ${
                                            device?.name ?: device?.address
                                        }!"
                                        )
                                        setPriority(NotificationCompat.PRIORITY_HIGH)
                                        setAutoCancel(true)
                                    }
                                    .build(),
                            )
                        }
                    }

                    ShizukuTetherHelper.setHotspotEnabledShizuku(newHotspotState)
                }

                if (responseNeeded) {
                    gattServer!!.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null,
                    )
                }
            }
        }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun start() {
        gattServer = bluetoothManager.openGattServer(context, callback)
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
