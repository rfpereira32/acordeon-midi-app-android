package com.robsonsmartins.androidmidisynth.session

import android.content.Context
import com.robsonsmartins.androidmidisynth.configuration.ChannelConfiguration
import com.robsonsmartins.androidmidisynth.configuration.MixerConfiguration
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.util.Log

data class SessionSoundFont(

    val id: Int,

    val nome: String,

    val arquivo: String,

    val carregada: Boolean

)

data class SessionState(

    val soundFonts: MutableList<SessionSoundFont> = mutableListOf(),

    var mixer: MixerConfiguration = MixerConfiguration()

)

class SessionManager(

    context: Context

) {

    companion object {

        private const val SESSION_FILE = "session.json"

    }

    private val sessionFile = File(
        context.filesDir,
        SESSION_FILE
    )

    fun salvar(session: SessionState) {
        Log.d(
            "SessionManager",
            "Salvando sessão em ${sessionFile.absolutePath}"
        )
        val json = JSONObject()

        //----------------------------------------------------
        // SoundFonts
        //----------------------------------------------------

        val soundFonts = JSONArray()

        session.soundFonts.forEach {

            val sf = JSONObject()

            sf.put("id", it.id)
            sf.put("nome", it.nome)
            sf.put("arquivo", it.arquivo)
            sf.put("carregada", it.carregada)

            soundFonts.put(sf)

        }

        json.put(
            "soundFonts",
            soundFonts
        )

        //----------------------------------------------------
        // Mixer
        //----------------------------------------------------

        val channels = JSONArray()

        session.mixer.channels.forEach {

            val canal = JSONObject()

            canal.put(
                "enabled",
                it.enabled
            )

            canal.put(
                "soundFontId",
                it.soundFontId
            )

            canal.put(
                "bank",
                it.bank
            )

            canal.put(
                "program",
                it.program
            )

            canal.put(
                "volume",
                it.volume
            )

            canal.put(
                "mute",
                it.mute
            )

            channels.put(canal)

        }

        json.put(
            "channels",
            channels
        )
        session.mixer.channels.forEachIndexed { index, channel ->

            Log.d(
                "SessionManager",
                "Gravando canal $index " +
                        "SF=${channel.soundFontId} " +
                        "Program=${channel.program}"
            )

        }

        sessionFile.writeText(
            json.toString(4)
        )

        Log.d(
            "SessionManager",
            json.toString(4)
        )
    }

    fun carregar(): SessionState {

        if (!sessionFile.exists()) {

            return SessionState()

        }

        val json = JSONObject(

            sessionFile.readText()

        )

        val session = SessionState()

        //----------------------------------------------------
        // SoundFonts
        //----------------------------------------------------

        val soundFonts = json.optJSONArray(
            "soundFonts"
        ) ?: JSONArray()

        for (i in 0 until soundFonts.length()) {

            val sf = soundFonts.getJSONObject(i)

            session.soundFonts.add(

                SessionSoundFont(

                    id = sf.getInt("id"),

                    nome = sf.getString("nome"),

                    arquivo = sf.getString("arquivo"),

                    carregada = sf.getBoolean("carregada")

                )

            )

        }

        //----------------------------------------------------
        // Mixer
        //----------------------------------------------------

        val channels = json.optJSONArray(
            "channels"
        ) ?: JSONArray()

        session.mixer.channels.clear()

        for (i in 0 until channels.length()) {

            val canal = channels.getJSONObject(i)

            session.mixer.channels.add(

                ChannelConfiguration(

                    enabled = canal.optBoolean(
                        "enabled",
                        true
                    ),

                    soundFontId = canal.optInt(
                        "soundFontId",
                        -1
                    ),

                    bank = canal.getInt(
                        "bank"
                    ),

                    program = canal.getInt(
                        "program"
                    ),

                    volume = canal.getInt(
                        "volume"
                    ),

                    mute = canal.getBoolean(
                        "mute"
                    )

                )

            )

        }

        return session

    }

    fun limpar() {

        if (sessionFile.exists()) {

            sessionFile.delete()

        }

    }

}