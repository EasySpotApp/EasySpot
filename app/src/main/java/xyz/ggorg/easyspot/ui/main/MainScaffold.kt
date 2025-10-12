package xyz.ggorg.easyspot.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import xyz.ggorg.easyspot.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    mainVm: MainViewModel,
    modifier: Modifier = Modifier,
) {
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
