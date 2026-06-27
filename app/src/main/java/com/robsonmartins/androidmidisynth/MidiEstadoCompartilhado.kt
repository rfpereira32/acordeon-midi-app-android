package com.robsonmartins.androidmidisynth

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
        onConexaoAlterada?.invoke(nome, conectado)
    }

    /**
     * Reseta os estados e limpa a porta ao desconectar
     */
    fun finalizarConexao() {
        // Apenas removemos o receiver do barramento ativo, quem fecha a porta é o MidiManager
        receiverMidiAtivo = null
        nomeDispositivoPareado = "Nenhum dispositivo pareado"
        isDispositivoConectado = false
    }

}
