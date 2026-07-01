package com.robsonsmartins.androidmidisynth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.media.midi.MidiReceiver

object MidiEstadoCompartilhado {
    // Escutador legado mantido por retrocompatibilidade se outros arquivos usarem
    var onConexaoAlterada: ((String, Boolean) -> Unit)? = null

    // Referência física de envio (Usada pelo OtaManager e transmissão)
    var receiverMidiAtivo: MidiReceiver? = null

    // Estados Reativos do Jetpack Compose - A UI observa e muda o LED na hora
    var nomeDispositivoPareado by mutableStateOf("Nenhum dispositivo pareado")
    var isDispositivoConectado by mutableStateOf(false)

    // MONITOR REATIVO DA BATERIA REAL INJETADO DE FORMA LIMPA
    var porcentagemBateriaReal by mutableStateOf("100%")

    // Getters e Setters para compatibilidade com códigos antigos que buscam as variáveis antigas
    var nomeAtual: String
        get() = nomeDispositivoPareado
        set(value) { nomeDispositivoPareado = value }

    var conectadoAtual: Boolean
        get() = isDispositivoConectado
        set(value) { isDispositivoConectado = value }

    /**
     * Centraliza a atualização em uma única chamada segura
     */
    fun atualizarEstado(nome: String, conectado: Boolean) {
        nomeDispositivoPareado = nome
        isDispositivoConectado = conectado
        if (!conectado) porcentagemBateriaReal = "--%"
        onConexaoAlterada?.invoke(nome, conectado)
    }

    /**
     * Reseta os estados e limpa a porta ao desconectar
     */
    fun finalizarConexao() {
        receiverMidiAtivo = null
        nomeDispositivoPareado = "Nenhum dispositivo pareado"
        isDispositivoConectado = false
        porcentagemBateriaReal = "--%"
    }
}
