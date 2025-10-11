package xyz.ggorg.easyspot.service

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

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

                Log.d(this.toString(), "Advertising started successfully")
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)

                Log.e(this.toString(), "Advertising failed with error code: $errorCode")
            }
        }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    fun start() {
        val settings =
            AdvertiseSettings
                .Builder()
                .apply {
                    setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                    setConnectable(true)
                    setTimeout(0)
                    setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
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
