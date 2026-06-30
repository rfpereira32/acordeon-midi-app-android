package com.robsonsmartins.androidmidisynth

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import android.util.Log
import android.media.midi.MidiDeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    var volume by mutableFloatStateOf(0.8f)
    var usoCpu by mutableIntStateOf(0)
    var listaDispositivos by mutableStateOf<List<MidiDeviceInfo>>(emptyList())

    private val _nomeDispositivoConectado = MutableStateFlow("Nenhum dispositivo pareado")
    val nomeDispositivoConectado: StateFlow<String> = _nomeDispositivoConectado.asStateFlow()

    private val _isBleConectado = MutableStateFlow(false)
    val isBleConectado: StateFlow<Boolean> = _isBleConectado.asStateFlow()
}

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
    private lateinit var synthManager: SynthManager
    private lateinit var midiManager: MidiManager

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

        // Inicializa o motor de áudio Fluidsynth
        synthManager = SynthManager(this)
        synthManager.loadSF("AcordeonGiulietti.sf2")
        synthManager.setVolume((viewModel.volume * 127).toInt())

        // Inicializa o gerenciador MIDI nativo com o pacote corrigido
        midiManager = MidiManager(this) { mensagem: String ->
            Log.d(TAG, "Callback MIDI: $mensagem")
        }

        midiManager.iniciarEscaneamentoAutomatico()
        viewModel.listaDispositivos = midiManager.listarDispositivosDisponiveis(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TelaMidiSintetizador(
                        listaDispositivos = viewModel.listaDispositivos,
                        onVolumeChanged = { novoVolume: Float ->
                            viewModel.volume = novoVolume
                            synthManager.setVolume((novoVolume * 127).toInt())
                        },
                        onDispositivoSelecionado = { dispositivoEscolhido: MidiDeviceInfo ->
                            midiManager.conectarAoDispositivo(dispositivoEscolhido)
                        },
                        midiReceiver = midiManager.obterReceiverMidi(),
                        instanciaMidiManager = midiManager // Passagem estável da ponte de dados do rádio BLE
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

    private fun onMidiMessageReceived(message: String) {
        runOnUiThread {
            if (message.startsWith("F0") || message.contains("F0")) {
                processarSysExCpu(message)
            }
        }
    }

    // ==============================================================================
    // PARSER CORRIGIDO: Indexação estrita dos elementos do lote SysEx (Base 16)
    // ==============================================================================
    private fun processarSysExCpu(message: String) {
        try {
            val bytesText = message.split(" ")

            // Garante que o array tenha o tamanho mínimo para evitar estouro de índice (IndexOutOfBounds)
            if (bytesText.size >= 5) {
                // Compara o byte 1 (ID Experimental) ignorando maiúsculas/minúsculas sem parâmetro nomeado
                if (bytesText[1].trim().equals("7D", true)) {

                    val tipoDado = bytesText[3].trim()
                    if (tipoDado == "01" || tipoDado == "1" || tipoDado == "01 ") {

                        // Extrai e converte estritamente o caractere do índice 4 de hexadecimal (Radix 16) para Int
                        val valorCPU = bytesText[4].trim().toInt(16)
                        viewModel.usoCpu = valorCPU
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
