package com.robsonsmartins.androidmidisynth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height

@Composable
fun StatusBar(

    bleState: BleState,

    batteryVoltage: Float,

    batteryLevel: Int

) {

    androidx.compose.foundation.layout.Column(

        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F11))

    ) {

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),

            horizontalArrangement = Arrangement.SpaceBetween,

            verticalAlignment = Alignment.CenterVertically

        ) {

            BleIndicator(

                state = bleState

            )

            BatteryIndicator(

                voltage = batteryVoltage,

                level = batteryLevel

            )

        }

        Box(

            modifier = Modifier

                .fillMaxWidth()

                .height(1.dp)

                .background(Color.DarkGray)

        )

    }

}