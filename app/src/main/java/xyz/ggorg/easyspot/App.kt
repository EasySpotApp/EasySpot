package xyz.ggorg.easyspot

import android.app.Application
import org.lsposed.hiddenapibypass.HiddenApiBypass
import timber.log.Timber

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())

        HiddenApiBypass.addHiddenApiExemptions("Landroid/net")
    }
}
