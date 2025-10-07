package xyz.ggorg.easyspot.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.ggorg.easyspot.ui.theme.EasySpotTheme

@Composable
fun EasySpotPreview(content: @Composable () -> Unit) {
    EasySpotTheme {
        Box(Modifier.fillMaxSize()) {
            content()
        }
    }
}