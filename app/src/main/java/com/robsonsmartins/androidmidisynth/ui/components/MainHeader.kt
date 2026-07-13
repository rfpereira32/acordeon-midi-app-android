package com.robsonsmartins.androidmidisynth.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MainHeader(

    deviceName: String,

    showBackButton: Boolean,

    bleState: BleState,

    batteryVoltage: Float,

    batteryLevel: Int,

    onMenuClick: () -> Unit,

    onBackClick: () -> Unit

) {

    Column {

        MainTopBar(

            deviceName = deviceName,

            showBackButton = showBackButton,

            onMenuClick = onMenuClick,

            onBackClick = onBackClick,

            backgroundColor = Color(0xFF0F0F11)

        )

        StatusBar(

            bleState = bleState,

            batteryVoltage = batteryVoltage,

            batteryLevel = batteryLevel

        )

    }

}