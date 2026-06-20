package com.robsonmartins.androidmidisynth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

class BleScannerManager(
    private val context: Context,
    private val onDeviceFound: (BluetoothDevice) -> Unit
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner = bluetoothAdapter?.bluetoothLeScanner

    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { bluetoothDevice ->
                val nome = bluetoothDevice.name ?: ""
                // Captura qualquer um dos seus acordeons baseando-se no nome do sinal de rádio
                if (nome.contains("Acordeon", ignoreCase = true) ||
                    nome.contains("MIDI", ignoreCase = true) ||
                    nome.contains("Cordovox", ignoreCase = true)) {
                    Log.d("BLE_PROV", "Acordeon localizado via rádio: $nome")
                    stopScan()
                    onDeviceFound(bluetoothDevice) // Repassa o dispositivo Bluetooth bruto
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (isScanning || scanner == null) return
        isScanning = true
        Log.d("BLE_PROV", "Buscando instrumentos sem cabos...")

        handler.postDelayed({ stopScan() }, 15000)

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning || scanner == null) return
        isScanning = false
        scanner.stopScan(scanCallback)
    }
}
