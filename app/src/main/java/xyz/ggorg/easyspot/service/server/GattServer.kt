package xyz.ggorg.easyspot.service.server

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManagerHidden
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.ggorg.easyspot.R
import xyz.ggorg.easyspot.service.softap.shizuku.SoftApController

class GattServer(
    private val context: Context,
    private val softApController: SoftApController,
    private val softApState: StateFlow<Int>,
) {
    companion object {
        const val EVENT_CHANNEL_ID = "HotspotEventChannel"
    }

    private var gattServer: BluetoothGattServer? = null

    private val bluetoothManager: BluetoothManager? =
        ContextCompat.getSystemService(context, BluetoothManager::class.java)

    private var subscribedDevices = mutableSetOf<BluetoothDevice>()

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
                val state =
                    when (newState) {
                        BluetoothProfile.STATE_DISCONNECTED -> "STATE_DISCONNECTED"
                        BluetoothProfile.STATE_CONNECTING -> "STATE_CONNECTING"
                        BluetoothProfile.STATE_CONNECTED -> "STATE_CONNECTED"
                        BluetoothProfile.STATE_DISCONNECTING -> "STATE_DISCONNECTING"
                        else -> "STATE_UNKNOWN"
                    }

                Timber.d("Device ${device?.address}: $state")
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
                if (characteristic?.uuid != HotspotProfile.CHARACTERISTIC_UUID) {
                    Timber.w("Unknown characteristic write request: ${characteristic?.uuid}")
                    return
                }

                val formattedValue =
                    value?.joinToString(
                        separator = " ",
                    ) { String.format("%02X", it) }

                Timber.d(
                    "Characteristic ${characteristic.uuid} write $formattedValue by ${device?.address} (${device?.name})",
                )

                val newHotspotState =
                    when (value?.firstOrNull()) {
                        0x00.toByte() -> false
                        0x01.toByte() -> true
                        else -> null
                    }

                val result =
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
                                                    if (newHotspotState) {
                                                        R.string.gattserver_notification_enabled
                                                    } else {
                                                        R.string.gattserver_notification_disabled
                                                    },
                                                    device?.name ?: device?.address,
                                                ),
                                            )
                                            setPriority(NotificationCompat.PRIORITY_HIGH)
                                            setAutoCancel(true)
                                        }.build(),
                                )
                            }
                        }

                        when (newHotspotState) {
                            true -> softApController.startSoftAp()
                            false -> softApController.stopSoftAp()
                        }
                    } else {
                        false
                    }

                if (responseNeeded) {
                    Timber.d("Responding to write request")
                    gattServer!!.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        byteArrayOf(if (result) 0x01 else 0x00),
                    )
                } else {
                    Timber.d("No response needed for write request")
                }
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onCharacteristicReadRequest(
                device: BluetoothDevice?,
                requestId: Int,
                offset: Int,
                characteristic: BluetoothGattCharacteristic?,
            ) {
                if (characteristic?.uuid != HotspotProfile.CHARACTERISTIC_UUID) {
                    Timber.w("Unknown characteristic write request: ${characteristic?.uuid}")
                    return
                }

                Timber.d(
                    "Characteristic ${characteristic.uuid} read by ${device?.address} (${device?.name})",
                )

                val hotspotState =
                    when (softApState.value) {
                        WifiManagerHidden.WIFI_AP_STATE_ENABLED -> true
                        else -> false
                    }

                gattServer!!.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    byteArrayOf(if (hotspotState) 0x01 else 0x00),
                )
            }

            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onDescriptorWriteRequest(
                device: BluetoothDevice?,
                requestId: Int,
                descriptor: BluetoothGattDescriptor?,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray?,
            ) {
                if (descriptor?.characteristic?.uuid != HotspotProfile.CHARACTERISTIC_UUID) {
                    Timber.w("Unknown descriptor write request: ${descriptor?.uuid}")
                    return
                }

                when {
                    value?.contentEquals(
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
                    ) == true -> {
                        Timber.d("Device ${device?.address} subscribed to notifications")
                        device?.let { subscribedDevices.add(it) }
                    }

                    value?.contentEquals(
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE,
                    ) == true -> {
                        Timber.d("Device ${device?.address} unsubscribed from notifications")
                        device?.let { subscribedDevices.remove(it) }
                    }

                    else -> {
                        Timber.w("Unknown descriptor value: ${value?.joinToString()}")
                    }
                }

                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
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
                    BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM or
                        BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
                }

                encryption && !mitmProtection -> {
                    BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED or
                        BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
                }

                else -> {
                    BluetoothGattCharacteristic.PERMISSION_WRITE or
                        BluetoothGattCharacteristic.PERMISSION_READ
                }
            }
        val descriptor =
            BluetoothGattDescriptor(
                HotspotProfile.DESCRIPTOR_UUID,
                permissions,
            )

        val characteristic =
            BluetoothGattCharacteristic(
                HotspotProfile.CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or
                    BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                permissions,
            ).apply {
                addDescriptor(descriptor)
            }

        val service =
            BluetoothGattService(
                HotspotProfile.SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY,
            ).apply {
                addCharacteristic(characteristic)
            }

        gattServer?.addService(service)

        coroutineScope.launch {
            softApState.collectLatest { state ->
                val hotspotState =
                    when (state) {
                        WifiManagerHidden.WIFI_AP_STATE_ENABLED -> true
                        else -> false
                    }

                for (device in subscribedDevices) {
                    Timber.d(
                        "Notifying device ${device.address} of hotspot state change: $hotspotState",
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gattServer?.notifyCharacteristicChanged(
                            device,
                            characteristic,
                            false,
                            byteArrayOf(if (hotspotState) 0x01 else 0x00),
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        {
                            characteristic.value = byteArrayOf(if (hotspotState) 0x01 else 0x00)
                            gattServer?.notifyCharacteristicChanged(
                                device,
                                characteristic,
                                false,
                            )
                        }
                    }
                }
            }
        }

        Timber.d("GATT Server started with encryption=$encryption mitmProtection=$mitmProtection")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun stop() {
        if (gattServer == null) return

        coroutineScope.cancel()
        gattServer?.close()
        gattServer = null

        Timber.d("GATT Server stopped")
    }
}
