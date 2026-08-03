package com.robsonsmartins.androidmidisynth.protocol

class ConfigPacketBuilder {

    fun criarPacoteStatus(): ByteArray {
        return criarPacote(
            ConfigProtocol.CMD_STATUS
        )
    }

    fun criarPacoteVersao(): ByteArray {
        return criarPacote(
            ConfigProtocol.CMD_VERSION
        )
    }

    fun criarPacoteRequestSync(): ByteArray {
        return criarPacote(
            ConfigProtocol.CMD_REQUEST_SYNC
        )
    }

    fun criarPacotePreset(
        preset: Int
    ): ByteArray {

        return criarPacote(
            ConfigProtocol.CMD_PRESET,
            byteArrayOf(
                preset.toByte()
            )
        )
    }

    fun criarPacoteVolume(
        canal: Int,
        volume: Int
    ): ByteArray {

        return criarPacote(
            ConfigProtocol.CMD_VOLUME,
            byteArrayOf(
                canal.toByte(),
                volume.toByte()
            )
        )
    }

    fun criarPacoteInstrumento(
        canal: Int,
        instrumento: Int
    ): ByteArray {

        return criarPacote(
            ConfigProtocol.CMD_INSTRUMENT,
            byteArrayOf(
                canal.toByte(),
                instrumento.toByte()
            )
        )
    }

    private fun criarPacote(
        comando: Int,
        payload: ByteArray = byteArrayOf()
    ): ByteArray {

        val pacote = ByteArray(payload.size + 2)

        pacote[0] = comando.toByte()
        pacote[1] = payload.size.toByte()

        payload.copyInto(
            destination = pacote,
            destinationOffset = 2
        )

        return pacote
    }

    fun criarPacoteSetVolume(
        canal: Int,
        volume: Int
    ): ByteArray {

        return criarPacote(
            ConfigProtocol.CMD_SET_VOLUME,
            byteArrayOf(
                canal.toByte(),
                volume.toByte()
            )
        )
    }
}