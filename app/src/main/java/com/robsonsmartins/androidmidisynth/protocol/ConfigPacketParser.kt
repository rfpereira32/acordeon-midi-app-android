package com.robsonsmartins.androidmidisynth.protocol

import android.os.Handler
import android.util.Log
import com.robsonsmartins.androidmidisynth.MidiEstadoCompartilhado

class ConfigPacketParser(
    private val mainHandler: Handler
) {

    companion object {
        private const val TAG = "ConfigParser"
    }

    fun interpretar(dados: ByteArray) {

        if (dados.size < 2) {
            Log.e(TAG, "Pacote inválido.")
            return
        }

        val comando = dados[0].toInt() and 0xFF
        val tamanho = dados[1].toInt() and 0xFF

        Log.d(TAG, "Comando = $comando")
        Log.d(TAG, "Tamanho = $tamanho")
        Log.d(TAG, "Pacote = ${dados.joinToString(" ") { "%02X".format(it) }}")

        if (dados.size < tamanho + 2) {
            Log.e(TAG, "Pacote incompleto.")
            return
        }

        val payload = dados.copyOfRange(2, 2 + tamanho)

        when (comando) {

            ConfigProtocol.CMD_SYNC ->
                interpretarSync(payload)

            ConfigProtocol.CMD_BATTERY ->
                interpretarBattery(payload)

            else ->
                Log.d(TAG, "Comando desconhecido: $comando")
        }
    }

    private fun interpretarBattery(payload: ByteArray) {

        if (payload.size < 3) {
            Log.e(TAG, "CMD_BATTERY inválido.")
            return
        }

        val percentual = payload[0].toInt() and 0xFF

        val tensao =
            (payload[1].toInt() and 0xFF) or
                    ((payload[2].toInt() and 0xFF) shl 8)

        mainHandler.post {
            MidiEstadoCompartilhado.porcentagemBateriaReal = "$percentual%"
        }

        Log.d(TAG, "===== BATERIA =====")
        Log.d(TAG, "Percentual : $percentual %")
        Log.d(TAG, "Tensão     : ${tensao / 100.0f} V")
    }

    private fun interpretarSync(payload: ByteArray) {

        Log.d(TAG, "===== CMD_SYNC =====")
        Log.d(TAG, "Payload recebido (${payload.size} bytes)")

        payload.forEachIndexed { indice, valor ->
            Log.d(
                TAG,
                String.format("[%02d] = %02X", indice, valor.toInt() and 0xFF)
            )
        }
    }
}