package xyz.ggorg.easyspot.service

import android.util.Log
import rikka.shizuku.Shizuku

class ShizukuStateReceiver(
    private val bleService: BleService,
) : Shizuku.OnBinderReceivedListener,
    Shizuku.OnBinderDeadListener,
    Shizuku.OnRequestPermissionResultListener {
    private var isRegistered: Boolean = false

    fun register() {
        if (isRegistered) return

        isRegistered = true

        Shizuku.addBinderReceivedListener(this)
        Shizuku.addBinderDeadListener(this)
        Shizuku.addRequestPermissionResultListener(this)

        Log.d(this.toString(), "Shizuku state receiver registered")
    }

    override fun onBinderDead() {
        bleService.updateState()
    }

    override fun onBinderReceived() {
        bleService.updateState()
    }

    override fun onRequestPermissionResult(
        requestCode: Int,
        grantResult: Int,
    ) {
        bleService.updateState()
    }

    fun unregister() {
        if (!isRegistered) return

        Shizuku.removeBinderReceivedListener(this)
        Shizuku.removeBinderDeadListener(this)
        Shizuku.removeRequestPermissionResultListener(this)

        isRegistered = false

        Log.d(this.toString(), "Shizuku state receiver unregistered")
    }
}
