package xyz.ggorg.easyspot.service

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import rikka.shizuku.Shizuku
import xyz.ggorg.easyspot.shizuku.ShizukuUtils

data class ServiceState(
    var bluetooth: BluetoothState = BluetoothState.NoAdapter,
    var shizuku: ShizukuState = ShizukuState.NotInstalled,
    var notificationPermission: Boolean = false,
) {
    enum class BluetoothState {
        NoAdapter,
        NoBle,
        NoAdvertising,
        NoPermission,
        Off,
        On;

        companion object {
            val PERMISSIONS =
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                )

            fun getState(context: Context): BluetoothState {
                val bluetoothAdapter =
                    ContextCompat.getSystemService(context, BluetoothManager::class.java)?.adapter

                return when {
                    bluetoothAdapter == null -> NoAdapter

                    !context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) ->
                        NoBle

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

    enum class ShizukuState {
        NotInstalled,
        NotRunning,
        NoPermission,
        Running;

        companion object {
            fun isInstalled(context: Context): Boolean =
                runCatching {
                        context.packageManager
                            .getApplicationInfo(ShizukuUtils.PACKAGE_NAME, 0)
                            .enabled
                    }
                    .getOrElse { false }

            fun getState(context: Context): ShizukuState =
                when {
                    !isInstalled(context) -> NotInstalled

                    !Shizuku.pingBinder() -> NotRunning

                    Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED ->
                        NoPermission

                    else -> Running
                }
        }
    }

    fun isAllGood(): Boolean = bluetooth == BluetoothState.On && shizuku == ShizukuState.Running

    companion object {
        fun getState(context: Context): ServiceState =
            ServiceState(
                bluetooth = BluetoothState.getState(context),
                shizuku = ShizukuState.getState(context),
                notificationPermission =
                    ContextCompat.checkSelfPermission(
                        context,
                        @SuppressLint("InlinedApi") Manifest.permission.POST_NOTIFICATIONS,
                    ) == PackageManager.PERMISSION_GRANTED,
            )
    }
}
