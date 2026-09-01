package com.robsonsmartins.androidmidisynth

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.robsonsmartins.androidmidisynth.audio.SynthController
import com.robsonsmartins.androidmidisynth.session.PresetManager
import com.robsonsmartins.androidmidisynth.session.SessionCoordinator
import com.robsonsmartins.androidmidisynth.session.SessionManager
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontManager
import com.robsonsmartins.androidmidisynth.sync.ConfigurationSynchronizer
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
    private lateinit var configurationSynchronizer: ConfigurationSynchronizer
    private lateinit var soundFontManager: SoundFontManager
    private lateinit var synthController: SynthController
    private lateinit var sessionCoordinator: SessionCoordinator
    private lateinit var presetManager: PresetManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        enableEdgeToEdge()

        if (
            android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.S
        ) {

            requestPermissions(
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ),
                101
            )

        }

        // Inicializa o motor de áudio FluidSynth interno do projeto
        synthManager =
            FluidSynthManager(this)

        synthController =
            SynthController(
                synthManager
            )

        soundFontManager =
            SoundFontManager(
                this,
                synthController
            )

        viewModel.setSynthController(
            synthController
        )

        viewModel.setSoundFontManager(
            soundFontManager
        )

        // =============================================================================
        // PRESETS
        // =============================================================================

        presetManager =
            PresetManager(this)

        viewModel.setPresetManager(
            presetManager
        )

        // =============================================================================
        // SESSÃO
        // =============================================================================

        sessionCoordinator =
            SessionCoordinator(

                SessionManager(this),

                soundFontManager,

                viewModel.mixerState,

                restaurarCanal = {
                        canal,
                        soundFont,
                        preset ->

                    viewModel.setChannelInstrument(
                        canal,
                        soundFont,
                        preset
                    )

                },

                restaurarVolume = {
                        canal,
                        volume,
                        mute ->

                    viewModel.setChannelVolume(
                        canal,
                        volume
                    )

                    viewModel.setChannelMute(
                        canal,
                        mute
                    )

                }

            )

        viewModel.setSessionCoordinator(
            sessionCoordinator
        )

        sessionCoordinator.restaurar()

        sessionCoordinator.restaurarInstrumentos()

        soundFontManager.sincronizarBiblioteca()

        synthController.setVolume(
            0,
            (viewModel.masterVolume * 127).toInt()
        )

        // ==============================================================================
        // INICIALIZAÇÃO DO DRIVER DE RÁDIO MIDI
        // ==============================================================================

        midiManager =
            MidiManager(this) {
                    mensagem: String ->

                Log.d(
                    TAG,
                    "Callback MIDI: $mensagem"
                )

                if (
                    mensagem.startsWith(
                        "CHANNEL_ACTIVITY:"
                    )
                ) {

                    val canal =
                        mensagem
                            .removePrefix(
                                "CHANNEL_ACTIVITY:"
                            )
                            .toIntOrNull()

                    if (canal != null) {

                        viewModel.ativarLedCanal(
                            canal
                        )

                    }

                }

            }

        viewModel.setMidiManager(
            midiManager
        )

        configurationSynchronizer =
            ConfigurationSynchronizer(
                midiManager
            )

        viewModel.setConfigurationSynchronizer(
            configurationSynchronizer
        )

        verificarBluetoothEIniciarMidi()

        setContent {

            MaterialTheme {

                Surface(
                    modifier =
                        Modifier.fillMaxSize(),

                    color =
                        MaterialTheme.colorScheme.background

                ) {

                    com.robsonsmartins.androidmidisynth.TelaMidiSintetizador(

                       instanciaMidiManager =
                            midiManager,

                        viewModel =
                            viewModel

                    )

                }

            }

        }

    }

    private fun verificarBluetoothEIniciarMidi() {

        val bluetoothManager =
            getSystemService(
                Context.BLUETOOTH_SERVICE
            ) as BluetoothManager

        val bluetoothAdapter =
            bluetoothManager.adapter

        if (bluetoothAdapter == null) {

            Log.d(
                TAG,
                "Bluetooth não disponível neste dispositivo"
            )

            return

        }

        if (!bluetoothAdapter.isEnabled) {

            Log.d(
                TAG,
                "Bluetooth desligado. Solicitando ativação."
            )

            val intent =
                Intent(
                    android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE
                )

            startActivityForResult(
                intent,
                102
            )

            return

        }

        Log.d(
            TAG,
            "Bluetooth ligado. Iniciando escaneamento MIDI."
        )

        midiManager.iniciarEscaneamentoAutomatico()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode == 101 &&
            ::midiManager.isInitialized
        ) {

            verificarBluetoothEIniciarMidi()

        }

    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode == 102 &&
            ::midiManager.isInitialized
        ) {

            verificarBluetoothEIniciarMidi()

        }

    }

    override fun onDestroy() {

        try {

            configurationSynchronizer.destroy()

            midiManager.finalize()

            synthManager.finalize()

        } catch (_: Exception) {}

        super.onDestroy()

    }

}