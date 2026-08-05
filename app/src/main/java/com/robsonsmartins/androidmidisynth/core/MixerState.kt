package com.robsonsmartins.androidmidisynth.core

import androidx.compose.runtime.mutableStateListOf

class MixerState(
    numberOfChannels: Int = 5
) {

    val channels = mutableStateListOf<ChannelState>()

    init {
        repeat(numberOfChannels) {
            channels += ChannelState(channel = it)
        }
    }

    fun getChannel(index: Int): ChannelState {
        return channels[index]
    }

    fun usaSoundFont(id: Int): Boolean {

        return channels.any {

            it.soundFont?.id == id

        }

    }
}