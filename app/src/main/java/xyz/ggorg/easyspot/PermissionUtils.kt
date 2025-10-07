package xyz.ggorg.easyspot

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import xyz.ggorg.easyspot.shizuku.ShizukuUtils

object PermissionUtils {
    val essentialPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
    )

    val nonEssentialPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

    val permissionsToRequest = essentialPermissions + nonEssentialPermissions

    fun arePermissionsGranted(context: Context, essential: Boolean = true): Boolean {
        val permissionsToCheck = if (essential) {
            essentialPermissions
        } else {
            permissionsToRequest
        }

        return permissionsToCheck.all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        } && ShizukuUtils.isRunning(context)
    }
}