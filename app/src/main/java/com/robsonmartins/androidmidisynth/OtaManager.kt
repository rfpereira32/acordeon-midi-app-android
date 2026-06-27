package com.robsonmartins.androidmidisynth

import android.content.Context
import android.media.midi.MidiManager
import android.media.midi.MidiReceiver
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class OtaManager(private val context: Context, private val midiReceiver: MidiReceiver?) {

    val statusOta = MutableStateFlow("Aguardando início...")
    val progressoOta = MutableStateFlow(0f)
    val estaAtualizando = MutableStateFlow(false)

    suspend fun iniciarAtualizacao(fileUri: Uri) {
        if (estaAtualizando.value) return

        var receiverAtivo = midiReceiver ?: MidiEstadoCompartilhado.receiverMidiAtivo

        if (receiverAtivo == null) {
            try {
                val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
                val primeiroAparelhoConectado = midiManager.devices.firstOrNull()

                if (primeiroAparelhoConectado != null) {
                    var portaInjetada: MidiReceiver? = null
                    midiManager.openDevice(primeiroAparelhoConectado, { device ->
                        if (device != null) {
                            try {
                                portaInjetada = device.openInputPort(0) ?: device.openInputPort(1)
                                if (portaInjetada != null) {
                                    MidiEstadoCompartilhado.receiverMidiAtivo = portaInjetada
                                }
                            } catch (e: Exception) {
                                Log.e("OTA", "Erro na ponte de bypass do receiver.")
                            }
                        }
                    }, android.os.Handler(android.os.Looper.getMainLooper()))

                    delay(500)
                    receiverAtivo = portaInjetada ?: MidiEstadoCompartilhado.receiverMidiAtivo
                }
            } catch (e: Exception) {
                Log.e("OTA", "Falha ao forcar a ponte automatica: ${e.message}")
            }
        }

        if (receiverAtivo == null) {
            statusOta.value = "Erro: Canal de escrita indisponível!"
            return
        }

        estaAtualizando.value = true
        statusOta.value = "Abrindo arquivo de firmware..."
        progressoOta.value = 0f
        delay(300)

        try {
            val contentResolver = context.contentResolver
            val totalBytes = contentResolver.openAssetFileDescriptor(fileUri, "r")?.use {
                it.length.toInt()
            } ?: throw Exception("Não foi possível ler o tamanho do arquivo.")

            contentResolver.openInputStream(fileUri).use { inputStream ->
                if (inputStream == null) throw Exception("Falha ao abrir fluxo do arquivo.")

                statusOta.value = "Conectando ao instrumento..."
                delay(300)

                // PASSO 1: Handshake Inicial (7-bit)
                val handshakeMidi = ByteArray(9)
                handshakeMidi[0] = 0xF0.toByte()
                handshakeMidi[1] = 0x7D.toByte()
                handshakeMidi[2] = 0x0A.toByte()
                handshakeMidi[3] = 0x02.toByte()
                handshakeMidi[4] = ((totalBytes shr 21) and 0x7F).toByte()
                handshakeMidi[5] = ((totalBytes shr 14) and 0x7F).toByte()
                handshakeMidi[6] = ((totalBytes shr 7) and 0x7F).toByte()
                handshakeMidi[7] = (totalBytes and 0x7F).toByte()
                handshakeMidi[8] = 0xF7.toByte()

//                receiverAtivo.send(handshakeMidi, 0, handshakeMidi.size)

                try {
                    Log.d("OTA_DEBUG", "Enviando envelope: ${handshakeMidi.joinToString { String.format("0x%02X", it) }}")

                    // Envia imediatamente para o hardware informando o timestamp 0 (envio imediato)
                    receiverAtivo.send(handshakeMidi, 0, handshakeMidi.size)
                } catch (e: Exception) {
                    Log.e("OTA_DEBUG", "Falha crítica ao chamar receiver.send: ${e.message}")
                }

                // Dá um fôlego maior de 2.2 segundos para garantir o handshake dinâmico
                statusOta.value = "Iniciando gravacao dinamica..."
                delay(2200)

                statusOta.value = "Transmitindo firmware..."

                // Pacotes ultracompactos de 7 bytes puros para blindagem contra quebra de MTU
                val bufferBlocoBruto = ByteArray(7)
                var bytesBrutosLidos: Int
                var totalBytesProcessadosDoBinario = 0

                // PASSO 2: Loop de faturamento
                while (inputStream.read(bufferBlocoBruto).also { bytesBrutosLidos = it } != -1) {
                    val tamanhoDadosMidiNoAr = bytesBrutosLidos * 2
                    val envelopeSysEx = ByteArray(5 + tamanhoDadosMidiNoAr + 1)

                    envelopeSysEx[0] = 0xF0.toByte()
                    envelopeSysEx[1] = 0x7D.toByte()
                    envelopeSysEx[2] = 0x0A.toByte()
                    envelopeSysEx[3] = 0x03.toByte()
                    envelopeSysEx[4] = bytesBrutosLidos.toByte()

                    var indiceDestinoMidi = 5
                    for (i in 0 until bytesBrutosLidos) {
                        val byteBruto = bufferBlocoBruto[i].toInt()
                        val nibbleSuperior = (byteBruto shr 4) and 0x0F
                        val nibbleInferior = byteBruto and 0x0F

                        envelopeSysEx[indiceDestinoMidi++] = nibbleSuperior.toByte()
                        envelopeSysEx[indiceDestinoMidi++] = nibbleInferior.toByte()
                    }
                    envelopeSysEx[envelopeSysEx.size - 1] = 0xF7.toByte()

                    receiverAtivo.send(envelopeSysEx, 0, envelopeSysEx.size)

                    totalBytesProcessadosDoBinario += bytesBrutosLidos
                    progressoOta.value = totalBytesProcessadosDoBinario.toFloat() / totalBytes.toFloat()

                    // Delay calibrado em 22ms para casar perfeitamente com o erase-on-write do ESP32
                    delay(22)
                }

                statusOta.value = "Concluído! O Cordovox está reiniciando..."
                delay(2000)
            }
        } catch (e: Exception) {
            statusOta.value = "Falha no envio MIDI: ${e.message}"
            Log.e("OTA_ERROR", "Erro fatal na transmissao: ${e.message}")
        } finally {
            estaAtualizando.value = false
        }
    }
}
