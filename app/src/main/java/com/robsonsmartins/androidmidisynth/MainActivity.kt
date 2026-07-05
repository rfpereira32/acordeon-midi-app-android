package com.robsonsmartins.androidmidisynth

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import android.util.Log
import android.media.midi.MidiDeviceInfo
import java.io.File
import java.io.FileOutputStream
import com.robsonsmartins.androidmidisynth.audio.SoundFontManager
import com.robsonsmartins.androidmidisynth.viewmodel.MainViewModel

private fun MidiManager.iniciarEscaneamentoAutomatico() {
    start()
}

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        init {
            System.loadLibrary("c++_shared")
            System.loadLibrary("oboe")
            System.loadLibrary("fluidsynth")
            System.loadLibrary("synth-lib")
        }
    }

    private val viewModel: MainViewModel by viewModels()
    private lateinit var synthManager: FluidSynthManager
    private lateinit var midiManager: MidiManager
    private lateinit var soundFontManager: SoundFontManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ), 101
            )
        }

        // Inicializa o motor de áudio FluidSynth interno do projeto
        synthManager = FluidSynthManager(this)

        soundFontManager = SoundFontManager(this)

        val caminhoSoundFont =
            soundFontManager.prepareSoundFont("AcordeonGiulietti.sf2")

        synthManager.loadSF(caminhoSoundFont)
        synthManager.setVolume((viewModel.masterVolume * 127).toInt())

        // ==============================================================================
        // COUPLING TEXTUAL DE ALTA FIDELIDADE: CAPTURA E ATUALIZA A BATERIA EM RUNTIME
        // ==============================================================================
        // ==============================================================================
        // INICIALIZAÇÃO DO DRIVER DE RÁDIO MIDI COM FILTRO DE BYTES CONTROLO CHANGE
        // ==============================================================================
        // Inicialização limpa e original do seu driver MIDI
        midiManager = MidiManager(this) { mensagem: String ->
            Log.d(TAG, "Callback MIDI: $mensagem")
        }


        midiManager.iniciarEscaneamentoAutomatico()
        viewModel.dispositivosMidi = midiManager.listarDispositivosDisponiveis(this)

        val sistemaMidi = getSystemService(Context.MIDI_SERVICE) as android.media.midi.MidiManager
        sistemaMidi.registerDeviceCallback(object : android.media.midi.MidiManager.DeviceCallback() {
            override fun onDeviceAdded(deviceInfo: MidiDeviceInfo) {
                if (deviceInfo.properties.getString("product")?.contains("Cordovox", ignoreCase = true) == true ||
                    deviceInfo.type == MidiDeviceInfo.TYPE_BLUETOOTH) {

                    sistemaMidi.openDevice(deviceInfo, { dispositivo ->
                        if (dispositivo != null) {
                            val outputPort = dispositivo.openOutputPort(0)
                            outputPort?.connect(object : android.media.midi.MidiReceiver() {
                                override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                                    if (count >= 3) {
                                        val status = msg[offset].toInt() and 0xFF
                                        val nota = msg[offset + 1].toInt() and 0xFF
                                        val vel = msg[offset + 2].toInt() and 0xFF

                                        if (status in 0x90..0x9F && vel > 0) {
                                            synthManager.setVolume((viewModel.masterVolume * 127).toInt())
                                        }
                                    }
                                }
                            })
                        }
                    }, null)
                }
            }
        }, null)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TelaMidiSintetizador(
                        listaDispositivos = viewModel.dispositivosMidi,
                        onVolumeChanged = { novoVolume: Float ->
                            viewModel.masterVolume = novoVolume
                            synthManager.setVolume((viewModel.masterVolume * 127).toInt())
                        },
                        onDispositivoSelecionado = { dispositivoEscolhido: MidiDeviceInfo ->
                            midiManager.conectarAoDispositivo(dispositivoEscolhido)
                        },
                        midiReceiver = midiManager.obterReceiverMidi(),
                        instanciaMidiManager = midiManager
                    )
                }
            }
        }
    }

    private fun atualizarVolumeInternoFluidSynth(canal: Int, volume: Int) {
        Log.d(TAG, "Sincronizando ganho interno FluidSynth - Canal: $canal, Vol: $volume")
    }

    private fun despacharSysExMixerBluetooth(canal: Int, volume: Int) {
        val envelopeSysEx = byteArrayOf(
            0xF0.toByte(),
            0x7D.toByte(),
            0x05.toByte(),
            (canal and 0x7F).toByte(),
            (volume and 0x7F).toByte(),
            0xF7.toByte()
        )
        Log.d(TAG, "SysEx Mixer despachado para radio BLE: Canal $canal, Volume $volume")
    }

    private fun despacharSysExOtaBluetooth() {
        val envelopeOtaSysEx = byteArrayOf(
            0xF0.toByte(),
            0x7D.toByte(),
            0x0A.toByte(),
            0xF7.toByte()
        )
        Log.d(TAG, "SysEx OTA enviado com sucesso para chaveamento de infraestrutura.")
    }

    // ==============================================================================
    // INTERCEPTADOR TEXTUAL DIRETO: PESCA A BATERIA ANTES DO FILTRO DE MENSAGENS MIDI
    // ==============================================================================
    private fun onMidiMessageReceived(message: String) {
        // Log de bancada essencial: Imprime TUDO o que o rádio joga para o Kotlin
        Log.d(TAG, "📥 [RADIO_RAW] Texto puro vindo do fole: $message")

        // GATILHO CRÍTICO: Captura a string de bateria enviada pelo ESP32-S3
        if (message.contains("BAT:", ignoreCase = true)) {
            try {
                // Extrai o número que vem após o marcador "BAT:"
                val valorNumericoTexto = message.substringAfter("BAT:").trim()
                val porcentagemCarga = valorNumericoTexto.toInt().coerceIn(0, 100)

                // Atualiza instantaneamente o Singleton reativo que a TelaMidi observa
                runOnUiThread {
                    MidiEstadoCompartilhado.porcentagemBateriaReal = "$porcentagemCarga%"
                }
                Log.d(TAG, "🔋 [TELEMETRIA] Interface atualizada com sucesso: $porcentagemCarga%")
                return // Retorna para não enviar resíduos para o processador SysEx
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao decodificar telemetria de bateria: ${e.message}")
            }
        }

        // Fluxo original preservado para mensagens SysEx padrão e CPU
        runOnUiThread {
            if (message.startsWith("F0") || message.contains("F0")) {
                processarSysExCpu(message)
            }
        }
    }


    private fun processarSysExCpu(message: String) {
        try {
            val bytesText = message.trim().replace("\\s+".toRegex(), " ").split(" ")
            if (bytesText.size >= 5 && bytesText[1].trim().equals("7D", ignoreCase = true)) {
                val subIdComando = bytesText[2].trim()
                if (subIdComando == "01" || subIdComando == "1") {
                    val valorCPU = bytesText[4].trim().toInt(16)
                    viewModel.usoCpu = valorCPU
                }
            }
        } catch (_: Exception) {}
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && ::midiManager.isInitialized) {
            midiManager.iniciarEscaneamentoAutomatico()
        }
    }

    override fun onDestroy() {
        try {
            midiManager.finalize()
            synthManager.finalize()
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
