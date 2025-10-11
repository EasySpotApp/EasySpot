package xyz.ggorg.easyspot.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import timber.log.Timber
import xyz.ggorg.easyspot.service.BleService
import xyz.ggorg.easyspot.ui.theme.EasySpotTheme

class MainActivity : ComponentActivity() {
    private val mainVm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EasySpotTheme {
                MainScaffold(mainVm)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        Timber.d("Starting and binding service")

        val serviceIntent = Intent(this, BleService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, mainVm, BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()

        Timber.d("Unbinding service")

        unbindService(mainVm)
    }
}
