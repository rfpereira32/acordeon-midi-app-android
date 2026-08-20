package com.robsonsmartins.androidmidisynth.session

import android.content.Context
import android.util.Log
import com.robsonsmartins.androidmidisynth.configuration.ChannelConfiguration
import com.robsonsmartins.androidmidisynth.configuration.MixerConfiguration
import com.robsonsmartins.androidmidisynth.configuration.PresetConfiguration
import com.robsonsmartins.androidmidisynth.configuration.PresetSoundFont
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class PresetManager(

    context: Context

) {

    companion object {

        private const val PRESETS_FILE =
            "presets.json"

    }

    private val presetsFile =
        File(
            context.filesDir,
            PRESETS_FILE
        )

    /**
     * Salva um preset.
     *
     * Se já existir um preset com o mesmo nome,
     * ele será substituído.
     */
    fun salvar(
        preset: PresetConfiguration
    ) {

        val presets =
            carregarTodos().toMutableList()

        val indiceExistente =
            presets.indexOfFirst {

                it.nome.equals(
                    preset.nome,
                    ignoreCase = true
                )

            }

        if (indiceExistente >= 0) {

            presets[indiceExistente] =
                preset

        } else {

            presets.add(
                preset
            )

        }

        gravarTodos(
            presets
        )

        Log.d(
            "PresetManager",
            "Preset '${preset.nome}' salvo"
        )

    }

    /**
     * Carrega um preset pelo nome.
     */
    fun carregar(
        nome: String
    ): PresetConfiguration? {

        return carregarTodos().firstOrNull {

            it.nome.equals(
                nome,
                ignoreCase = true
            )

        }

    }

    /**
     * Retorna todos os presets salvos.
     */
    fun listar(): List<PresetConfiguration> {

        return carregarTodos()

    }

    /**
     * Exclui um preset pelo nome.
     *
     * @return true se o preset foi encontrado e excluído.
     */
    fun excluir(
        nome: String
    ): Boolean {

        val presets =
            carregarTodos().toMutableList()

        val removido =
            presets.removeAll {

                it.nome.equals(
                    nome,
                    ignoreCase = true
                )

            }

        if (!removido)
            return false

        gravarTodos(
            presets
        )

        Log.d(
            "PresetManager",
            "Preset '$nome' excluído"
        )

        return true

    }

    /**
     * Carrega todos os presets do arquivo JSON.
     */
    private fun carregarTodos():
            List<PresetConfiguration> {

        if (!presetsFile.exists()) {

            return emptyList()

        }

        return try {

            val json =
                JSONObject(
                    presetsFile.readText()
                )

            val presetsJson =
                json.optJSONArray(
                    "presets"
                )
                    ?: JSONArray()

            val presets =
                mutableListOf<PresetConfiguration>()

            for (
            i in 0 until presetsJson.length()
            ) {

                val presetJson =
                    presetsJson.getJSONObject(i)

                presets.add(
                    lerPreset(
                        presetJson
                    )
                )

            }

            presets

        } catch (e: Exception) {

            Log.e(
                "PresetManager",
                "Erro ao carregar presets",
                e
            )

            emptyList()

        }

    }

    /**
     * Grava todos os presets no arquivo JSON.
     */
    private fun gravarTodos(
        presets: List<PresetConfiguration>
    ) {

        val json =
            JSONObject()

        val presetsJson =
            JSONArray()

        presets.forEach { preset ->

            presetsJson.put(
                criarJson(
                    preset
                )
            )

        }

        json.put(
            "presets",
            presetsJson
        )

        presetsFile.writeText(
            json.toString(4)
        )

    }

    /**
     * Converte um preset para JSON.
     */
    private fun criarJson(
        preset: PresetConfiguration
    ): JSONObject {

        val json =
            JSONObject()

        json.put(
            "nome",
            preset.nome
        )

        // ================================================================
        // SoundFonts
        // ================================================================

        val soundFonts =
            JSONArray()

        preset.soundFonts.forEach { soundFont ->

            val sf =
                JSONObject()

            sf.put(
                "id",
                soundFont.id
            )

            sf.put(
                "nome",
                soundFont.nome
            )

            sf.put(
                "arquivo",
                soundFont.arquivo
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
            preset.mixer.channel1AsMaster
        )

        val channels =
            JSONArray()

        preset.mixer.channels.forEach { channel ->

            val canal =
                JSONObject()

            canal.put(
                "enabled",
                channel.enabled
            )

            canal.put(
                "soundFontId",
                channel.soundFontId
            )

            canal.put(
                "bank",
                channel.bank
            )

            canal.put(
                "program",
                channel.program
            )

            canal.put(
                "volume",
                channel.volume
            )

            canal.put(
                "mute",
                channel.mute
            )

            canal.put(
                "controlSource",
                channel.controlSource
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

        return json

    }

    /**
     * Converte um objeto JSON em um preset.
     */
    private fun lerPreset(
        json: JSONObject
    ): PresetConfiguration {

        // ================================================================
        // Nome
        // ================================================================

        val nome =
            json.optString(
                "nome",
                "Preset"
            )

        // ================================================================
        // SoundFonts
        // ================================================================

        val soundFontsJson =
            json.optJSONArray(
                "soundFonts"
            )
                ?: JSONArray()

        val soundFonts =
            mutableListOf<PresetSoundFont>()

        for (
        i in 0 until soundFontsJson.length()
        ) {

            val sf =
                soundFontsJson.getJSONObject(i)

            soundFonts.add(

                PresetSoundFont(

                    id =
                        sf.optInt(
                            "id",
                            -1
                        ),

                    nome =
                        sf.optString(
                            "nome",
                            ""
                        ),

                    arquivo =
                        sf.optString(
                            "arquivo",
                            ""
                        )

                )

            )

        }

        // ================================================================
        // Mixer
        // ================================================================

        val mixerJson =
            json.optJSONObject(
                "mixer"
            )
                ?: JSONObject()

        val channelsJson =
            mixerJson.optJSONArray(
                "channels"
            )
                ?: JSONArray()

        val channels =
            mutableListOf<ChannelConfiguration>()

        for (
        i in 0 until channelsJson.length()
        ) {

            val canal =
                channelsJson.getJSONObject(i)

            val controlSource =
                canal.optInt(
                    "controlSource",
                    when (i) {
                        0 -> 1
                        1 -> 2
                        2 -> 3
                        else -> 4
                    }
                ).coerceIn(
                    1,
                    4
                )

            channels.add(

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

        /*
         * Se o preset não possuir canais, utilizamos
         * a configuração padrão do mixer.
         */
        val mixer =
            if (channels.isEmpty()) {

                MixerConfiguration()

            } else {

                MixerConfiguration(

                    channels =
                        channels,

                    channel1AsMaster =
                        mixerJson.optBoolean(
                            "channel1AsMaster",
                            false
                        )

                )

            }

        return PresetConfiguration(

            nome =
                nome,

            soundFonts =
                soundFonts,

            mixer =
                mixer

        )

    }

}