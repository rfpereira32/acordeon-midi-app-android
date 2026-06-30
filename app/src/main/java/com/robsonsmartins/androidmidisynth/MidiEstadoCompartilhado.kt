package com.robsonsmartins.androidmidisynth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.media.midi.MidiInputPort

object MidiEstadoCompartilhado {
    var nomeDispositivoPareado by mutableStateOf("Nenhum dispositivo pareado")
    var isDispositivoConectado by mutableStateOf(false)
    var receiverMidiAtivo: MidiInputPort? = null

    fun atualizarEstado(nome: String, conectado: Boolean) {
        nomeDispositivoPareado = nome
        isDispositivoConectado = conectado
    }
}
