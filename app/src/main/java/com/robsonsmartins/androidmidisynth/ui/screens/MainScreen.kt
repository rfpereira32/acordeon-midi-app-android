package com.robsonsmartins.androidmidisynth.ui.screens

import android.media.midi.MidiReceiver
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.robsonsmartins.androidmidisynth.MidiManager
import com.robsonsmartins.androidmidisynth.MonitorScreenContent
import com.robsonsmartins.androidmidisynth.MixerScreenContent
import com.robsonsmartins.androidmidisynth.viewmodel.MainViewModel
import com.robsonsmartins.androidmidisynth.ui.components.BleState
import com.robsonsmartins.androidmidisynth.ui.components.MainHeader

@Composable
fun MainScreen(

    deviceName: String,

    bleState: BleState,

    batteryVoltage: Float,

    batteryLevel: Int,

    mostrarMonitor: Boolean,

    selectedFileUri: Uri?,

    midiReceiver: MidiReceiver?,

    midiManager: MidiManager,

    viewModel: MainViewModel,

    onMostrarMonitor: (Boolean) -> Unit,

    onMenuClick: () -> Unit,

    onBackClick: () -> Unit

) {

    Scaffold(

        topBar = {

            MainHeader(

                deviceName = deviceName,

                showBackButton = mostrarMonitor,

                bleState = bleState,

                batteryVoltage = batteryVoltage,

                batteryLevel = batteryLevel,

                onMenuClick = onMenuClick,

                onBackClick = onBackClick

            )

        }

    ) { padding ->

        Box(

            modifier = Modifier

                .fillMaxSize()

                .background(Color(0xFF0F0F11))

                .padding(padding)

        ) {

            if (mostrarMonitor) {

                MonitorScreenContent(

                    fileUri = selectedFileUri,

                    midiReceiver = midiReceiver,

                    onFileSelected = { }

                )

            } else {

                MixerScreenContent(

                    nomeInstrumento = deviceName,

                    isConnected = bleState == BleState.CONNECTED,

                    onOtaClick = {

                        onMostrarMonitor(true)

                    },

                    midiManager = midiManager,

                    viewModel = viewModel

                )

            }

        }

    }

}