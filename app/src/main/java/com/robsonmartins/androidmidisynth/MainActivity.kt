package com.robsonsmartins.androidmidisynth

import com.robsonmartins.androidmidisynth.TelaMidiSintetizador
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import android.media.midi.MidiDeviceInfo

class MainViewModel : ViewModel() {
    var volume by mutableFloatStateOf(0.8f)
    var usoCpu by mutableIntStateOf(0)
    var listaDispositivos by mutableStateOf<List<MidiDeviceInfo>>(emptyList())
}

class MainActivity : ComponentActivity() {

    companion object {
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

        // Pedido de permissão em tempo real obrigatório do Android moderno
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ), 101
            )
        }

        // Inicializa o Sintetizador
        synthManager = SynthManager(this)
        synthManager.loadSF("AcordeonGiulietti.sf2") // Garanta que o nome bate com seu arquivo
        synthManager.setVolume((viewModel.volume * 127).toInt())

        // Inicializa o gerenciador de MIDI
        midiManager = MidiManager(this, ::onMidiMessageReceived)
        midiManager.start()

        // Carrega os dispositivos iniciais na lista da interface
        viewModel.listaDispositivos = midiManager.listarDispositivosDisponiveis(this)

        setContent {
            TelaMidiSintetizador(
                viewModel = viewModel,
                onVolumeChanged = { novoVolume ->
                    viewModel.volume = novoVolume
                    synthManager.setVolume((novoVolume * 127).toInt())
                },
                onDispositivoSelecionado = { dispositivoEscolhido ->
                    midiManager.conectarAoDispositivo(dispositivoEscolhido)
                }
            )
        }
    } // <-- CHAVE CORRETA QUE FECHA O ONCREATE

    /** @brief Callback acionado em tempo real quando mensagens chegam do ESP32-S3 / Cordovox */
    private fun onMidiMessageReceived(message: String) {
        runOnUiThread {
            if (message.startsWith("F0") || message.contains("F0")) {
                processarSysExCpu(message)
            }
        }
    } // <-- CHAVE CORRETA QUE FECHA O ONMIDIMESSAGERECEIVED

    /** @brief Função que extrai o byte de CPU vindo do envelope SysEx */
    private fun processarSysExCpu(message: String) {
        try {
            val bytesText = message.split(" ")
            if (bytesText.size >= 5 && bytesText[1].equals("7D", ignoreCase = true)) {
                val tipoDado = bytesText[3]
                if (tipoDado == "01" || tipoDado == "1" || tipoDado == "01 ") {
                    val valorCPU = bytesText[4].toInt(16)
                    viewModel.usoCpu = valorCPU
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    } // <-- CHAVE CORRETA QUE FECHA O PROCESSARSYSEXCPU

    override fun onDestroy() {
        midiManager.finalize()
        synthManager.finalize()
        super.onDestroy()
    }
}
