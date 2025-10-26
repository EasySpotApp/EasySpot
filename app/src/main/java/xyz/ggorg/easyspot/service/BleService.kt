package xyz.ggorg.easyspot.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManagerHidden
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import xyz.ggorg.easyspot.R
import xyz.ggorg.easyspot.service.server.Advertiser
import xyz.ggorg.easyspot.service.server.GattServer
import xyz.ggorg.easyspot.service.softap.shizuku.SoftApController
import xyz.ggorg.easyspot.service.softap.shizuku.SoftApStateListener
import xyz.ggorg.easyspot.shizuku.ShizukuStateReceiver
import xyz.ggorg.easyspot.ui.components.settings.SettingsDataStore
import xyz.ggorg.easyspot.ui.main.MainActivity

class BleService : Service() {
    companion object {
        private const val SERVICE_CHANNEL_ID = "HotspotServiceChannel"
    }

    private var isForeground: Boolean = false

    private val serviceState = MutableStateFlow(ServiceState())
    private var isRunning = MutableStateFlow(false)

    private val softApEnabledState = MutableStateFlow(WifiManagerHidden.WIFI_AP_STATE_DISABLED)

    private val softApController = SoftApController(softApEnabledState.asStateFlow())
    private val softApStateListener = SoftApStateListener(softApEnabledState)

    private lateinit var settingsDataStore: SettingsDataStore
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
                    getString(R.string.service_notification_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    setShowBadge(false)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_SECRET
                },
            )

        settingsDataStore = SettingsDataStore(this)
        bluetoothStateReceiver = BluetoothStateReceiver(this)
        bluetoothGattServer = GattServer(this, softApController, softApEnabledState.asStateFlow())
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

    private fun createNotification(): Notification {
        val notificationIntent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE,
            )

        val textRes =
            if (isRunning.value) {
                R.string.service_notification_running
            } else {
                R.string.service_notification_not_running
            }

        return NotificationCompat
            .Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(textRes))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun startForeground() {
        if (isForeground) return

        Timber.d("Starting foreground service")

        val notification = createNotification()

        ServiceCompat.startForeground(
            this,
            1,
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

            if (isForeground && state.isStartAllowed()) {
                start()
            } else {
                stop()
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(this).notify(1, createNotification())
            }

            return@update state
        }
    }

    @SuppressLint("MissingPermission")
    private fun start() {
        if (isRunning.value || !isForeground) return

        isRunning.update { true }

        // TODO: Time of check, time of use bug?
        // Also maybe race condition?
        runBlocking {
            val bleEncryption = settingsDataStore.bleEncryptionFlow.first()
            val bleMitmProtection = settingsDataStore.bleMitmProtectionFlow.first()
            val advertisingPowerMode = settingsDataStore.advertisingPowerModeFlow.first()
            val advertisingTxPower = settingsDataStore.advertisingTxPowerFlow.first()

            softApStateListener.register()
            bluetoothGattServer.start(bleEncryption, bleMitmProtection)
            bluetoothLeAdvertiser.start(advertisingPowerMode, advertisingTxPower)
        }
    }

    @SuppressLint("MissingPermission")
    private fun stop() {
        if (!isRunning.value) return

        bluetoothLeAdvertiser.stop()
        bluetoothGattServer.stop()
        softApStateListener.unregister()

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

        fun stop() = this@BleService.stop()
    }
}
