package xyz.ggorg.easyspot.ui.main

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.ggorg.easyspot.service.BleService
import xyz.ggorg.easyspot.service.ServiceState

class MainViewModel :
    ViewModel(),
    ServiceConnection {
    var binder: BleService.BleServiceBinder? = null
        private set

    private var serviceJob: Job? = null

    private val _serviceConnectionState = MutableStateFlow(false)
    val serviceConnectionState = _serviceConnectionState.asStateFlow()

    private val _serviceState = MutableStateFlow(ServiceState())
    val serviceState = _serviceState.asStateFlow()

    override fun onServiceConnected(
        name: ComponentName?,
        service: IBinder?,
    ) {
        Timber.d("Service $name connected")
        binder = service as? BleService.BleServiceBinder ?: return

        serviceJob =
            viewModelScope.launch {
                _serviceState.emitAll(binder?.serviceState ?: return@launch)
            }

        _serviceConnectionState.update { true }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Timber.d("Service $name disconnected")

        _serviceConnectionState.update { false }
        serviceJob?.cancel()
        binder = null
    }
}
