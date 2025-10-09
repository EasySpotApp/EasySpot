package xyz.ggorg.easyspot.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import rikka.shizuku.Shizuku
import xyz.ggorg.easyspot.PermissionUtils
import xyz.ggorg.easyspot.PermissionUtils.permissionsToRequest
import xyz.ggorg.easyspot.service.BleService
import xyz.ggorg.easyspot.shizuku.ShizukuState
import xyz.ggorg.easyspot.shizuku.ShizukuUtils
import xyz.ggorg.easyspot.ui.theme.EasySpotTheme

class MainActivity : ComponentActivity() {
    private lateinit var bluetoothManager: BluetoothManager

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
            val allPermissionsGranted = permissions.entries.all { it.value }
            if (allPermissionsGranted) {
                if (!ShizukuUtils.isPermissionGranted()) {
                    if (Shizuku.shouldShowRequestPermissionRationale()) {
                        Toast.makeText(
                                this,
                                "The app requires all permissions to function correctly.",
                                Toast.LENGTH_LONG,
                            )
                            .show()

                        Log.w(this.toString(), "Shizuku not granted")
                        val intent =
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", packageName, null),
                            )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        finish()
                    } else {
                        ShizukuUtils.requestPermission(0)
                    }
                } else {
                    onAllPermissionsGranted()
                }
            } else {
                Toast.makeText(
                        this,
                        "The app requires all permissions to function correctly.",
                        Toast.LENGTH_LONG,
                    )
                    .show()

                Log.w(this.toString(), "Not all permissions granted: $permissions")

                val intent =
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null),
                    )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }

    fun checkAndRequestPermissions() {
        when (ShizukuUtils.getShizukuState(this)) {
            ShizukuState.NOT_INSTALLED -> {
                Toast.makeText(
                        this,
                        "Shizuku is not installed. Please install it from the Play Store.",
                        Toast.LENGTH_LONG,
                    )
                    .show()
                ShizukuUtils.openPlayStoreListing(this)
                finish()
                return
            }

            ShizukuState.NOT_RUNNING -> {
                Toast.makeText(
                        this,
                        "Shizuku is not running. Please start the Shizuku service.",
                        Toast.LENGTH_LONG,
                    )
                    .show()
                ShizukuUtils.startShizukuActivity(this)
                finish()
                return
            }

            else -> {}
        }

        val permissionsNotGranted =
            permissionsToRequest.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

        Log.w(this.toString(), "Permissions not granted: $permissionsNotGranted")

        if (permissionsNotGranted.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsNotGranted.toTypedArray())
        } else {
            if (!ShizukuUtils.isPermissionGranted()) {
                if (Shizuku.shouldShowRequestPermissionRationale()) {
                    Toast.makeText(
                            this,
                            "The app requires all permissions to function correctly.",
                            Toast.LENGTH_LONG,
                        )
                        .show()

                    Log.w(this.toString(), "Shizuku not granted")
                    val intent =
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", packageName, null),
                        )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                } else {
                    ShizukuUtils.requestPermission(0)
                }
            } else {
                onAllPermissionsGranted()
            }
        }
    }

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode: Int, grantResult: Int ->
            if (
                grantResult == PackageManager.PERMISSION_GRANTED &&
                    PermissionUtils.arePermissionsGranted(this)
            ) {
                onAllPermissionsGranted()
            } else {
                Toast.makeText(
                        this,
                        "The app requires all permissions to function correctly.",
                        Toast.LENGTH_LONG,
                    )
                    .show()

                Log.w(this.toString(), "Shizuku permission not granted")

                val intent =
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null),
                    )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EasySpotTheme { MainScaffold(this) } }

        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        checkBluetoothSupport()

        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }

    override fun onStart() {
        super.onStart()

        checkAndRequestPermissions()
    }

    private fun checkBluetoothSupport() {
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth.", Toast.LENGTH_LONG).show()
            Log.e(this.toString(), "Bluetooth not supported")
            finish()
        }

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Device doesn't support Bluetooth LE.", Toast.LENGTH_LONG).show()
            Log.e(this.toString(), "Bluetooth LE not supported")
            finish()
        }

        if (bluetoothAdapter.isEnabled && !bluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(
                    this,
                    "Device doesn't support Bluetooth LE Advertising.",
                    Toast.LENGTH_LONG,
                )
                .show()
            Log.e(this.toString(), "Bluetooth LE Advertising not supported")
            finish()
        }
    }

    @SuppressLint("MissingPermission")
    private fun onAllPermissionsGranted() {
        enableBluetooth()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun enableBluetooth() {
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter.isEnabled) {
            BleService.tryStartForeground(this)
        } else {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }
}
