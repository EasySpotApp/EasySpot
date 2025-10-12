package xyz.ggorg.easyspot.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import xyz.ggorg.easyspot.R

class BleService : Service() {
    companion object {
        private const val SERVICE_CHANNEL_ID = "HotspotServiceChannel"
    }

    private var isForeground: Boolean = false

    private val serviceState = MutableStateFlow(ServiceState())
    private var isRunning = MutableStateFlow(false)

    private lateinit var bluetoothStateReceiver: BluetoothStateReceiver
    private lateinit var bluetoothGattServer: GattServer
    private lateinit var bluetoothLeAdvertiser: Advertiser
    private lateinit var shizukuStateReceiver: ShizukuStateReceiver

    override fun onCreate() {
        super.onCreate()

        Timber.d("Creating service")

        ContextCompat
            .getSystemService(this, NotificationManager::class.java)
            ?.createNotificationChannel(
                NotificationChannel(
                    SERVICE_CHANNEL_ID,
                    "Hotspot Service Channel",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )

        bluetoothStateReceiver = BluetoothStateReceiver(this)
        bluetoothGattServer = GattServer(this)
        bluetoothLeAdvertiser = Advertiser(this)
        shizukuStateReceiver = ShizukuStateReceiver(this)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Timber.d("Start command received")

        shizukuStateReceiver.register()

        updateState()

        return START_STICKY
    }

    private fun startForeground() {
        if (isForeground) return

        Timber.d("Starting foreground service")

        val notification =
            Notification
                .Builder(this, SERVICE_CHANNEL_ID)
                .setContentTitle("EasySpot")
                .setContentText("Service is running")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build()

        ServiceCompat.startForeground(
            this,
            System.currentTimeMillis().toInt(),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )

        bluetoothStateReceiver.register()

        isForeground = true
    }

    override fun onBind(intent: Intent): IBinder? = BleServiceBinder()

    fun updateState() {
        serviceState.update {
            val state = ServiceState.getState(this)

            if (state.bluetooth > ServiceState.BluetoothState.NoPermission) {
                startForeground()
            } else {
                stopForeground()
            }

            if (isForeground && state.isAllGood()) {
                start()
            } else {
                stop()
            }

            return@update state
        }
    }

    @SuppressLint("MissingPermission")
    private fun start() {
        if (isRunning.value || !isForeground) return

        bluetoothGattServer.start()
        bluetoothLeAdvertiser.start()

        isRunning.update { true }
    }

    @SuppressLint("MissingPermission")
    private fun stop() {
        if (!isRunning.value) return

        bluetoothLeAdvertiser.stop()
        bluetoothGattServer.stop()

        isRunning.update { false }
    }

    private fun stopForeground() {
        bluetoothStateReceiver.unregister()

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)

        isForeground = false
    }

    override fun onDestroy() {
        super.onDestroy()

        Timber.d("Destroying service")

        shizukuStateReceiver.unregister()
        stop()
        stopForeground()
    }

    inner class BleServiceBinder : Binder() {
        val serviceState = this@BleService.serviceState.asStateFlow()
        val isRunning = this@BleService.isRunning.asStateFlow()

        fun updateState() = this@BleService.updateState()
    }
}
