package com.robsonsmartins.androidmidisynth

import android.media.midi.MidiReceiver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object MidiEstadoCompartilhado {

    // Escutador legado mantido por retrocompatibilidade
    var onConexaoAlterada: ((String, Boolean) -> Unit)? = null

    // Referência física de envio
    var receiverMidiAtivo: MidiReceiver? = null

    // =============================================================================
    // Estado da conexão
    // =============================================================================

    var nomeDispositivoPareado by mutableStateOf(
        "Nenhum dispositivo pareado"
    )

    var isDispositivoConectado by mutableStateOf(false)

    // =============================================================================
    // Estado da bateria
    // =============================================================================

    var percentualBateria by mutableIntStateOf(100)

    var tensaoBateria by mutableFloatStateOf(0.0f)

    // =============================================================================
    // Compatibilidade
    // =============================================================================

    var nomeAtual: String
        get() = nomeDispositivoPareado
        set(value) {
            nomeDispositivoPareado = value
        }

    var conectadoAtual: Boolean
        get() = isDispositivoConectado
        set(value) {
            isDispositivoConectado = value
        }

    /**
     * Centraliza a atualização em uma única chamada segura
     */
    fun atualizarEstado(
        nome: String,
        conectado: Boolean
    ) {

        nomeDispositivoPareado = nome
        isDispositivoConectado = conectado

        if (!conectado) {

            percentualBateria = 0
            tensaoBateria = 0f

        }

        onConexaoAlterada?.invoke(
            nome,
            conectado
        )

    }

    /**
     * Reseta os estados e limpa a porta ao desconectar
     */
    fun finalizarConexao()
    {

        receiverMidiAtivo = null

        nomeDispositivoPareado =
            "Nenhum dispositivo pareado"

        isDispositivoConectado = false

        percentualBateria = 0

        tensaoBateria = 0f

    }

}