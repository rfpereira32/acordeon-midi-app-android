/*
 * Copyright (c) 2024 Robson Martins
 * (Modificado para seleção dinâmica de Acordeon)
*/

package com.robsonsmartins.androidmidisynth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
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
import com.robsonsmartins.androidmidisynth.protocol.ConfigPacketParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.robsonsmartins.androidmidisynth.protocol.ConfigPacketBuilder

private var inputPort: MidiInputPort? = null
private var configCharacteristic: BluetoothGattCharacteristic? = null
class MidiManager(
    private val context: Context,
    private val onMidiMessageReceived: (String) -> Unit
) {
    companion object {
        private var configGatt: BluetoothGatt? = null
        private const val TAG = "MIDI_C"
        private val BLE_MIDI_SERVICE_UUID = ParcelUuid.fromString("03B80E5A-EDE8-4B33-A751-6CE34EC4C700")
        private val CONFIG_CHARACTERISTIC_UUID = java.util.UUID.fromString("8c2f4c10-45f1-4b6b-9b1a-1d6d7e5f1001")
    }

    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as AndroidMidiManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private var dispositivoAberto: MidiDevice? = null
    private var bluetoothDeviceAtual: BluetoothDevice? = null
    private var scanningBleMidi = false
    private val bluetoothDevicesEmAbertura = mutableSetOf<String>()
    private val configPacketParser = ConfigPacketParser(mainHandler)

    private val configPacketBuilder = ConfigPacketBuilder()

    private val _nomeDispositivoConectado = MutableStateFlow("Nenhum dispositivo pareado")
    val nomeDispositivoConectado: StateFlow<String> = _nomeDispositivoConectado.asStateFlow()

    private val _isBleConectado = MutableStateFlow(false)
    val isBleConectado: StateFlow<Boolean> = _isBleConectado.asStateFlow()

    fun obterReceiverMidi(): MidiReceiver? {
        return inputPort
    }

    fun enviarComandoIniciarOtaWifi() {
        val rádio = inputPort
        if (rádio != null) {
            try {
                val envelopeOtaSysEx = byteArrayOf(
                    SysExProtocol.START.toByte(),
                    SysExProtocol.MANUFACTURER.toByte(),
                    SysExProtocol.CMD_OTA.toByte(),
                    SysExProtocol.END.toByte()
                )
                rádio.send(envelopeOtaSysEx, 0, envelopeOtaSysEx.size)
                onMidiMessageReceived("Sinal OTA via Wi-Fi AP injetado no fluxo BLE.")
            } catch (e: Exception) {
                Log.e(TAG, "Falha critica ao descarregar buffer SysEx 0x0A: ${e.message}")
            }
        } else {
            onMidiMessageReceived("Erro: Acordeon desconectado. Impossivel carregar comando.")
        }
    }

    fun despacharComandoMixerSysEx(canal: Int, volume: Int) {
        val rádio = inputPort
        if (rádio != null) {
            try {
                val envelopeSysEx = byteArrayOf(
                    SysExProtocol.START.toByte(),
                    SysExProtocol.MANUFACTURER.toByte(),
                    SysExProtocol.CMD_MIXER_VOLUME.toByte(),
                    (canal and SysExProtocol.DATA_MASK).toByte(),
                    (volume and SysExProtocol.DATA_MASK).toByte(),
                    SysExProtocol.END.toByte()
                )

                rádio.send(envelopeSysEx, 0, envelopeSysEx.size)

            } catch (e: Exception) {
                Log.e(TAG, "Falha ao escoar fader SysEx no rádio: ${e.message}")
            }
        }
    }

    fun despacharComandoInstrumentoSysEx(canal: Int, instrumento: Int) {
        val rádio = inputPort
        if (rádio != null) {
            try {
                val envelopeSysEx = byteArrayOf(
                    SysExProtocol.START.toByte(),
                    SysExProtocol.MANUFACTURER.toByte(),
                    SysExProtocol.CMD_INSTRUMENTO.toByte(),
                    (canal and SysExProtocol.DATA_MASK).toByte(),
                    (instrumento and SysExProtocol.DATA_MASK).toByte(),
                    SysExProtocol.END.toByte()
                )

                rádio.send(envelopeSysEx, 0, envelopeSysEx.size)

            } catch (e: Exception) {
                Log.e(TAG, "Falha ao enviar instrumento SysEx: ${e.message}")
            }
        }
    }

    fun start() {
        midiManager.registerDeviceCallback(
            object : AndroidMidiManager.DeviceCallback() {
                override fun onDeviceAdded(deviceInfo: MidiDeviceInfo) {
                    val nome = nomeDispositivo(deviceInfo)
                    onMidiMessageReceived("Dispositivo MIDI detectado: $nome")
                    if (ehDispositivoBleMidi(deviceInfo)) {
                        conectarAoDispositivo(deviceInfo)
                    }
                }
                override fun onDeviceRemoved(deviceInfo: MidiDeviceInfo) {

                    onMidiMessageReceived(
                        "Desconectado: ${nomeDispositivo(deviceInfo)}"
                    )

                    tratarDesconexao()

                }
            }, mainHandler
        )
        iniciarBuscaBleMidi()
    }

    fun listarDispositivosDisponiveis(contextoEfetivo: Context): List<MidiDeviceInfo> {
        val manager = contextoEfetivo.getSystemService(Context.MIDI_SERVICE) as AndroidMidiManager
        return manager.devices.filter { deviceInfo ->
            deviceInfo.properties.getString("product")?.lowercase() != "fluidsynth" &&
                    (deviceInfo.inputPortCount > 0 || deviceInfo.outputPortCount > 0)
        }
    }

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
        bluetoothDeviceAtual = device
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

                val info = dispositivo.info

                if (info == null) {

                    Log.e(TAG, "MidiDevice.info ainda não disponível.")

                    bluetoothDevicesEmAbertura.remove(endereco)

                    iniciarBuscaBleMidi()

                    return@postDelayed
                }

                configurarDispositivoAberto(
                    dispositivo,
                    info
                )

            }, 300)
        }, mainHandler)
    }

    fun iniciarBuscaBleMidi() {
        if (scanningBleMidi) return
        if (!temPermissaoBluetooth()) {
            onMidiMessageReceived("Permissão Bluetooth pendente para buscar BLE MIDI.")
            return
        }
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner == null) {
            onMidiMessageReceived("Bluetooth LE indisponível.")
            return
        }
        val filtros = listOf(ScanFilter.Builder().setServiceUuid(BLE_MIDI_SERVICE_UUID).build())
        val configuracao = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(filtros, configuracao, bleMidiScanCallback)
        scanningBleMidi = true
    }

    fun pararBuscaBleMidi() {
        if (!scanningBleMidi || !temPermissaoBluetooth()) return
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(bleMidiScanCallback)
        scanningBleMidi = false
    }

    private val bleMidiScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) { abrirDispositivoBluetoothMidi(result.device) }
        override fun onBatchScanResults(results: MutableList<ScanResult>) { results.forEach { abrirDispositivoBluetoothMidi(it.device) } }
        override fun onScanFailed(errorCode: Int) { scanningBleMidi = false }
    }
    private fun configurarDispositivoAberto(dispositivo: MidiDevice, deviceInfo: MidiDeviceInfo) {
        try {
            inputPort?.close()
            dispositivoAberto?.close()
        } catch (_: Exception) {}

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
                    portaAbertaComSucesso = true
                    break
                }
            } catch (_: Exception) {}
        }

        mainHandler.post {
            if (portaAbertaComSucesso) {
                MidiEstadoCompartilhado.atualizarEstado(nomeDoAparato, true)
                _nomeDispositivoConectado.value = nomeDoAparato
                _isBleConectado.value = true
                onMidiMessageReceived("Conectado com sucesso a $nomeDoAparato!")

                // ==============================================================================
                // VARREDURA GATT POR DISPOSITIVO CONECTADO RE REAL-TIME (CORREÇÃO DE MAC NULO)
                // ==============================================================================
                try {

                    val targetDevice = bluetoothDeviceAtual

                    if (targetDevice == null) {
                        Log.e(TAG, "BluetoothDevice não disponível.")
                        return@post
                    }

                    try {
                        configGatt?.close()
                    } catch (_: Exception) {
                    }

                    configGatt = null

                    configGatt = targetDevice.connectGatt(
                        context,
                        false,
                        object : BluetoothGattCallback() {
                            override fun onDescriptorWrite(
                                gatt: BluetoothGatt,
                                descriptor: BluetoothGattDescriptor,
                                status: Int
                            ) {
                                Log.d(
                                    TAG,
                                    "Descriptor escrito: ${descriptor.characteristic.uuid} status=$status"
                                )

                                if (
                                    status == BluetoothGatt.GATT_SUCCESS &&
                                    descriptor.characteristic.uuid == CONFIG_CHARACTERISTIC_UUID
                                ) {

                                    Log.d(TAG, "Solicitando sincronização ao ESP...")

                                    enviarPacoteConfiguracao(
                                        configPacketBuilder.criarPacoteRequestSync()
                                    )
                                }
                            }

                            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                                if (newState == BluetoothProfile.STATE_CONNECTED) {
                                    gatt?.discoverServices()
                                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                                    try {
                                        gatt?.close()
                                    } catch (_: Exception) {
                                    }

                                    mainHandler.post {

                                        tratarDesconexao()

                                    }

                                }
                            }

                            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                                gatt?.services?.forEach { service ->

                                    Log.d(TAG, "SERVICE ${service.uuid}")

                                    service.characteristics.forEach { characteristic ->

                                        Log.d(
                                            TAG,
                                            "   CHARACTERISTIC ${characteristic.uuid}"
                                        )
                                    }
                                }
                                if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                                   val servicoConfig =
                                        gatt.getService(
                                            java.util.UUID.fromString("8c2f4c00-45f1-4b6b-9b1a-1d6d7e5f1001")
                                        )

                                    configCharacteristic =
                                        servicoConfig?.getCharacteristic(CONFIG_CHARACTERISTIC_UUID)

                                    if (configCharacteristic != null)
                                    {
                                        Log.d(TAG, "✅ Config Characteristic encontrada.")

                                        gatt.setCharacteristicNotification(configCharacteristic, true)

                                        mainHandler.postDelayed({

                                            try {

                                                val descriptor = configCharacteristic!!.getDescriptor(
                                                    java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                                                )

                                                if (descriptor != null)
                                                {
                                                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                                    val ok = gatt.writeDescriptor(descriptor)

                                                    Log.d(TAG, "Config descriptor enviado: $ok")
                                                }

                                            } catch (e: Exception) {

                                                Log.e(TAG, "Erro habilitando notify Config: ${e.message}")

                                            }

                                        }, 200)
                                    }
                                    else
                                    {
                                        Log.e(TAG, "❌ Config Characteristic NÃO encontrada.")
                                    }

                                }
                            }


                            override fun onCharacteristicChanged(
                                gatt: BluetoothGatt,
                                characteristic: BluetoothGattCharacteristic,
                                value: ByteArray
                            ) {
//                                Log.d(TAG, "Gatt hash = ${gatt.hashCode()}")
                                Log.d(
                                    TAG,
                                    "RX ${characteristic.uuid} -> ${
                                        value.joinToString(" ") { "%02X".format(it) }
                                    }"
                                )
                                Log.d(TAG, "Characteristic: ${characteristic.uuid}")
                                if (characteristic.uuid == CONFIG_CHARACTERISTIC_UUID) {
                                    Log.d(TAG, "Pacote de configuração recebido")
//                                    interpretarPacoteConfiguracao(value)
                                    Log.d(TAG, "UUID = ${characteristic.uuid}")
                                    Log.d(TAG, "value = ${value.joinToString(" ") { "%02X".format(it) }}")

                                    Log.d(
                                        TAG,
                                        "characteristic.value = ${
                                            characteristic.value.joinToString(" ") { "%02X".format(it) }
                                        }"
                                    )
                                    configPacketParser.interpretar(value)
                                    return
                                }

                            }
                        }, BluetoothDevice.TRANSPORT_LE)
                     } catch (e: Exception) {
                    Log.e("BATERIA_GATT", "Falha de barramento por injeção direta: ${e.message}")
                }
            } else {
                MidiEstadoCompartilhado.atualizarEstado("Nenhum dispositivo pareado", false)
                _nomeDispositivoConectado.value = "Nenhum dispositivo pareado"
                _isBleConectado.value = false
            }
        }

        if (deviceInfo.outputPortCount > 0) {
            startReadingMidi(dispositivo, 0)
        }
    }

    fun enviarPacoteConfiguracao(
        pacote: ByteArray
    ): Boolean {

        val characteristic = configCharacteristic
        val gatt = configGatt

        if (characteristic == null || gatt == null) {
            Log.w(TAG, "Não foi possível enviar pacote: BLE desconectado.")
            return false
        }

        characteristic.value = pacote

        Log.d(TAG,"TX ${pacote.joinToString(" ") {"%02X".format(it)}}")

        return gatt.writeCharacteristic(characteristic)
    }

    private fun tratarDesconexao() {
        try {
            configGatt?.close()
        } catch (_: Exception) {
        }

        configGatt = null
        bluetoothDeviceAtual = null
        if (temPermissaoBluetooth()) {
            iniciarBuscaBleMidi()
        }

        stopReadingMidi()

        try {
            inputPort?.close()
            dispositivoAberto?.close()
        } catch (_: Exception) {
        }

        inputPort = null
        dispositivoAberto = null

        MidiEstadoCompartilhado.receiverMidiAtivo = null

        MidiEstadoCompartilhado.atualizarEstado(
            "Nenhum dispositivo pareado",
            false
        )

        _nomeDispositivoConectado.value = "Nenhum dispositivo pareado"
        _isBleConectado.value = false

        bluetoothDevicesEmAbertura.clear()

        iniciarBuscaBleMidi()

        onMidiMessageReceived("Aguardando reconexão do Cordovox...")

        Log.d(TAG, "Desconexão tratada com sucesso.")

    }

    fun finalize() {
        try {
            configGatt?.close()
        } catch (_: Exception) {
        }

        configGatt = null
        bluetoothDeviceAtual = null
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
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceInfo.type == MidiDeviceInfo.TYPE_BLUETOOTH
    }

    private fun nomeDispositivo(deviceInfo: MidiDeviceInfo): String {
        return deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            ?: deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT) ?: "Dispositivo MIDI"
    }

    private fun temPermissaoBluetooth(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else { context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED }
    }

    @androidx.annotation.Keep
    private fun onNativeMessageReceive(message: ByteArray) {

        /*
         * Mensagem de 1 byte enviada pelo código nativo:
         * representa o canal MIDI que recebeu atividade.
         */
        if (message.size == 1) {

            val canal = message[0].toInt() and 0xFF

            if (canal in 0..15) {

                onMidiMessageReceived(
                    "CHANNEL_ACTIVITY:$canal"
                )

                return

            }

        }

        /*
         * Mantém o tratamento das mensagens de texto
         * já existente.
         */
        onMidiMessageReceived(
            String(message).trim()
        )

    }

    private external fun startReadingMidi(
        receiveDevice: MidiDevice,
        portNumber: Int
    )

    private external fun stopReadingMidi()

    /**
     * Define qual controle do acordeão controla
     * determinado canal MIDI do sintetizador.
     *
     * canal: índice MIDI 0..15
     * controle:
     * 1 = Controle 1
     * 2 = Controle 2
     * 3 = Controle 3
     * 4 = Controles 2 + 3
     */
    fun setControlSource(
        canal: Int,
        controle: Int
    ) {

        if (canal !in 0..15)
            return

        if (controle !in 1..4)
            return

        setNativeControlSource(
            canal,
            controle
        )

    }

    private external fun setNativeControlSource(
        canal: Int,
        controle: Int
    )
}