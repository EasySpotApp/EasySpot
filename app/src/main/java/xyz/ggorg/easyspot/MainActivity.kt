package xyz.ggorg.easyspot

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import xyz.ggorg.easyspot.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager

    companion object {
        val essentialPermissions = arrayOf(
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
        )

        val nonEssentialPermissions = arrayOf(
            Manifest.permission.POST_NOTIFICATIONS,
        )

        val permissionsToRequest = essentialPermissions + nonEssentialPermissions
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionsGranted = permissions.entries.all { it.value }
            if (allPermissionsGranted) {
                @SuppressLint("MissingPermission")
                enableBluetooth()
            } else {
                Toast.makeText(
                    this,
                    "The app requires all permissions to function correctly.",
                    Toast.LENGTH_LONG
                ).show()

                println("Not all permissions granted: $permissions")

                val intent =
                    Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.fromParts("package", packageName, null)
                    )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }

    private fun checkAndRequestPermissions() {
        val permissionsNotGranted = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        permissionsNotGranted.forEach {
            println("Permission not granted: $it")
        }

        if (permissionsNotGranted.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsNotGranted.toTypedArray())
        } else {
            @SuppressLint("MissingPermission")
            enableBluetooth()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        checkBluetoothSupport()

        binding.startButton.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.stopButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val intent = Intent(this, HotspotService::class.java)
                stopService(intent)
            }
        }
    }

    private fun checkBluetoothSupport() {
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't support Bluetooth.", Toast.LENGTH_LONG).show()
            finish()
        }

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Device doesn't support Bluetooth LE.", Toast.LENGTH_LONG)
                .show()
            finish()
        }

        if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(
                this,
                "Device doesn't support Bluetooth LE Advertising.",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun enableBluetooth() {
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter.isEnabled) {
            val intent = Intent(this, HotspotService::class.java)
            startService(intent)
        } else {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }
}
