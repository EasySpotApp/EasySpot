package xyz.ggorg.easyspot.service

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import timber.log.Timber

class Advertiser(
    context: Context,
) {
    private val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
        ContextCompat
            .getSystemService(context, BluetoothManager::class.java)
            ?.adapter
            ?.bluetoothLeAdvertiser

    private val advertiseCallback =
        object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                super.onStartSuccess(settingsInEffect)

                Timber.d(
                    "Advertising started successfully with mode=${settingsInEffect.mode} txPowerLevel=${settingsInEffect.txPowerLevel}",
                )
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)

                Timber.e("Advertising failed with error code: $errorCode")
            }
        }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun start(
        powerMode: Int,
        txPower: Int,
    ) {
        val settings =
            AdvertiseSettings
                .Builder()
                .apply {
                    setAdvertiseMode(powerMode)
                    setTxPowerLevel(txPower)
                    setConnectable(true)
                    setTimeout(0)
                }.build()

        val data =
            AdvertiseData
                .Builder()
                .apply {
                    setIncludeDeviceName(false)
                    setIncludeTxPowerLevel(true)
                    addServiceUuid(ParcelUuid(HotspotProfile.SERVICE_UUID))
                }.build()

        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun stop() {
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
    }
}
