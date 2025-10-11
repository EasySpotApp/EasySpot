package xyz.ggorg.easyspot

import android.Manifest
import android.os.Build
import xyz.ggorg.easyspot.service.ServiceState

object PermissionUtils {
    val essentialPermissions = arrayOf(ServiceState.BluetoothState.PERMISSIONS).flatten()

    val nonEssentialPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }

    val permissionsToRequest = (essentialPermissions + nonEssentialPermissions).toTypedArray()
}
