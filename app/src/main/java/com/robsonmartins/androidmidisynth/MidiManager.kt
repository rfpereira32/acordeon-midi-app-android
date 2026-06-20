/*
 * Copyright (c) 2024 Robson Martins
 * (Modificado para seleção dinâmica de Acordeon)
*/

package com.robsonsmartins.androidmidisynth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiReceiver
import android.media.midi.MidiManager as AndroidMidiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log

private var inputPort: MidiInputPort? = null

/**
 * @brief MidiManager class.
 * @details Gerencia a varredura, abertura dinâmica do canal de dados MIDI e conexão BLE MIDI nativa.
 */
class MidiManager(
    private val context: Context,
    private val onMidiMessageReceived: (String) -> Unit
) {
    companion object {
        private const val TAG = "MIDI_C"
        private val BLE_MIDI_SERVICE_UUID = ParcelUuid.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700")
    }

    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as AndroidMidiManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private var dispositivoAberto: MidiDevice? = null
    private var scanningBleMidi = false
    private val bluetoothDevicesEmAbertura = mutableSetOf<String>()

    /**
     * Expõe a porta de envio convertida em MidiReceiver para a MainActivity.
     */
    fun obterReceiverMidi(): MidiReceiver? {
        return inputPort
    }

    fun finalize() {
        pararBuscaBleMidi()
        stopReadingMidi()
        inputPort?.close()
        dispositivoAberto?.close()
        inputPort = null
        dispositivoAberto = null
    }

    /** @brief Inicializa callbacks MIDI e a busca BLE MIDI nativa, sem depender de apps externos. */
    fun start() {
        midiManager.registerDeviceCallback(
            object : AndroidMidiManager.DeviceCallback() {
                override fun onDeviceAdded(device: MidiDeviceInfo) {
                    val nome = nomeDispositivo(device)
                    onMidiMessageReceived("Dispositivo MIDI detectado: $nome")
                    if (ehDispositivoBleMidi(device)) {
                        conectarAoDispositivo(device)
                    }
                }

                override fun onDeviceRemoved(device: MidiDeviceInfo) {
                    onMidiMessageReceived("Desconectado: ${nomeDispositivo(device)}")
                }
            }, mainHandler
        )

        iniciarBuscaBleMidi()
    }

    /** @brief Retorna dinamicamente a lista de instrumentos disponíveis para a tela. */
    fun listarDispositivosDisponiveis(contextoEfetivo: Context): List<MidiDeviceInfo> {
        val manager = contextoEfetivo.getSystemService(Context.MIDI_SERVICE) as AndroidMidiManager
        return manager.devices.filter { deviceInfo ->
            deviceInfo.properties.getString("product")?.lowercase() != "fluidsynth" &&
                (deviceInfo.inputPortCount > 0 || deviceInfo.outputPortCount > 0)
        }
    }

    /** @brief Abre um dispositivo MIDI já publicado pelo Android. */
    fun conectarAoDispositivo(deviceInfo: MidiDeviceInfo) {
        onMidiMessageReceived("Conectando a: ${nomeDispositivo(deviceInfo)}...")
        midiManager.openDevice(deviceInfo, { dispositivo ->
            if (dispositivo == null) {
                onMidiMessageReceived("Falha ao abrir ${nomeDispositivo(deviceInfo)}.")
                return@openDevice
            }
            configurarDispositivoAberto(dispositivo, deviceInfo)
        }, mainHandler)
    }

    /** @brief Inicia varredura BLE por periféricos que anunciam o serviço oficial BLE MIDI. */
    fun iniciarBuscaBleMidi() {
        if (scanningBleMidi) return
        if (!temPermissaoBluetooth()) {
            onMidiMessageReceived("Permissão Bluetooth pendente para buscar BLE MIDI.")
            return
        }

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            onMidiMessageReceived("Bluetooth LE indisponível ou desligado.")
            return
        }

        val filtros = listOf(ScanFilter.Builder().setServiceUuid(BLE_MIDI_SERVICE_UUID).build())
        val configuracao = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(filtros, configuracao, bleMidiScanCallback)
        scanningBleMidi = true
        onMidiMessageReceived("Buscando dispositivos BLE MIDI...")
    }

    /** @brief Compatibilidade com chamadas existentes no app para iniciar a conexão BLE MIDI automática. */
    fun iniciarEscaneamentoAutomatico() {
        iniciarBuscaBleMidi()
    }

    fun pararBuscaBleMidi() {
        if (!scanningBleMidi || !temPermissaoBluetooth()) return
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(bleMidiScanCallback)
        scanningBleMidi = false
    }

    private val bleMidiScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            abrirDispositivoBluetoothMidi(result.device)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { abrirDispositivoBluetoothMidi(it.device) }
        }

        override fun onScanFailed(errorCode: Int) {
            scanningBleMidi = false
            onMidiMessageReceived("Falha na busca BLE MIDI: código $errorCode")
        }
    }

    private fun abrirDispositivoBluetoothMidi(device: BluetoothDevice) {
        if (!temPermissaoBluetooth()) return
        val endereco = device.address ?: return
        if (!bluetoothDevicesEmAbertura.add(endereco)) return

        val nome = try { device.name ?: endereco } catch (_: SecurityException) { endereco }
        onMidiMessageReceived("BLE MIDI encontrado: $nome. Abrindo conexão nativa...")

        midiManager.openBluetoothDevice(device, { dispositivo ->
            bluetoothDevicesEmAbertura.remove(endereco)
            if (dispositivo == null) {
                onMidiMessageReceived("Não foi possível abrir BLE MIDI: $nome")
                return@openBluetoothDevice
            }
            pararBuscaBleMidi()
            configurarDispositivoAberto(dispositivo, dispositivo.info)
        }, mainHandler)
    }

    private fun configurarDispositivoAberto(dispositivo: MidiDevice, deviceInfo: MidiDeviceInfo) {
        dispositivoAberto?.close()
        dispositivoAberto = dispositivo
        onMidiMessageReceived("Conectado com sucesso a ${nomeDispositivo(deviceInfo)}!")

        stopReadingMidi()
        inputPort?.close()
        inputPort = null

        try {
            if (deviceInfo.inputPortCount > 0) {
                inputPort = dispositivo.openInputPort(0)
                Log.d(TAG, "Canal de envio para o ESP32 aberto.")
            }
        } catch (e: Exception) {
            onMidiMessageReceived("Aviso: falha ao abrir canal MIDI de envio/OTA.")
            Log.e(TAG, "Erro ao abrir porta de entrada do dispositivo: ${e.message}")
        }

        if (deviceInfo.outputPortCount > 0) {
            onMidiMessageReceived("Abrindo porta MIDI padrão: 0")
            startReadingMidi(dispositivo, 0)
            onMidiMessageReceived("Canal de áudio aberto!")
        } else {
            onMidiMessageReceived("Dispositivo conectado sem porta MIDI de leitura.")
        }
    }

    private fun ehDispositivoBleMidi(deviceInfo: MidiDeviceInfo): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            deviceInfo.type == MidiDeviceInfo.TYPE_BLUETOOTH
    }

    private fun nomeDispositivo(deviceInfo: MidiDeviceInfo): String {
        return deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            ?: deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
            ?: "Dispositivo MIDI"
    }

    private fun temPermissaoBluetooth(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    @androidx.annotation.Keep
    @Suppress("unused")
    private fun onNativeMessageReceive(message: ByteArray) {
        val mensagemTexto = String(message).trim()
        onMidiMessageReceived(mensagemTexto)
    }

    private external fun startReadingMidi(receiveDevice: MidiDevice, portNumber: Int)
    private external fun stopReadingMidi()
}
