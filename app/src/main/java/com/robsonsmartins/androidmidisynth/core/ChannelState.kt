package com.robsonsmartins.androidmidisynth.core

data class ChannelState(

    val channel: Int,

    var volume: Int = 100,

    var muted: Boolean = false,

    var expression: Int = 127,

    var pan: Int = 64,

    var bankMsb: Int = 0,

    var bankLsb: Int = 0,

    var program: Int = 0,

    var instrumentName: String = "",

    var soundFont: String = "",

    var led: Boolean = false

)