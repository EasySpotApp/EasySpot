package xyz.ggorg.easyspot.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import timber.log.Timber
import xyz.ggorg.easyspot.R
import xyz.ggorg.easyspot.service.HotspotProfile.CHARACTERISTIC_UUID
import xyz.ggorg.easyspot.service.HotspotProfile.SERVICE_UUID
import xyz.ggorg.easyspot.shizuku.ShizukuTetherHelper

class GattServer(
    private val context: Context,
) {
    companion object {
        const val EVENT_CHANNEL_ID = "HotspotEventChannel"
    }

    private var gattServer: BluetoothGattServer? = null

    private val bluetoothManager: BluetoothManager? =
        ContextCompat.getSystemService(context, BluetoothManager::class.java)

    init {
        ContextCompat
            .getSystemService(context, NotificationManager::class.java)
            ?.createNotificationChannel(
                NotificationChannel(
                    EVENT_CHANNEL_ID,
                    context.getString(R.string.gattserver_notification_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
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
                    Timber.d("Device connected: ${device?.address}")
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Timber.d("Device disconnected: ${device?.address}")
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

                Timber.d(
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
                                NotificationCompat
                                    .Builder(context, EVENT_CHANNEL_ID)
                                    .apply {
                                        setSmallIcon(R.drawable.ic_launcher_foreground)
                                        setContentTitle(context.getString(R.string.app_name))
                                        setContentText(
                                            context.getString(
                                                R.string.gattserver_notification_enabled,
                                                device?.name ?: device?.address,
                                            ),
                                        )
                                        setPriority(NotificationCompat.PRIORITY_HIGH)
                                        setAutoCancel(true)
                                    }.build(),
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
    fun start(
        encryption: Boolean,
        mitmProtection: Boolean,
    ) {
        gattServer = bluetoothManager?.openGattServer(context, callback)

        val permissions =
            when {
                encryption && mitmProtection -> {
                    BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
                }

                encryption && !mitmProtection -> {
                    BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
                }

                else -> {
                    BluetoothGattCharacteristic.PERMISSION_WRITE
                }
            }

        val service =
            BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY).apply {
                addCharacteristic(
                    BluetoothGattCharacteristic(
                        CHARACTERISTIC_UUID,
                        BluetoothGattCharacteristic.PROPERTY_WRITE,
                        permissions,
                    ),
                )
            }

        gattServer?.addService(service)

        Timber.d("GATT Server started with encryption=$encryption mitmProtection=$mitmProtection")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun stop() {
        if (gattServer == null) return

        gattServer?.close()
        gattServer = null

        Timber.d("GATT Server stopped")
    }
}
