package xyz.ggorg.easyspot.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import timber.log.Timber
import xyz.ggorg.easyspot.R
import xyz.ggorg.easyspot.ui.components.settings.SettingsDialogWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    mainVm: MainViewModel,
    modifier: Modifier = Modifier,
) {
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    if (showSettingsDialog) {
        SettingsDialogWrapper(
            onDismiss = { showSettingsDialog = false },
            restartService = {
                Timber.d("Restarting service to apply settings")

                mainVm.binder?.stop()
                mainVm.binder?.updateState()
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                title = {
                    Text(stringResource(R.string.app_name))
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rounded_settings_24),
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        MainUi(
            @Suppress("ktlint:compose:vm-forwarding-check") mainVm,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
