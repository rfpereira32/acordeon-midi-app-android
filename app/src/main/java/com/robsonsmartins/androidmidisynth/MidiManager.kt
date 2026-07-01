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
import android.bluetooth.BluetoothManager
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

private var inputPort: MidiInputPort? = null

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
                    0xF0.toByte(),
                    0x7D.toByte(),
                    0x0A.toByte(),
                    0xF7.toByte()
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
                    onMidiMessageReceived("Desconectado: ${nomeDispositivo(deviceInfo)}")
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
                    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

                    // Varre a lista de conexões GATT ativas no barramento do smartphone
                    val dispositivosGattConectados = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
                    var targetDevice: BluetoothDevice? = null

                    // 1. Primeiro tenta achar o dispositivo pelo nome de anúncio público (Infalível em BLE)
                    for (btDevice in dispositivosGattConectados) {
                        val nomeDispositivoBt = try { btDevice.name } catch(_: SecurityException) { "" }
                        if (nomeDispositivoBt?.contains("Cordovox", ignoreCase = true) == true ||
                            nomeDispositivoBt?.contains("ACORDEON", ignoreCase = true) == true ||
                            nomeDispositivoBt?.contains("MIDI", ignoreCase = true) == true) {
                            targetDevice = btDevice
                            Log.d("BATERIA_GATT", "Dispositivo alvo localizado pelo nome: $nomeDispositivoBt [${btDevice.address}]")
                            break
                        }
                    }

                    // 2. Se falhar pelo nome, tenta resgatar por Fallback usando a propriedade MIDI limpa
                    if (targetDevice == null && bluetoothAdapter != null) {
                        val fallbackMac = deviceInfo.properties.getParcelable<BluetoothDevice>(MidiDeviceInfo.PROPERTY_BLUETOOTH_DEVICE)?.address
                            ?: deviceInfo.properties.getString("bluetooth_device") ?: ""
                        if (fallbackMac.isNotEmpty()) {
                            targetDevice = bluetoothAdapter!!.getRemoteDevice(fallbackMac)
                            Log.d("BATERIA_GATT", "Dispositivo alvo localizado via Fallback MAC: $fallbackMac")
                        }
                    }

                    // 3. Conecta a escuta paralela de energia se o alvo foi mapeado no hardware
                    if (targetDevice != null) {
                        targetDevice.connectGatt(context, false, object : BluetoothGattCallback() {

                            private fun atualizarInterfaceComValor(valoresBytes: ByteArray, characteristic: BluetoothGattCharacteristic) {
                                if (characteristic.uuid.toString().contains("2a19") && valoresBytes.isNotEmpty()) {
                                    val nivelCargaBateria = valoresBytes[0].toInt() and 0xFF
                                    mainHandler.post {
                                        MidiEstadoCompartilhado.porcentagemBateriaReal = "$nivelCargaBateria%"
                                    }
                                    Log.d("BATERIA_GATT", "🔋 Telemetria injetada no layout: $nivelCargaBateria%")
                                }
                            }

                            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                                if (newState == BluetoothProfile.STATE_CONNECTED) {
                                    gatt?.discoverServices()
                                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                    try { gatt?.close() } catch(_: Exception) {}
                                }
                            }

                            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                                if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                                    val servicoBateria = gatt.getService(java.util.UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb"))
                                    val caracteristicaBateria = servicoBateria?.getCharacteristic(java.util.UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb"))

                                    if (caracteristicaBateria != null) {
                                        Log.d("BATERIA_GATT", "Serviço de bateria localizado. Inscrevendo notificações...")

                                        // 1. Ativa a escuta de notificações na camada de software do Android
                                        gatt.setCharacteristicNotification(caracteristicaBateria, true)

                                        // 2. CRÍTICO: Dá um respiro de 150 milissegundos para o rádio processar a troca de estado
                                        // antes de gravar o descritor físico 0x2902 no chip do ESP32-S3
                                        mainHandler.postDelayed({
                                            try {
                                                val descriptor = caracteristicaBateria.getDescriptor(java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                                                if (descriptor != null) {
                                                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                                    val sucessoEscrita = gatt.writeDescriptor(descriptor)
                                                    Log.d("BATERIA_GATT", "✍️ Escrita do descritor 0x2902 enviada? $sucessoEscrita")
                                                }
                                            } catch (e: Exception) {
                                                Log.e("BATERIA_GATT", "Falha ao assinar descritor: ${e.message}")
                                            }
                                        }, 150)

                                        // 3. Dá outro respiro de 300 milissegundos para executar a primeira leitura síncrona,
                                        // garantindo que uma operação não atropele a outra no barramento GATT
                                        mainHandler.postDelayed({
                                            try {
                                                gatt.readCharacteristic(caracteristicaBateria)
                                            } catch (_: Exception) {}
                                        }, 450)
                                    }
                                }
                            }


                            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    atualizarInterfaceComValor(value, characteristic)
                                }
                            }

                            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                                atualizarInterfaceComValor(value, characteristic)
                            }
                        }, BluetoothDevice.TRANSPORT_LE)
                    } else {
                        Log.e("BATERIA_GATT", "Impossível acoplar: O dispositivo não foi listado nas conexões ativas do Android.")
                    }
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
        onMidiMessageReceived(String(message).trim())
    }

    private external fun startReadingMidi(receiveDevice: MidiDevice, portNumber: Int)
    private external fun stopReadingMidi()
}
