package com.robsonsmartins.androidmidisynth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.media.midi.MidiDeviceInfo

@Composable
fun TelaMidiSintetizador(
    viewModel: MainViewModel,
    onVolumeChanged: (Float) -> Unit,
    onDispositivoSelecionado: (MidiDeviceInfo) -> Unit
) {
    val volume = viewModel.volume
    val usoCpu = viewModel.usoCpu
    val listaDispositivos = viewModel.listaDispositivos

    var mostrarDialogo by remember { mutableStateOf(false) }

    val cpuAnimada by animateFloatAsState(
        targetValue = usoCpu / 100f,
        animationSpec = tween(durationMillis = 300),
        label = "AnimacaoCPU"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            text = "ESP32-S3 BLE MIDI",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )

        // Botão para abrir a lista
        Button(
            onClick = { mostrarDialogo = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00ADB5)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Selecionar Acordeon MIDI", color = Color.White)
        }

        // Janela de diálogo de escolha
        if (mostrarDialogo) {
            AlertDialog(
                onDismissRequest = { mostrarDialogo = false },
                title = { Text("Escolha o Instrumento", color = Color.White) },
                containerColor = Color(0xFF1E1E1E),
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (listaDispositivos.isEmpty()) {
                            Text("Nenhum dispositivo MIDI encontrado.", color = Color.LightGray)
                        } else {
                            for (dispositivo in listaDispositivos) {
                                val nome = dispositivo.properties.getString("name") ?: "Dispositivo Desconhecido"
                                Button(
                                    onClick = {
                                        onDispositivoSelecionado(dispositivo)
                                        mostrarDialogo = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2d2d2d))
                                ) {
                                    Text(nome, color = Color.White)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { mostrarDialogo = false }) {
                        Text("Fechar", color = Color(0xFF00ADB5))
                    }
                }
            )
        }

        // Card do Volume
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Volume Geral", color = Color.LightGray, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = volume,
                        onValueChange = { novoVolume -> onVolumeChanged(novoVolume) },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00ADB5),
                            activeTrackColor = Color(0xFF00ADB5)
                        )
                    )
                    Text(
                        text = "${(volume * 100).toInt()}%",
                        color = Color.White,
                        modifier = Modifier.padding(start = 12.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Card da CPU
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Uso de CPU do ESP32", color = Color.LightGray, fontSize = 16.sp)
                    Text(
                        text = "$usoCpu%",
                        color = if (usoCpu > 80) Color.Red else Color(0xFF00ADB5),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = cpuAnimada,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = if (usoCpu > 80) Color.Red else Color(0xFF00ADB5),
                    trackColor = Color(0xFF333333)
                )
            }
        }
    }
}
