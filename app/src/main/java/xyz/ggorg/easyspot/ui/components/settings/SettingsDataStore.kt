package xyz.ggorg.easyspot.ui.components.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(
    private val context: Context,
) {
    companion object {
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")

        val BLE_ENCRYPTION = booleanPreferencesKey("ble_encryption")

        val BLE_MITM_PROTECTION = booleanPreferencesKey("ble_mitm_protection")

        val ADVERTISING_POWER_MODE = intPreferencesKey("advertising_power_mode")

        val ADVERTISING_TX_POWER = intPreferencesKey("advertising_tx_power")
    }

    val startOnBootFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[START_ON_BOOT] ?: true
        }

    suspend fun setStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[START_ON_BOOT] = enabled
        }
    }

    val bleEncryptionFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[BLE_ENCRYPTION] ?: true
        }

    suspend fun setBleEncryption(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BLE_ENCRYPTION] = enabled
        }
    }

    val bleMitmProtectionFlow: Flow<Boolean> =
        context.dataStore.data.map { preferences ->
            preferences[BLE_MITM_PROTECTION] ?: false
        }

    suspend fun setBleMitmProtection(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BLE_MITM_PROTECTION] = enabled
        }
    }

    val advertisingPowerModeFlow: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[ADVERTISING_POWER_MODE] ?: 0
        }

    suspend fun setAdvertisingPowerMode(mode: Int) {
        context.dataStore.edit { preferences ->
            preferences[ADVERTISING_POWER_MODE] = mode
        }
    }

    val advertisingTxPowerFlow: Flow<Int> =
        context.dataStore.data.map { preferences ->
            preferences[ADVERTISING_TX_POWER] ?: 1
        }

    suspend fun setAdvertisingTxPower(txPower: Int) {
        context.dataStore.edit { preferences ->
            preferences[ADVERTISING_TX_POWER] = txPower
        }
    }
}
