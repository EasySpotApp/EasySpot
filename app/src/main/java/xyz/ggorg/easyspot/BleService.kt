package xyz.ggorg.easyspot

import android.Manifest
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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class BleService : Service() {
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

            Log.d(context.toString(), "Bluetooth state receiver registered")
        }

        fun unregister(context: Context) {
            context.unregisterReceiver(this)

            Log.d(context.toString(), "Bluetooth state receiver unregistered")
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
        override fun onReceive(context: Context, intent: Intent) {
            val permissionsGranted = MainActivity.essentialPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
            if (!permissionsGranted) {
                return
            }

            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when (state) {
                BluetoothAdapter.STATE_OFF -> {
                    Log.d(context.toString(), "Bluetooth is off")
                    stopAdvertising()
                    stopServer()
                }

                BluetoothAdapter.STATE_TURNING_OFF -> {
                    Log.d(context.toString(), "Bluetooth is turning off")
                    stopAdvertising()
                    stopServer()
                }

                BluetoothAdapter.STATE_ON -> {
                    Log.d(context.toString(), "Bluetooth is on")
                    startServer()
                    startAdvertising()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(this.toString(), "Starting background service")

        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

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

        Log.d(this.toString(), "Destroying service")

        stopForeground()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(this.toString(), "Start command received")

        if (bluetoothAdapter?.isEnabled == true) {
            startForeground()
        } else {
            Log.e(this.toString(), "$this was started, but Bluetooth is not enabled")
            stopSelf()
        }

        return START_STICKY
    }

    private fun startForeground() {
        if (isForeground) return

        Log.d(this.toString(), "Starting foreground service")

        val permissionsGranted = MainActivity.essentialPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!permissionsGranted) {
            Log.e(this.toString(), "Essential permissions not granted")
            stopSelf()
            return
        }

        if (bluetoothAdapter == null || !packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) || (bluetoothAdapter!!.isEnabled && !bluetoothAdapter!!.isMultipleAdvertisementSupported)) {
            Log.e(this.toString(), "Bluetooth LE Advertising not supported")
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

        bluetoothStateReceiver.register(this)

        if (bluetoothAdapter!!.isEnabled) {
            startServer()
            startAdvertising()
        }
    }

    private fun stopForeground() {
        if (!isForeground) return

        Log.d(this.toString(), "Stopping foreground service")

        bluetoothStateReceiver.unregister(this)

        val permissionsGranted = MainActivity.essentialPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (permissionsGranted) {
            stopAdvertising()
            stopServer()
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        isForeground = false
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
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
                    this@BleService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                with(NotificationManagerCompat.from(this@BleService)) {
                    notify(
                        1, NotificationCompat.Builder(
                            this@BleService,
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

        Log.d(this.toString(), "GATT Server started")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun stopServer() {
        bluetoothGattServer?.close()

        Log.d(this.toString(), "GATT Server stopped")
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d(this.toString(), "Advertising started successfully")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(this.toString(), "Advertising failed with error code: $errorCode")
        }
    }

    private fun startAdvertising() {
        val bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser

        val settings = AdvertiseSettings.Builder().apply {
            setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            setConnectable(true)
            setTimeout(0)
            setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
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