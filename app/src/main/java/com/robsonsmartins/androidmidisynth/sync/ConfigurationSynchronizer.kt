package com.robsonsmartins.androidmidisynth.sync

import com.robsonsmartins.androidmidisynth.MidiManager
import com.robsonsmartins.androidmidisynth.protocol.ConfigPacketBuilder
import kotlinx.coroutines.*

class ConfigurationSynchronizer(

    private val midiManager: MidiManager

) {

    private val packetBuilder = ConfigPacketBuilder()

    private val scope = CoroutineScope(
        Dispatchers.Main + SupervisorJob()
    )

    private val volumeJobs =
        Array<Job?>(5) { null }

    private val instrumentJobs =
        Array<Job?>(5) { null }

    fun setVolume(
        canal: Int,
        volume: Int
    ) {

        if (canal !in 0..4)
            return

        volumeJobs[canal]?.cancel()

        volumeJobs[canal] = scope.launch {

            delay(300)

            midiManager.enviarPacoteConfiguracao(

                packetBuilder.criarPacoteSetVolume(
                    canal,
                    volume
                )

            )

        }

    }

    fun setInstrumento(
        canal: Int,
        instrumento: Int
    ) {

        if (canal !in 0..4)
            return

        instrumentJobs[canal]?.cancel()

        instrumentJobs[canal] = scope.launch {

            delay(300)

            midiManager.enviarPacoteConfiguracao(

                packetBuilder.criarPacoteSetInstrumento(
                    canal,
                    instrumento
                )

            )

        }

    }

    fun destroy() {

        scope.cancel()

    }

}