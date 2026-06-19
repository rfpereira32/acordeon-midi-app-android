package com.robsonmartins.androidmidisynth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MixerScreenContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFF4CAF50), RoundedCornerShape(4.dp)))
            Text(" BLE Ativo   |   \uD83C\uDFB5 Scandalli Master ▾", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StaticChannelRow("1", "Teclado / Melodia", ColorChannel1, 85f)
            StaticChannelRow("2", "Baixos Fundamentais", ColorChannel2, 70f, true)
            StaticChannelRow("3", "Acordes / Harmonia", ColorChannel3, 60f)
            StaticChannelRow("4", "Instrumentos Extras 1", ColorChannel4, 40f)
            StaticChannelRow("5", "Instrumentos Extras 2", ColorChannel5, 50f, true)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Botão 1
            Card(modifier = Modifier.height(60.dp).weight(1f), colors = CardDefaults.cardColors(containerColor = ColorCardBg)) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("SoundFont", color = Color.Gray, fontSize = 10.sp)
                }
            }
            // Botão 2
            Card(modifier = Modifier.height(60.dp).weight(1f), colors = CardDefaults.cardColors(containerColor = ColorCardBg)) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Presets", color = Color.Gray, fontSize = 10.sp)
                }
            }
            // Botão 3
            Card(modifier = Modifier.height(60.dp).weight(1f), colors = CardDefaults.cardColors(containerColor = ColorCardBg)) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("OTA Update", color = Color.Gray, fontSize = 10.sp)
                }
            }
            // Botão 4
            Card(modifier = Modifier.height(60.dp).weight(1f), colors = CardDefaults.cardColors(containerColor = ColorCardBg)) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Config", color = Color.Gray, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun MonitorScreenContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Telemetria do Instrumento", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Card(modifier = Modifier.fillMaxWidth().height(180.dp), colors = CardDefaults.cardColors(containerColor = ColorCardBg)) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("[Espaço do Gráfico de Linha Animado]", color = Color.Gray, fontSize = 14.sp)
                    Text("Uso de CPU do ESP32-S3 em tempo real", color = Color.DarkGray, fontSize = 12.sp)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ColorCardBg)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Informações do Hardware", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Divider(color = Color.DarkGray, thickness = 1.dp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Status da Conexão", color = Color.Gray, fontSize = 14.sp)
                    Text("Conectado (BLE)", color = ColorChannel3, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Frequência de Envio", color = Color.Gray, fontSize = 14.sp)
                    Text("300 ms (SysEx)", color = Color.White, fontSize = 14.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pacotes Recebidos", color = Color.Gray, fontSize = 14.sp)
                    Text("0 (Modo de Demonstração)", color = Color.Yellow, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun StaticChannelRow(
    number: String,
    name: String,
    accentColor: Color,
    initialVolume: Float,
    isIndicatorOn: Boolean = false
) {
    var currentVolume by remember { mutableStateOf(initialVolume) }
    var isMuted by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp).background(ColorCardBg, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.fillMaxHeight().width(50.dp).background(accentColor, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = { isMuted = !isMuted }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.Clear else Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isMuted) Color.Red else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier.size(8.dp).background(if (isIndicatorOn) Color(0xFF4CAF50) else Color.DarkGray, RoundedCornerShape(4.dp))
                    )
                    Text("${currentVolume.toInt()}%", color = accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Slider(
                value = currentVolume,
                onValueChange = { currentVolume = it },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth().height(16.dp),
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = Color(0xFF2C2C32)
                )
            )
        }
    }
}
