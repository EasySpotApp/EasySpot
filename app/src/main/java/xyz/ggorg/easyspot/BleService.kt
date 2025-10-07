package xyz.ggorg.easyspot

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

class BleService : Service() {
    companion object {
        private const val SERVICE_CHANNEL_ID = "HotspotServiceChannel"

        fun tryStart(context: Context) {
            if (!PermissionUtils.arePermissionsGranted(context)) {
                Log.w(this.toString(), "Not all permissions granted - not starting service")
                return
            }

            val bluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter

            if (bluetoothAdapter == null ||
                !context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) ||
                (bluetoothAdapter.isEnabled && !bluetoothAdapter.isMultipleAdvertisementSupported)
            ) {
                Log.w(this.toString(), "Bluetooth LE Advertising not supported")
                return
            }

            val serviceIntent = Intent(context, BleService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter
        set(_) {}

    private var isForeground = false

    private lateinit var bluetoothStateReceiver: BluetoothStateReceiver
    private lateinit var bluetoothGattServer: GattServer
    private lateinit var bluetoothLeAdvertiser: Advertiser

    override fun onCreate() {
        super.onCreate()

        Log.d(this.toString(), "Creating service")

        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "Hotspot Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(serviceChannel)

        bluetoothStateReceiver = BluetoothStateReceiver(this)
        bluetoothGattServer = GattServer(this)
        bluetoothLeAdvertiser = Advertiser(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.d(this.toString(), "Destroying service")

        stopForeground()
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(this.toString(), "Start command received")

        startForeground()
        return START_STICKY
    }

    private fun startForeground() {
        if (isForeground) return

        Log.d(this.toString(), "Starting foreground service")

        if (!PermissionUtils.arePermissionsGranted(this)) {
            Log.e(this.toString(), "Essential permissions not granted")
            stopSelf()
            return
        }

        if (bluetoothAdapter == null ||
            !packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) ||
            (bluetoothAdapter!!.isEnabled && !bluetoothAdapter!!.isMultipleAdvertisementSupported)
        ) {
            Log.e(this.toString(), "Bluetooth LE Advertising not supported")
            stopSelf()
            return
        }

        val notification = Notification.Builder(this, SERVICE_CHANNEL_ID)
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
            start()
        }
    }

    private fun stopForeground() {
        if (!isForeground) return

        Log.d(this.toString(), "Stopping foreground service")

        bluetoothStateReceiver.unregister(this)

        stop()

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        isForeground = false
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!PermissionUtils.arePermissionsGranted(this)) {
            Log.e(this.toString(), "Essential permissions not granted")
            return
        }

        bluetoothGattServer.start()
        bluetoothLeAdvertiser.start()
    }

    @SuppressLint("MissingPermission")
    fun stop() {
        if (!PermissionUtils.arePermissionsGranted(this)) {
            Log.e(this.toString(), "Essential permissions not granted")
            return
        }

        bluetoothLeAdvertiser.stop()
        bluetoothGattServer.stop()
    }
}