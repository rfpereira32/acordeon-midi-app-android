package com.robsonsmartins.androidmidisynth.core

class MixerState(
    numberOfChannels: Int = 5
) {

    val channels = MutableList(numberOfChannels) {

        ChannelState(channel = it)

    }

    fun getChannel(index: Int): ChannelState {

        return channels[index]

    }

}