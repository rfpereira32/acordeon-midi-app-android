package com.robsonsmartins.androidmidisynth.session

import android.content.Context
import android.util.Log
import com.robsonsmartins.androidmidisynth.configuration.ChannelConfiguration
import com.robsonsmartins.androidmidisynth.configuration.MixerConfiguration
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SessionManager(

    context: Context

) {

    companion object {

        private const val SESSION_FILE =
            "session.json"

    }

    private val sessionFile =
        File(
            context.filesDir,
            SESSION_FILE
        )

    fun salvar(
        session: SessionState
    ) {

        Log.d(
            "SessionManager",
            "Salvando sessão em ${sessionFile.absolutePath}"
        )

        val json =
            JSONObject()

        // ================================================================
        // SoundFonts
        // ================================================================

        val soundFonts =
            JSONArray()

        session.soundFonts.forEach {

            val sf =
                JSONObject()

            sf.put(
                "id",
                it.id
            )

            sf.put(
                "nome",
                it.nome
            )

            sf.put(
                "arquivo",
                it.arquivo
            )

            sf.put(
                "carregada",
                it.carregada
            )

            soundFonts.put(
                sf
            )

        }

        json.put(
            "soundFonts",
            soundFonts
        )

        // ================================================================
        // Mixer
        // ================================================================

        val mixer =
            JSONObject()

        mixer.put(
            "channel1AsMaster",
            session.mixer.channel1AsMaster
        )

        // ================================================================
        // Canais
        // ================================================================

        val channels =
            JSONArray()

        session.mixer.channels.forEachIndexed {
                index,
                channelConfig ->

            Log.d(
                "SessionManager",
                "Gravando canal $index " +
                        "SF=${channelConfig.soundFontId} " +
                        "Program=${channelConfig.program} " +
                        "Controle=${channelConfig.controlSource}"
            )

            val canal =
                JSONObject()

            canal.put(
                "enabled",
                channelConfig.enabled
            )

            canal.put(
                "soundFontId",
                channelConfig.soundFontId
            )

            canal.put(
                "bank",
                channelConfig.bank
            )

            canal.put(
                "program",
                channelConfig.program
            )

            canal.put(
                "volume",
                channelConfig.volume
            )

            canal.put(
                "mute",
                channelConfig.mute
            )

            // ------------------------------------------------------------
            // Controle do acordeão
            //
            // 1 = Controle 1
            // 2 = Controle 2
            // 3 = Controle 3
            // 4 = Controles 2 + 3
            // ------------------------------------------------------------

            canal.put(
                "controlSource",
                channelConfig.controlSource
            )

            channels.put(
                canal
            )

        }

        mixer.put(
            "channels",
            channels
        )

        json.put(
            "mixer",
            mixer
        )

        // ================================================================
        // Grava arquivo
        // ================================================================

        sessionFile.writeText(
            json.toString(4)
        )

        Log.d(
            "SessionManager",
            "Sessão salva com sucesso"
        )

        Log.d(
            "SessionManager",
            json.toString(4)
        )

    }

    fun carregar(): SessionState {

        if (!sessionFile.exists()) {

            Log.d(
                "SessionManager",
                "Arquivo de sessão não encontrado"
            )

            return SessionState()

        }

        Log.d(
            "SessionManager",
            "Carregando sessão de ${sessionFile.absolutePath}"
        )

        val json =
            JSONObject(
                sessionFile.readText()
            )

        val session =
            SessionState()

        // ================================================================
        // SoundFonts
        // ================================================================

        val soundFonts =
            json.optJSONArray(
                "soundFonts"
            )
                ?: JSONArray()

        for (
        i in 0 until soundFonts.length()
        ) {

            val sf =
                soundFonts.getJSONObject(i)

            session.soundFonts.add(

                SessionSoundFont(

                    id =
                        sf.getInt(
                            "id"
                        ),

                    nome =
                        sf.getString(
                            "nome"
                        ),

                    arquivo =
                        sf.getString(
                            "arquivo"
                        ),

                    carregada =
                        sf.getBoolean(
                            "carregada"
                        )

                )

            )

        }

        // ================================================================
        // Mixer
        // ================================================================

        /*
         * A versão atual salva o mixer dentro de um objeto
         * "mixer".
         *
         * Mantemos também compatibilidade com o formato
         * antigo, onde "channels" ficava diretamente na raiz.
         */
        val mixerJson =
            json.optJSONObject(
                "mixer"
            )

        val channels =
            mixerJson?.optJSONArray(
                "channels"
            )
                ?: json.optJSONArray(
                    "channels"
                )
                ?: JSONArray()

        // ================================================================
        // Canais
        // ================================================================

        val channelConfigurations =
            mutableListOf<ChannelConfiguration>()

        for (
        i in 0 until channels.length()
        ) {

            val canal =
                channels.getJSONObject(i)

            val controlSource =
                canal.optInt(
                    "controlSource",
                    1
                ).coerceIn(
                    1,
                    4
                )

            Log.d(
                "SessionManager",
                "Carregando canal $i " +
                        "Controle=$controlSource"
            )

            channelConfigurations.add(

                ChannelConfiguration(

                    enabled =
                        canal.optBoolean(
                            "enabled",
                            true
                        ),

                    soundFontId =
                        canal.optInt(
                            "soundFontId",
                            -1
                        ),

                    bank =
                        canal.optInt(
                            "bank",
                            0
                        ),

                    program =
                        canal.optInt(
                            "program",
                            0
                        ),

                    volume =
                        canal.optInt(
                            "volume",
                            100
                        ),

                    mute =
                        canal.optBoolean(
                            "mute",
                            false
                        ),

                    controlSource =
                        controlSource

                )

            )

        }

        val channel1AsMaster =
            mixerJson?.optBoolean(
                "channel1AsMaster",
                false
            )
                ?: json.optBoolean(
                    "channel1AsMaster",
                    false
                )

        session.mixer =
            MixerConfiguration(

                channels =
                    channelConfigurations,

                channel1AsMaster =
                    channel1AsMaster

            )

        Log.d(
            "SessionManager",
            "Canal 1 como Master = $channel1AsMaster"
        )

        Log.d(
            "SessionManager",
            "Sessão carregada com sucesso"
        )

        return session

    }

    fun limpar() {

        if (sessionFile.exists()) {

            sessionFile.delete()

            Log.d(
                "SessionManager",
                "Sessão apagada"
            )

        }

    }

}