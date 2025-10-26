/*
 * Some code in this file is adapted from https://github.com/supershadoe/delta/blob/81c5cb75811eac49afc69fb7aeff560d353498f4/data/src/main/kotlin/dev/shadoe/delta/data/softap/callbacks/SoftApCallback.kt, which is distributed under the following license:
 *
 * Copyright 2024, 2025 supershadoe
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package xyz.ggorg.easyspot.service.softap.shizuku

import android.net.TetheringManagerHidden
import android.net.TetheringManagerHidden.TETHERING_WIFI
import android.net.wifi.ISoftApCallback
import android.net.wifi.IWifiManager
import android.net.wifi.SoftApCapability
import android.net.wifi.SoftApInfo
import android.net.wifi.SoftApState
import android.net.wifi.WifiClient
import android.os.Binder
import android.os.Build
import dev.rikka.tools.refine.Refine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import xyz.ggorg.easyspot.shizuku.SystemServiceHelper

class SoftApStateListener(
    private val softApState: MutableStateFlow<Int>,
) : ISoftApCallback.Stub() {
    private var wifiManager: IWifiManager? = null

    private var isRegistered: Boolean = false

    fun register() {
        if (isRegistered) return

        isRegistered = true

        wifiManager = IWifiManager.Stub.asInterface(SystemServiceHelper.getSystemService("wifi"))

        try {
            wifiManager?.registerSoftApCallback(this)
        } catch (_: NoSuchMethodException) {
            wifiManager?.registerSoftApCallback(
                Binder(),
                this,
                this.hashCode(),
            )
        } catch (_: NoSuchMethodException) {
            wifiManager?.registerSoftApCallback(Binder(), this)
        }
    }

    override fun onStateChanged(state: SoftApState?) {
        state ?: return
        Refine
            .unsafeCast<TetheringManagerHidden.TetheringRequest?>(
                state.tetheringRequest,
            )?.parcel
            ?.tetheringType
            ?.let { it == TETHERING_WIFI }
            .takeIf { it == true }
            .let { softApState.update { state.state } }
    }

    @Deprecated("Removed in API 35")
    override fun onStateChanged(
        state: Int,
        failureReason: Int,
    ) {
        softApState.update { state }
    }

    override fun onConnectedClientsOrInfoChanged(
        infos: Map<String?, SoftApInfo?>?,
        clients: Map<String?, List<WifiClient?>?>?,
        isBridged: Boolean,
        isRegistration: Boolean,
    ) {
    }

    @Deprecated("Deprecated in Java")
    override fun onConnectedClientsChanged(clients: List<WifiClient?>?) {
    }

    @Deprecated("Deprecated in Java")
    override fun onInfoChanged(softApInfo: SoftApInfo?) {
    }

    @Deprecated("Deprecated in Java")
    override fun onInfoListChanged(softApInfoList: List<SoftApInfo?>?) {
    }

    override fun onCapabilityChanged(capability: SoftApCapability?) {}

    override fun onBlockedClientConnecting(
        client: WifiClient?,
        blockedReason: Int,
    ) {
    }

    override fun onClientsDisconnected(
        info: SoftApInfo?,
        clients: List<WifiClient?>?,
    ) {
    }

    fun unregister() {
        if (!isRegistered) return

        isRegistered = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            wifiManager?.unregisterSoftApCallback(this)
        } else {
            @Suppress("DEPRECATION")
            wifiManager?.unregisterSoftApCallback(this.hashCode())
        }
    }
}
