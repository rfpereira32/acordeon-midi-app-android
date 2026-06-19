package com.robsonmartins.androidmidisynth

import android.content.Context
import android.media.midi.MidiReceiver
import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class OtaManager(private val context: Context, private val midiReceiver: MidiReceiver?) {

    val statusOta = MutableStateFlow("Aguardando início...")
    val progressoOta = MutableStateFlow(0f)
    val estaAtualizando = MutableStateFlow(false)

    suspend fun iniciarAtualizacao(fileUri: Uri) {
        if (estaAtualizando.value) return
        if (midiReceiver == null) {
            statusOta.value = "Erro: Acordeon não está conectado via MIDI!"
            return
        }

        estaAtualizando.value = true
        statusOta.value = "Abrindo arquivo de firmware..."
        progressoOta.value = 0f

        try {
            val contentResolver = context.contentResolver

            // Descobre o tamanho do arquivo .bin
            val totalBytes = contentResolver.openAssetFileDescriptor(fileUri, "r")?.use {
                it.length.toInt()
            } ?: throw Exception("Não foi possível ler o tamanho do arquivo.")

            contentResolver.openInputStream(fileUri).use { inputStream ->
                if (inputStream == null) throw Exception("Falha ao abrir fluxo do arquivo.")

                statusOta.value = "Preparando ESP32-S3 para gravação..."

                // Passo 1: Handshake inicial via SysEx informando o tamanho total do firmware
                // Formato: [0xF0, 0x7D, 0x0A, 0x02, tamanho_byte1, tamanho_byte2, tamanho_byte3, tamanho_byte4, 0xF7]
                val handshakeMidi = ByteArray(9)
                handshakeMidi[0] = 0xF0.toByte() // Início do SysEx
                handshakeMidi[1] = 0x7D.toByte() // ID Educacional/Customizado
                handshakeMidi[2] = 0x0A.toByte() // Seu identificador de telemetria/sistema
                handshakeMidi[3] = 0x02.toByte() // Sub-ID: 0x02 = Comando Iniciar OTA

                // Divide o tamanho de 32 bits (Int) em 4 bytes para caber no pacote MIDI
                handshakeMidi[4] = ((totalBytes shr 24) and 0xFF).toByte()
                handshakeMidi[5] = ((totalBytes shr 16) and 0xFF).toByte()
                handshakeMidi[6] = ((totalBytes shr 8) and 0xFF).toByte()
                handshakeMidi[7] = (totalBytes and 0xFF).toByte()
                handshakeMidi[8] = 0xF7.toByte() // Fim do SysEx

                // Despacha o comando usando o driver nativo do Android
                midiReceiver.send(handshakeMidi, 0, handshakeMidi.size)

                // Aguarda 1,5 segundos para o ESP32-S3 apagar os blocos velhos na memória flash
                delay(1500)

                statusOta.value = "Transmitindo firmware sem cabos..."

                // Blocos estáveis de 256 bytes para não estourar a RAM nem o buffer do chip
                val bufferBloco = ByteArray(256)
                var bytesLidos: Int
                var totalBytesEnviados = 0

                // Passo 2: Loop pesado de faturamento real
                while (inputStream.read(bufferBloco).also { bytesLidos = it } != -1) {

                    // Constrói o envelope SysEx para carregar os bytes brutos do bloco
                    // Tamanho: 5 bytes de cabeçalho + tamanho dos dados lidos + 1 byte de fim (0xF7)
                    val envelopeSysEx = ByteArray(5 + bytesLidos + 1)
                    envelopeSysEx[0] = 0xF0.toByte()
                    envelopeSysEx[1] = 0x7D.toByte()
                    envelopeSysEx[2] = 0x0A.toByte()
                    envelopeSysEx[3] = 0x03.toByte() // Sub-ID: 0x03 = Bloco de dados Brutos do Firmware
                    envelopeSysEx[4] = bytesLidos.toByte() // Informa quantos bytes válidos vão nesse bloco

                    // Injeta os bytes do firmware para dentro do miolo do SysEx
                    System.arraycopy(bufferBloco, 0, envelopeSysEx, 5, bytesLidos)
                    envelopeSysEx[envelopeSysEx.size - 1] = 0xF7.toByte() // Fecha o SysEx

                    // Envia o pacote completo pelo ar para o acordeon
                    midiReceiver.send(envelopeSysEx, 0, envelopeSysEx.size)

                    totalBytesEnviados += bytesLidos
                    progressoOta.value = totalBytesEnviados.toFloat() / totalBytes.toFloat()

                    // Pequena pausa (12ms) para a escrita física na Flash do ESP32 respirar
                    delay(12)
                }

                statusOta.value = "Concluído! O Cordovox está reiniciando..."
                delay(2000)
            }
        } catch (e: Exception) {
            statusOta.value = "Falha no envio MIDI: ${e.message}"
        } finally {
            estaAtualizando.value = false
        }
    }
}
