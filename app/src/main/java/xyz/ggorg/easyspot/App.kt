package xyz.ggorg.easyspot

import android.app.Application
import org.lsposed.hiddenapibypass.HiddenApiBypass

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        HiddenApiBypass.addHiddenApiExemptions("Landroid/net")
    }
}
