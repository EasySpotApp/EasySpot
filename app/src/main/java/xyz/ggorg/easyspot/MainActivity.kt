package xyz.ggorg.easyspot

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import xyz.ggorg.easyspot.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothGattServer: android.bluetooth.BluetoothGattServer? = null

    private val permissionsToRequest = mutableListOf(
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.POST_NOTIFICATIONS,
    ).toTypedArray()

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allPermissionsGranted = permissions.entries.all { it.value }
            if (allPermissionsGranted) {
                log(this, "All permissions granted!")

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

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            when (state) {
                BluetoothAdapter.STATE_OFF -> {
                    log(this@MainActivity, "Bluetooth is off")
                    stopAdvertising()
                    stopServer()
                }

                BluetoothAdapter.STATE_TURNING_OFF -> {
                    log(this@MainActivity, "Bluetooth is turning off")
                    stopAdvertising()
                    stopServer()
                }

                BluetoothAdapter.STATE_ON -> {
                    log(this@MainActivity, "Bluetooth is on")
                    startServer()
                    startAdvertising()
                }

                BluetoothAdapter.STATE_TURNING_ON -> {
                    log(this@MainActivity, "Bluetooth is turning on")
                    startServer()
                    startAdvertising()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        checkBluetoothSupport()

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)

        binding.startButton.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.stopButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                stopAdvertising()
                stopServer()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
    }

    private fun checkBluetoothSupport() {
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            log(this, "Device doesn't support Bluetooth.")
            finish()
        }

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            log(this, "Device doesn't support Bluetooth LE.")
            finish()
        }

        if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            log(this, "Device doesn't support Bluetooth LE Advertising.")
            finish()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun enableBluetooth() {
        // TODO: remove debug notifications
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            NotificationChannel(
                "mychannel",
                "Default",
                NotificationManager.IMPORTANCE_HIGH
            )
        )

        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter.isEnabled) {
            startServer()
            startAdvertising()
        } else {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                log(this@MainActivity, "Device connected: ${device?.address}")
            } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                log(this@MainActivity, "Device disconnected: ${device?.address}")
            }
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )

            log(
                this@MainActivity,
                "Characteristic ${characteristic?.uuid} write ${value.toString()} by ${device?.address}"
            )

            with(NotificationManagerCompat.from(this@MainActivity)) {
                @SuppressLint("MissingPermission")
                notify(
                    1, NotificationCompat.Builder(
                        this@MainActivity,
                        "mychannel"
                    )
                        .apply {
                            setSmallIcon(R.drawable.ic_launcher_foreground)
                            setContentTitle("Characteristic write")
                            setContentText("Characteristic ${characteristic?.uuid} write ${value.toString()} by ${device?.address}")
                            setPriority(NotificationCompat.PRIORITY_HIGH)
                            setAutoCancel(true)
                        }.build()
                )
            }

            if (responseNeeded) {
                bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    null
                )
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun startServer() {
        bluetoothGattServer =
            bluetoothManager.openGattServer(applicationContext, gattServerCallback)
        bluetoothGattServer?.addService(HotspotGattService.createHotspotService())

        binding.serverState.text = "Server: on"
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun stopServer() {
        bluetoothGattServer?.close()

        binding.serverState.text = "Server: off"
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            log(this@MainActivity, "Advertising started successfully")

            binding.advertiserState.text = "Advertiser: on"
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            log(this@MainActivity, "Advertising failed with error code: $errorCode")

            binding.advertiserState.text = "Advertiser: err"
        }
    }

    private fun startAdvertising() {
        val bluetoothLeAdvertiser = bluetoothManager.adapter.bluetoothLeAdvertiser

        val settings = AdvertiseSettings.Builder().apply {
            setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            setConnectable(true)
            setTimeout(0)
            setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        }.build()

        val data = AdvertiseData.Builder().apply {
            setIncludeDeviceName(false)
            setIncludeTxPowerLevel(true)
            addServiceUuid(ParcelUuid(HotspotGattService.SERVICE_UUID))
        }.build()

        bluetoothLeAdvertiser.startAdvertising(settings, data, advertiseCallback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun stopAdvertising() {
        val bluetoothLeAdvertiser = bluetoothManager.adapter.bluetoothLeAdvertiser
        bluetoothLeAdvertiser.stopAdvertising(advertiseCallback)

        binding.advertiserState.text = "Advertiser: off"
    }

    private fun log(context: Context, message: String) {
        println(message)
        runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
