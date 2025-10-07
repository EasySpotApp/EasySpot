package xyz.ggorg.easyspot.ui.main

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import xyz.ggorg.easyspot.PermissionUtils
import xyz.ggorg.easyspot.service.BleService

@Composable
fun TempUi(mainActivity: MainActivity) {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .padding(4.dp),
        Arrangement.Top,
        Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            mainActivity.checkAndRequestPermissions()
        }) { Text("Start") }

        Button(onClick = {
            if (PermissionUtils.arePermissionsGranted(context)) {
                val intent = Intent(context, BleService::class.java)
                context.stopService(intent)
            }
        }) { Text("Stop") }

        Text("The service state is visible in your notification panel. See the GitHub repo for a client application.")
    }
}

//@Preview
//@Composable
//private fun TempUiPreview() {
//    EasySpotPreview {
//        TempUi()
//    }
//}