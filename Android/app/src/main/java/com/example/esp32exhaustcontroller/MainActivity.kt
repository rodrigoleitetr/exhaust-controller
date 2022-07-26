package com.example.esp32exhaustcontroller

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.Charset
import java.util.*

class MainActivity : AppCompatActivity() {

    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.i("bluetooth_controller", "Bluetooth Allowed.")
        }else{
            Log.i("bluetooth_controller","Bluetooth Denied.")
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("bluetooth_controller", "${it.key} = ${it.value}")
            }
        }

    private lateinit var bluetoothStream : OutputStream

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
        }
        else{
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }

        val btnUrban = findViewById<Button>(R.id.main_bt_urbano)
        val btnPista = findViewById<Button>(R.id.main_bt_pista)
        val btnSport = findViewById<Button>(R.id.main_bt_sport)
        val seekbarLevel = findViewById<SeekBar>(R.id.main_seekbar)

        btnUrban.setOnClickListener {
            //writeToDevice("urban_mode\n");
            seekbarLevel.progress = 0
        }

        btnPista.setOnClickListener {
            seekbarLevel.progress = 3
        }

        btnSport.setOnClickListener {
            seekbarLevel.progress = 6
        }

        seekbarLevel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                writeToDevice("move_$progress\n");
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) { }
            override fun onStopTrackingTouch(seekBar: SeekBar) { }
        })
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice()
    {
        var maxRetries = 3
        var attempts = 0
        while(attempts < maxRetries)
        {
            var adapter = BluetoothAdapter.getDefaultAdapter();
            var pairedDevices = adapter.getBondedDevices()
            var uuid = UUID.fromString("00001101-0000-9000-8000-00805F9B34FB")
            if (pairedDevices.size > 0) {
                for (device in pairedDevices) {
                    var s = device.name
                    if (device.getName().equals("Exhaust_Controller", ignoreCase = true)) {
                        var socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                        var clazz = socket.remoteDevice.javaClass
                        var paramTypes = arrayOf<Class<*>>(Integer.TYPE)
                        var m = clazz.getMethod("createRfcommSocket", *paramTypes)
                        var fallbackSocket = m.invoke(socket.remoteDevice, Integer.valueOf(1)) as BluetoothSocket
                        try {
                            if(!fallbackSocket.isConnected)
                            {
                                fallbackSocket.connect()
                                Log.i("bluetooth_controller", "connected")
                            }
                            bluetoothStream = fallbackSocket.outputStream
                        } catch (e: Exception) {
                            Log.e("bluetooth_controller", e.toString())
                            e.printStackTrace()
                        }
                    }
                }
            }
            attempts++
        }
    }

    fun writeToDevice(text: String){
        var maxRetries = 2
        var attempts = 0
        while(attempts < maxRetries){
            try {
                bluetoothStream.write(text.toByteArray(Charset.forName("UTF-8")))
                attempts = maxRetries
            } catch (e: Exception){
                Log.e("bluetooth_controller", e.toString())
                e.printStackTrace()
                // Tries to connect again as this is the possible reason why
                // is not being able to write
                connectToDevice()
            }
            attempts++
        }
    }

    @SuppressLint("MissingPermission")
    fun connectAndWrite(text: String) {
        var adapter = BluetoothAdapter.getDefaultAdapter();
        var pairedDevices = adapter.getBondedDevices()
        var uuid = UUID.fromString("00001101-0000-9000-8000-00805F9B34FB")
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                var s = device.name
                if (device.getName().equals("Exhaust_Controller", ignoreCase = true)) {
                    Thread {
                        var socket = device.createInsecureRfcommSocketToServiceRecord(uuid)
                        var clazz = socket.remoteDevice.javaClass
                        var paramTypes = arrayOf<Class<*>>(Integer.TYPE)
                        var m = clazz.getMethod("createRfcommSocket", *paramTypes)
                        var fallbackSocket = m.invoke(socket.remoteDevice, Integer.valueOf(1)) as BluetoothSocket
                        try {
                            if(!fallbackSocket.isConnected)
                            {
                                fallbackSocket.connect()
                                Log.i("bluetooth_controller", "connected")
                            }
                            var stream = fallbackSocket.outputStream
                            Log.i("bluetooth_controller", text)
                            stream.write(text.toByteArray(Charset.forName("UTF-8")))
                        } catch (e: Exception) {
                            Log.e("bluetooth_controller", e.toString())
                            e.printStackTrace()
                        } finally {
                            fallbackSocket.close()
                            socket.close()
                        }
                    }.start()
                }
            }
        }
    }
}