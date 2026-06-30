package com.robsonsmartins.androidmidisynth

import android.content.Context
import android.media.midi.MidiReceiver
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.InputStream

class OtaManager(private val context: Context, private val receiver: MidiReceiver?) {

    val statusOta = MutableStateFlow("Aguardando arquivo binário...")
    val progressoOta = MutableStateFlow(0f)
    val estaAtualizando = MutableStateFlow(false)

    fun iniciarAtualizacao(fileUri: Uri) {
        // CORREÇÃO CRÍTICA DO SMART CAST: Salva o ponteiro atual em uma constante imutável
        val portaEstavel = receiver
        if (portaEstavel == null) {
            statusOta.value = "Erro: Instrumento Desconectado."
            return
        }

        estaAtualizando.value = true
        statusOta.value = "Preparando transmissão..."

        try {
            val inputStream = context.contentResolver.openInputStream(fileUri)
            if (inputStream != null) {
                transmitirFirmwareBinarioEstavel(inputStream, portaEstavel)
            } else {
                statusOta.value = "Falha ao ler o arquivo .bin"
                estaAtualizando.value = false
            }
        } catch (e: Exception) {
            statusOta.value = "Erro fatal: ${e.message}"
            estaAtualizando.value = false
        }
    }

    private fun transmitirFirmwareBinarioEstavel(inputStream: InputStream, portaInjetada: MidiReceiver) {
        val bufferDeFatiamento = ByteArray(64)

        try {
            var bytesLidos = inputStream.read(bufferDeFatiamento)
            var totalBytesEnviados = 0

            while (bytesLidos != -1) {
                portaInjetada.send(bufferDeFatiamento, 0, bytesLidos)
                totalBytesEnviados += bytesLidos
                progressoOta.value = 0.5f

                Thread.sleep(5)
                bytesLidos = inputStream.read(bufferDeFatiamento)
            }
            statusOta.value = "Firmware enviado com sucesso!"
            progressoOta.value = 1.0f
        } catch (e: Exception) {
            statusOta.value = "Erro na transmissão: ${e.message}"
        } finally {
            estaAtualizando.value = false
            try { inputStream.close() } catch (_: Exception) {}
        }
    }
}
