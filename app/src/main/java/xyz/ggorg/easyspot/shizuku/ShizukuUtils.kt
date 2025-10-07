package xyz.ggorg.easyspot.shizuku

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri
import rikka.shizuku.Shizuku

object ShizukuUtils {
    private const val PACKAGE_NAME = "moe.shizuku.privileged.api"

    private val PLAY_STORE_APP_URI: Uri = "market://details?id=${PACKAGE_NAME}".toUri()
    private val PLAY_STORE_APP_WEBURI: Uri =
        "https://play.google.com/store/apps/details?id=${PACKAGE_NAME}".toUri()

    fun getShizukuState(context: Context): ShizukuState {
        return if (Shizuku.pingBinder()) {
            if (isPermissionGranted()) {
                ShizukuState.RUNNING
            } else {
                ShizukuState.PERMISSION_DENIED
            }
        } else if (!isShizukuInstalled(context)) {
            ShizukuState.NOT_INSTALLED
        } else {
            ShizukuState.NOT_RUNNING
        }
    }

    fun isShizukuInstalled(context: Context): Boolean = try {
        context.packageManager.getApplicationInfo(PACKAGE_NAME, 0).enabled
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun isPermissionGranted(): Boolean {
        return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(requestCode: Int) {
        Shizuku.requestPermission(requestCode)
    }

    fun openPlayStoreListing(context: Context) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(PLAY_STORE_APP_URI)
            )
        } catch (_: ActivityNotFoundException) {
            val i = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(PLAY_STORE_APP_WEBURI)

            if (i.resolveActivity(context.packageManager) != null) {
                context.startActivity(i)
            }
        }
    }

    fun startShizukuActivity(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(
                        PACKAGE_NAME, "moe.shizuku.manager.MainActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    fun isRunning(context: Context): Boolean = getShizukuState(context) == ShizukuState.RUNNING
}

enum class ShizukuState {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_DENIED,
    RUNNING
}
