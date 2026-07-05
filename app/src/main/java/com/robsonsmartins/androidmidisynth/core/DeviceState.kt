package com.robsonsmartins.androidmidisynth.core

import android.media.midi.MidiDeviceInfo

class DeviceState {

    var connectedDevice: MidiDeviceInfo? = null

    var bleConnected = false

    var battery = 100

    var firmwareVersion = ""

    var model = ""

}