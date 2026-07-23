package com.robsonsmartins.androidmidisynth

object SysExProtocol {

    // =========================================================================
    // Estrutura do pacote SysEx
    // =========================================================================
    const val START = 0xF0
    const val END = 0xF7
    const val MANUFACTURER = 0x7D
    const val DATA_MASK = 0x7F;

    // =========================================================================
    // Índices dos bytes do pacote
    // =========================================================================
    const val IDX_START = 0
    const val IDX_MANUFACTURER = 1
    const val IDX_COMMAND = 2
    const val IDX_PARAM1 = 3
    const val IDX_PARAM2 = 4

    // =========================================================================
    // Comandos
    // =========================================================================
    const val CMD_CPU = 0x01
    const val CMD_MIXER_VOLUME = 0x05
    const val CMD_INSTRUMENTO = 0x06
    const val CMD_OTA = 0x0A
}