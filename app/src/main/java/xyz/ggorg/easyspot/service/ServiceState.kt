package xyz.ggorg.easyspot.service

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import rikka.shizuku.Shizuku
import xyz.ggorg.easyspot.R
import xyz.ggorg.easyspot.shizuku.ShizukuUtils

data class ServiceState(
    var bluetooth: BluetoothState = BluetoothState.NoAdapter,
    var shizuku: ShizukuState = ShizukuState.NotInstalled,
    var notificationPermission: NotificationState = NotificationState.Denied,
) {
    interface ServiceStateObject {
        fun isOk(): Boolean

        fun isStartAllowed(): Boolean = isOk()

        fun isFixable(): Boolean = !isOk()

        fun getTitleResource(): Int

        fun getStatusResource(): Int

        fun getDescriptionResource(): Int?
    }

    enum class BluetoothState : ServiceStateObject {
        NoAdapter,
        NoBle,
        NoAdvertising,
        NoPermission,
        Off,
        On,
        ;

        override fun isOk(): Boolean = this == On

        override fun isFixable(): Boolean = this >= NoPermission && this != On

        override fun getTitleResource(): Int = R.string.home_statuslist_bluetooth

        override fun getStatusResource(): Int =
            when (this) {
                NoAdapter -> R.string.home_statuslist_bluetooth_noadapter
                NoBle -> R.string.home_statuslist_bluetooth_noble
                NoAdvertising -> R.string.home_statuslist_bluetooth_noadvertising
                NoPermission -> R.string.home_statuslist_bluetooth_nopermission
                Off -> R.string.home_statuslist_bluetooth_off
                On -> R.string.home_statuslist_bluetooth_on
            }

        override fun getDescriptionResource(): Int? =
            when (this) {
                NoAdapter -> R.string.home_statuslist_bluetooth_noadapter_description
                NoBle -> R.string.home_statuslist_bluetooth_noble_description
                NoAdvertising -> R.string.home_statuslist_bluetooth_noadvertising_description
                NoPermission -> R.string.home_statuslist_bluetooth_nopermission_description
                Off -> R.string.home_statuslist_bluetooth_off_description
                On -> null
            }

        companion object {
            val PERMISSIONS =
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )

            fun getState(context: Context): BluetoothState {
                val bluetoothAdapter =
                    ContextCompat
                        .getSystemService(context, BluetoothManager::class.java)
                        ?.adapter

                return when {
                    bluetoothAdapter == null -> NoAdapter

                    !context.packageManager.hasSystemFeature(
                        PackageManager.FEATURE_BLUETOOTH_LE,
                    ) -> NoBle

                    bluetoothAdapter.isEnabled &&
                        !bluetoothAdapter.isMultipleAdvertisementSupported -> NoAdvertising

                    PERMISSIONS.any {
                        ContextCompat.checkSelfPermission(context, it) ==
                            PackageManager.PERMISSION_DENIED
                    } -> NoPermission

                    bluetoothAdapter.isEnabled -> On

                    else -> Off
                }
            }
        }
    }

    enum class ShizukuState : ServiceStateObject {
        NotInstalled,
        NotRunning,
        NoPermission,
        Running,
        ;

        override fun isOk(): Boolean = this == Running

        override fun getTitleResource(): Int = R.string.home_statuslist_shizuku

        override fun getStatusResource(): Int =
            when (this) {
                NotInstalled -> R.string.home_statuslist_shizuku_notinstalled
                NotRunning -> R.string.home_statuslist_shizuku_notrunning
                NoPermission -> R.string.home_statuslist_shizuku_nopermission
                Running -> R.string.home_statuslist_shizuku_running
            }

        override fun getDescriptionResource(): Int? =
            when (this) {
                Running -> null
                else -> R.string.home_statuslist_shizuku_off_description
            }

        companion object {
            fun isInstalled(context: Context): Boolean =
                runCatching {
                    context.packageManager
                        .getApplicationInfo(ShizukuUtils.PACKAGE_NAME, 0)
                        .enabled
                }.getOrElse { false }

            fun getState(context: Context): ShizukuState =
                when {
                    !isInstalled(context) -> NotInstalled

                    !Shizuku.pingBinder() -> NotRunning

                    Shizuku.checkSelfPermission()
                        != PackageManager.PERMISSION_GRANTED -> NoPermission

                    else -> Running
                }
        }
    }

    enum class NotificationState : ServiceStateObject {
        Denied,
        Granted,
        ;

        override fun isOk(): Boolean = this == Granted

        override fun isStartAllowed(): Boolean = true

        override fun getTitleResource(): Int = R.string.home_statuslist_notifications

        override fun getStatusResource(): Int =
            when (this) {
                Denied -> R.string.home_statuslist_notifications_denied
                Granted -> R.string.home_statuslist_notifications_granted
            }

        override fun getDescriptionResource(): Int? =
            when (this) {
                Denied -> R.string.home_statuslist_notifications_denied_description
                Granted -> null
            }

        companion object {
            fun getState(context: Context): NotificationState =
                if (
                    ContextCompat.checkSelfPermission(
                        context,
                        @SuppressLint("InlinedApi") Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    Granted
                } else {
                    Denied
                }
        }
    }

    fun isStartAllowed(): Boolean =
        bluetooth.isStartAllowed() &&
            shizuku.isStartAllowed() &&
            notificationPermission.isStartAllowed()

    companion object {
        fun getState(context: Context): ServiceState =
            ServiceState(
                bluetooth = BluetoothState.getState(context),
                shizuku = ShizukuState.getState(context),
                notificationPermission = NotificationState.getState(context),
            )
    }
}
