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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

    // --- NOVOS ESTADOS REATIVOS PARA A INTERFACE ---
    private val _nomeDispositivoConectado = MutableStateFlow("Nenhum dispositivo pareado")
    val nomeDispositivoConectado: StateFlow<String> = _nomeDispositivoConectado.asStateFlow()

    private val _isBleConectado = MutableStateFlow(false)
    val isBleConectado: StateFlow<Boolean> = _isBleConectado.asStateFlow()

    /**
     * Expõe a porta de envio convertida em MidiReceiver para a MainActivity.
     */
    fun obterReceiverMidi(): MidiReceiver? {
        return inputPort
    }

    // =======================================================================
    // COUT DE ENGENHARIA: RECURSOS ADICIONADOS DE TRANSMISSÃO DIGITAL SYSEX
    // =======================================================================

    /**
     * Dispara o envelope SysEx curto de chaveamento de infraestrutura (Sub-ID 0x0A).
     * Força o microcontrolador ESP32 a derrubar o BLE e invocar o iniciarOTA().
     */
    fun enviarComandoIniciarOtaWifi() {
        val rádio = inputPort
        if (rádio != null) {
            try {
                val envelopeOtaSysEx = byteArrayOf(
                    0xF0.toByte(),
                    0x7D.toByte(),
                    0x0A.toByte(),
                    0xF7.toByte()
                )
                rádio.send(envelopeOtaSysEx, 0, envelopeOtaSysEx.size)
                onMidiMessageReceived("Comando OTA via Wi-Fi AP despachado com sucesso.")
            } catch (e: Exception) {
                Log.e(TAG, "Falha critica ao descarregar buffer SysEx 0x0A: ${e.message}")
            }
        } else {
            onMidiMessageReceived("Erro: Acordeon desconectado. Impossivel enviar comando OTA.")
        }
    }

    /**
     * Monta e descarrega o pacote do Mixer de Volumes (Sub-ID 0x05) de 7 bits em runtime.
     */
    fun despacharComandoMixerSysEx(canal: Int, volume: Int) {
        val rádio = inputPort
        if (rádio != null) {
            try {
                val envelopeSysEx = byteArrayOf(
                    0xF0.toByte(),
                    0x7D.toByte(),
                    0x05.toByte(),
                    (canal and 0x7F).toByte(),
                    (volume and 0x7F).toByte(),
                    0xF7.toByte()
                )
                rádio.send(envelopeSysEx, 0, envelopeSysEx.size)
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao escoar fader SysEx no rádio: ${e.message}")
            }
        }
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

    /** @brief Abre um dispositivo MIDI selecionado manualmente no Pop-up */
    fun conectarAoDispositivo(deviceInfo: MidiDeviceInfo) {
        val nome = nomeDispositivo(deviceInfo)
        onMidiMessageReceived("Conectando manualmente a: $nome...")
        pararBuscaBleMidi()

        midiManager.openDevice(deviceInfo, { dispositivo ->
            if (dispositivo == null) {
                onMidiMessageReceived("Falha ao abrir $nome.")
                return@openDevice
            }
            mainHandler.postDelayed({
                configurarDispositivoAberto(dispositivo, deviceInfo)
            }, 300)
        }, mainHandler)
    }

    private fun abrirDispositivoBluetoothMidi(device: BluetoothDevice) {
        if (!temPermissaoBluetooth()) return
        val endereco = device.address ?: return
        if (!bluetoothDevicesEmAbertura.add(endereco)) return

        val nome = try { device.name ?: endereco } catch (_: SecurityException) { endereco }
        onMidiMessageReceived("BLE MIDI encontrado no ar: $nome. Conectando...")
        pararBuscaBleMidi()

        midiManager.openBluetoothDevice(device, { dispositivo ->
            bluetoothDevicesEmAbertura.remove(endereco)
            if (dispositivo == null) {
                onMidiMessageReceived("Não foi possível abrir BLE MIDI: $nome")
                iniciarBuscaBleMidi()
                return@openBluetoothDevice
            }
            mainHandler.postDelayed({
                configurarDispositivoAberto(dispositivo, dispositivo.info)
            }, 300)
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

    private fun configurarDispositivoAberto(dispositivo: MidiDevice, deviceInfo: MidiDeviceInfo) {
        try {
            inputPort?.close()
            dispositivoAberto?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao limpar conexoes residuais: ${e.message}")
        }

        MidiEstadoCompartilhado.receiverMidiAtivo = null
        inputPort = null
        dispositivoAberto = dispositivo

        val nomeDoAparato = nomeDispositivo(deviceInfo)
        stopReadingMidi()

        var portaAbertaComSucesso = false
        val totalDePortasDeEntrada = if (deviceInfo.inputPortCount > 0) deviceInfo.inputPortCount else 2

        for (indicePorta in 0 until totalDePortasDeEntrada) {
            try {
                val tentaPorta = dispositivo.openInputPort(indicePorta)
                if (tentaPorta != null) {
                    inputPort = tentaPorta
                    MidiEstadoCompartilhado.receiverMidiAtivo = tentaPorta
                    Log.d(TAG, "Bypass Sucesso: Canal de escrita indexado na porta: $indicePorta")
                    portaAbertaComSucesso = true
                    break
                }
            } catch (e: Exception) {
                Log.e(TAG, "Porta $indicePorta indisponivel no barramento.")
            }
        }

        if (!portaAbertaComSucesso) {
            try {
                val portaInjetada = dispositivo.openInputPort(0)
                if (portaInjetada != null) {
                    inputPort = portaInjetada
                    MidiEstadoCompartilhado.receiverMidiAtivo = portaInjetada
                    portaAbertaComSucesso = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha na ponte de injecao.")
            }
        }

        mainHandler.post {
            if (portaAbertaComSucesso) {
                MidiEstadoCompartilhado.atualizarEstado(nomeDoAparato, true)
                _nomeDispositivoConectado.value = nomeDoAparato
                _isBleConectado.value = true

                onMidiMessageReceived("Conectado com sucesso a $nomeDoAparato!")
                onMidiMessageReceived("Canal de envio para o ESP32 aberto.")
            } else {
                MidiEstadoCompartilhado.atualizarEstado("Nenhum dispositivo pareado", false)
                _nomeDispositivoConectado.value = "Nenhum dispositivo pareado"
                _isBleConectado.value = false
                onMidiMessageReceived("Aviso: ESP32 nao liberou canal de escrita.")
            }
        }

        if (deviceInfo.outputPortCount > 0) {
            startReadingMidi(dispositivo, 0)
        }
    }

    fun finalize() {
        mainHandler.post {
            MidiEstadoCompartilhado.atualizarEstado("Nenhum dispositivo pareado", false)
            MidiEstadoCompartilhado.receiverMidiAtivo = null
            _nomeDispositivoConectado.value = "Nenhum dispositivo pareado"
            _isBleConectado.value = false
        }

        pararBuscaBleMidi()
        stopReadingMidi()
        try {
            inputPort?.close()
            dispositivoAberto?.close()
        } catch (_: Exception) {}
        inputPort = null
        dispositivoAberto = null
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
