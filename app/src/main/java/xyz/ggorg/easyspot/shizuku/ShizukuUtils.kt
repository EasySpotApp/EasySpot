package xyz.ggorg.easyspot.shizuku

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri

object ShizukuUtils {
    const val PACKAGE_NAME = "moe.shizuku.privileged.api"

    private val PLAY_STORE_APP_URI: Uri = "market://details?id=${PACKAGE_NAME}".toUri()
    private val PLAY_STORE_APP_WEBURI: Uri =
        "https://play.google.com/store/apps/details?id=${PACKAGE_NAME}".toUri()

    fun openPlayStoreListing(context: Context) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(PLAY_STORE_APP_URI),
            )
        } catch (_: ActivityNotFoundException) {
            val intent =
                Intent(Intent.ACTION_VIEW)
                    .addCategory(Intent.CATEGORY_BROWSABLE)
                    .setData(PLAY_STORE_APP_WEBURI)

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        }
    }

    fun startShizukuActivity(context: Context) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(PACKAGE_NAME, "moe.shizuku.manager.MainActivity")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        } catch (_: Exception) {
            openPlayStoreListing(context)
        }
    }
}
