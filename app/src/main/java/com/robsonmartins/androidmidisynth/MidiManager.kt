/*
 * Copyright (c) 2024 Robson Martins
 * (Modificado para seleção dinâmica de Acordeon)
*/

package com.robsonsmartins.androidmidisynth

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiManager as AndroidMidiManager
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver


private var outputPort: MidiOutputPort? = null

/**
 * @brief MidiManager class.
 * @details Gerencia a varredura e abertura dinâmica do canal de dados MIDI.
 */
class MidiManager(
    private val context: Context,
    private val onMidiMessageReceived: (String) -> Unit
) {
    /**
     * Expõe a porta de envio convertida em MidiReceiver para a MainActivity
     */
    fun obterReceiverMidi(): MidiReceiver? {
        return outputPort as MidiReceiver?
    }


    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as AndroidMidiManager

    fun finalize() {
        stopReadingMidi()
    }

    /** @brief Inicializa o gerenciador de MIDI sem forçar conexões automáticas por nome */
    fun start() {
        // Registra callbacks para detectar quando novos acordeons forem ligados no Bluetooth
        midiManager.registerDeviceCallback(
            object : AndroidMidiManager.DeviceCallback() {
                override fun onDeviceAdded(device: MidiDeviceInfo) {
                    onMidiMessageReceived("Dispositivo detectado: ${device.properties.getString("name")}")
                }
                override fun onDeviceRemoved(device: MidiDeviceInfo) {
                    onMidiMessageReceived("Desconectado: ${device.properties.getString("product")}")
                }
            }, null
        )
    }

    /** @brief Retorna dinamicamente a lista de instrumentos disponíveis para a tela */
    fun listarDispositivosDisponiveis(contextoEfetivo: Context): List<MidiDeviceInfo> {
        val manager = contextoEfetivo.getSystemService(Context.MIDI_SERVICE) as AndroidMidiManager
        val listaFiltrada = mutableListOf<MidiDeviceInfo>()
        val devices = manager.devices

        for (deviceInfo in devices) {
            // Ignora o próprio sintetizador interno do FluidSynth para não listar ele mesmo
            if (deviceInfo.properties.getString("product")?.lowercase() == "fluidsynth") continue

            if (deviceInfo.inputPortCount > 0 || deviceInfo.outputPortCount > 0) {
                listaFiltrada.add(deviceInfo)
            }
        }
        return listaFiltrada
    }

    /** @brief Método acionado dinamicamente quando você clica no instrumento na interface */
    /** @brief Método acionado dinamicamente quando você clica no instrumento na interface */
    /** @brief Método acionado dinamicamente quando você clica no instrumento na interface */
    /** @brief Método acionado dinamicamente quando você clica no instrumento na interface */
    fun conectarAoDispositivo(deviceInfo: MidiDeviceInfo) {
        onMidiMessageReceived("Conectando a: ${deviceInfo.properties.getString("name")}...")


        midiManager.openDevice(deviceInfo, { dispositivoAberto ->
            onMidiMessageReceived("Conectado com sucesso!")

            // 1. Para qualquer leitura anterior por segurança
            stopReadingMidi()

            try {
                // =========================================================================
                // NOVA LINHA ADICIONADA: Abre a porta de saída '0' do dispositivo Bluetooth
                // para permitir que o Android envie comandos e blocos de firmware para o ESP32
                // =========================================================================
                outputPort = dispositivoAberto.openOutputPort(0)
                android.util.Log.d("MIDI_C", "Canal de transmissão para o ESP32-S3 aberto!")

            } catch (e: Exception) {
                onMidiMessageReceived("Aviso: Falha ao abrir canal de escrita para OTA.")
                android.util.Log.e("MIDI_C", "Erro ao abrir porta de saída: ${e.message}")
            }

            // 2. 🚀 DEFINE A PORTA DE ENTRADA PADRÃO:
            // Forçamos o índice 0, que é a porta universal de transmissão física de dados MIDI de 99% dos instrumentos.
            // (Como limpamos o filtro de bits do C++ anteriormente, o SIGSEGV foi resolvido de qualquer forma!)
            val portaFisica = 0

            onMidiMessageReceived("Abrindo porta MIDI padrão: $portaFisica")

            // Inicia a thread nativa em C++ para capturar as notas do acordeon
            startReadingMidi(dispositivoAberto, portaFisica)

            onMidiMessageReceived("Canal de áudio aberto!")
        }, null)
    }

    /*
     * @brief Método de retorno (Callback) exigido pelo motor C++ (JNI).
     * @details O arquivo C++ invoca este método automaticamente a cada nota ou SysEx recebido.
     */
    @androidx.annotation.Keep // IMPEDE QUE O OTIMIZADOR DO ANDROID APAGUE ESTE MÉTODO
    @Suppress("unused")
    private fun onNativeMessageReceive(message: ByteArray) {
        val mensagemTexto = String(message).trim()
        onMidiMessageReceived(mensagemTexto) // Envia para a MainActivity processar
    }

    /*
     * @brief Definições de métodos nativos vinculados ao arquivo C++ (JNI)
     */
    private external fun startReadingMidi(receiveDevice: MidiDevice, portNumber: Int)
    private external fun stopReadingMidi()
}
