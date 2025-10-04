package xyz.ggorg.easyspot

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class HotspotService : Service() {
    companion object {
        private const val SERVICE_CHANNEL_ID = "HotspotServiceChannel"

        private const val EVENT_CHANNEL_ID = "HotspotEventChannel"
    }

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter
        set(_) {}

    private var bluetoothGattServer: BluetoothGattServer? = null

    private var isForeground = false

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        fun register(context: Context) {
            val filter = android.content.IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            context.registerReceiver(this, filter)
        }

        fun unregister(context: Context) {
            context.unregisterReceiver(this)
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when (state) {
                BluetoothAdapter.STATE_OFF -> {
                    Log.d("HotspotService", "Bluetooth is off")
                    stopForeground()
                }

                BluetoothAdapter.STATE_TURNING_OFF -> {
                    Log.d("HotspotService", "Bluetooth is turning off")
                    stopForeground()
                }

                BluetoothAdapter.STATE_ON -> {
                    Log.d("HotspotService", "Bluetooth is on")
                    startForeground()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("HotspotService", "Starting background service")

        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothStateReceiver.register(this)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "Hotspot Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(serviceChannel)

        val eventChannel = NotificationChannel(
            EVENT_CHANNEL_ID,
            "Hotspot Event Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(eventChannel)
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d("HotspotService", "Destroying service")

        stopForeground()
        bluetoothStateReceiver.unregister(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("HotspotService", "Start command received")

        if (bluetoothAdapter?.isEnabled == true) {
            startForeground()
        }
        return START_STICKY
    }

    private fun startForeground() {
        if (isForeground) return

        Log.d("HotspotService", "Starting foreground service")

        val permissionsGranted = MainActivity.essentialPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!permissionsGranted) {
            Log.e("HotspotService", "Essential permissions not granted")
            stopSelf()
            return
        }

        if (bluetoothAdapter == null || !packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) || !bluetoothAdapter!!.isMultipleAdvertisementSupported) {
            Log.e("HotspotService", "Bluetooth LE Advertising not supported")
            stopSelf()
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            Log.e("HotspotService", "Bluetooth is not enabled")
            stopSelf()
            return
        }

        val notification = android.app.Notification.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle("EasySpot")
            .setContentText("Service is running")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        ServiceCompat.startForeground(
            this,
            System.currentTimeMillis().toInt(),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )

        isForeground = true

        startServer()
        startAdvertising()
    }

    private fun stopForeground() {
        if (!isForeground) return

        Log.d("HotspotService", "Stopping foreground service")

        val permissionsGranted = MainActivity.essentialPermissions.all {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsGranted) {
            stopAdvertising()
            @SuppressLint("MissingPermission")
            stopServer()
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        isForeground = false
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                Log.d("HotspotService", "Device connected: ${device?.address}")
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("HotspotService", "Device disconnected: ${device?.address}")
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
                "HotspotService",
                "Characteristic ${characteristic?.uuid} write ${value.toString()} by ${device?.address}"
            )

            with(NotificationManagerCompat.from(this@HotspotService)) {
                @SuppressLint("MissingPermission")
                notify(
                    1, NotificationCompat.Builder(
                        this@HotspotService,
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

            if (responseNeeded) {
                bluetoothGattServer?.sendResponse(
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
    private fun startServer() {
        bluetoothGattServer =
            bluetoothManager?.openGattServer(applicationContext, gattServerCallback)
        bluetoothGattServer?.addService(HotspotProfile.createHotspotService())

        Log.d("HotspotService", "GATT Server started")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun stopServer() {
        bluetoothGattServer?.close()

        Log.d("HotspotService", "GATT Server stopped")
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d("HotspotService", "Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e("HotspotService", "Advertising failed with error code: $errorCode")
        }
    }

    private fun startAdvertising() {
        val bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        val settings = AdvertiseSettings.Builder().apply {
            setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            setConnectable(true)
            setTimeout(0)
            setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        }.build()

        val data = AdvertiseData.Builder().apply {
            setIncludeDeviceName(false)
            setIncludeTxPowerLevel(true)
            addServiceUuid(ParcelUuid(HotspotProfile.SERVICE_UUID))
        }.build()

        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun stopAdvertising() {
        val bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
    }
}