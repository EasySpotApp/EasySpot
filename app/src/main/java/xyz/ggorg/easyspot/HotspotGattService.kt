package xyz.ggorg.easyspot

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import java.util.UUID

object HotspotGattService {
    val SERVICE_UUID: UUID = UUID.fromString("7baad717-1551-45e1-b852-78d20c7211ec")

    val CHARACTERISTIC_UUID: UUID = UUID.fromString("47436878-5308-40f9-9c29-82c2cb87f595")

    fun createHotspotService(): BluetoothGattService {
        return BluetoothGattService(
            SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        ).apply {
            addCharacteristic(
                BluetoothGattCharacteristic(
                    CHARACTERISTIC_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE
                )
            )
        }
    }
}