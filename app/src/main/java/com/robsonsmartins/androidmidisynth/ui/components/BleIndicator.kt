package com.robsonsmartins.androidmidisynth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class BleState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

@Composable
fun BleIndicator(
    state: BleState
) {

    val (cor, texto) = when (state) {

        BleState.CONNECTED ->
            Color(0xFF4CAF50) to "BLE Conectado"

        BleState.CONNECTING ->
            Color(0xFFFFC107) to "Conectando..."

        BleState.DISCONNECTED ->
            Color.Red to "BLE Desconectado"

    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(cor)
        )

        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )

    }

}