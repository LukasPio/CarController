package com.example.test

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var hc05MacAddress: String
    private val hc05UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                showAlert("Bluetooth permission granted")
            } else {
                showAlert("Bluetooth permission denied")
            }
        }

    @RequiresApi(Build.VERSION_CODES.S)
    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (bluetoothAdapter.isEnabled) {
                showAlert("Bluetooth enabled")
                connectToHc05()
            } else {
                showAlert("Bluetooth enabling failed")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            showAlert("Bluetooth is not supported on this device")
            return
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothSocket?.close()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun onTurnOnBluetoothClick(view: android.view.View) {
        if (bluetoothAdapter.isEnabled) {
            connectToHc05()
        } else {
            requestBluetoothPermissions()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestBluetoothPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            enableBluetooth()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(intent)
        } else {
            connectToHc05()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun connectToHc05() {
        val devices = bluetoothAdapter.bondedDevices
        for (device in devices) {
            if (device.name.lowercase() == "hc-05") {
                hc05MacAddress = device.address
                break
            }
        }

        if (::hc05MacAddress.isInitialized) {
            val hc05 = bluetoothAdapter.getRemoteDevice(hc05MacAddress)

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }

            try {
                bluetoothSocket = hc05.createRfcommSocketToServiceRecord(hc05UUID)
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                showAlert("Connected to HC-05")
                bluetoothStatusOn()
            } catch (e: IOException) {
                showAlert("Occurred an error, try again later")
                bluetoothStatusOff()
            }

        } else {
            showAlert("HC-05 not found")
        }
    }

    fun sendBluetoothSignal(view: android.view.View) {
        val signal = "O"
        if (outputStream != null) {
            try {
                outputStream!!.write(signal.toByteArray())
                showAlert("Signal was sent")
            } catch (e: Exception) {
                showAlert("Failed to send signal: ${e.message}")
            }
        } else {
            showAlert("You must connect to HC-05 before sending signals")
            bluetoothStatusOff()
        }
    }

    private fun showAlert(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun bluetoothStatusOn() {
        val bluetoothStatus = findViewById<TextView>(R.id.bluetoothStatus)
        val statusColor = ContextCompat.getColor(this, R.color.green)
        bluetoothStatus.setTextColor(statusColor)
        bluetoothStatus.text = getString(R.string.bluetooth_status_on)
    }

    private fun bluetoothStatusOff() {
        val bluetoothStatus = findViewById<TextView>(R.id.bluetoothStatus)
        val statusColor = ContextCompat.getColor(this, R.color.red)
        bluetoothStatus.setTextColor(statusColor)
        bluetoothStatus.text = getString(R.string.bluetooth_status_off)
    }
}
