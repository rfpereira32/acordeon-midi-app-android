package com.robsonsmartins.androidmidisynth.core

/**
 * Representa uma mensagem MIDI já decodificada.
 */
data class MidiMessage(

    val status: Int,

    val channel: Int,

    val data1: Int,

    val data2: Int

)

/**
 * Tipos de mensagens MIDI suportadas.
 */
enum class MidiEventType {

    NOTE_ON,

    NOTE_OFF,

    CONTROL_CHANGE,

    PROGRAM_CHANGE,

    PITCH_BEND,

    AFTER_TOUCH,

    UNKNOWN

}

/**
 * Responsável por processar todas as mensagens MIDI recebidas.
 *
 * Esta classe será o ponto central entre:
 *
 * BLE MIDI
 * USB MIDI
 * ESP32
 * FluidSynth
 */
class MidiProcessor {

    fun process(message: MidiMessage) {

        when (decode(message.status)) {

            MidiEventType.NOTE_ON -> {

                // Implementaremos depois

            }

            MidiEventType.NOTE_OFF -> {

            }

            MidiEventType.CONTROL_CHANGE -> {

            }

            MidiEventType.PROGRAM_CHANGE -> {

            }

            MidiEventType.PITCH_BEND -> {

            }

            MidiEventType.AFTER_TOUCH -> {

            }

            MidiEventType.UNKNOWN -> {

            }
        }
    }

    /**
     * Converte o Status Byte MIDI em um tipo de evento.
     */
    private fun decode(status: Int): MidiEventType {

        return when (status and 0xF0) {

            0x80 -> MidiEventType.NOTE_OFF

            0x90 -> MidiEventType.NOTE_ON

            0xB0 -> MidiEventType.CONTROL_CHANGE

            0xC0 -> MidiEventType.PROGRAM_CHANGE

            0xD0 -> MidiEventType.AFTER_TOUCH

            0xE0 -> MidiEventType.PITCH_BEND

            else -> MidiEventType.UNKNOWN

        }
    }

}