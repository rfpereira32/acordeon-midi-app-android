package com.robsonmartins.androidmidisynth

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixerScreenContent(nomeInstrumento: String, isConnected: Boolean, onOtaClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isConnected) Color(0xFF4CAF50) else Color.Red,
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            Text(
                text = if (isConnected) " BLE Ativo   |   🎵 $nomeInstrumento ▾" else " BLE Desconectado   |   🎵 $nomeInstrumento ▾",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // OS 5 SLIDERS DE VOLUME DO MIXER COM AS CAIXAS LATERAIS ESTREITAS
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StaticChannelRow("1", "Teclado / Melodia", ColorChannel1, 85f)
            StaticChannelRow("2", "Baixos Fundamentais", ColorChannel2, 70f, true)
            StaticChannelRow("3", "Acordes / Harmonia", ColorChannel3, 60f)
            StaticChannelRow("4", "Instrumentos Extras 1", ColorChannel4, 40f)
            StaticChannelRow("5", "Instrumentos Extras 2", ColorChannel5, 50f, true)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // OS 4 BOTÕES DE AÇÕES RÁPIDAS NO RODAPÉ DO MIXER
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(modifier = Modifier.height(60.dp).weight(1f), colors = CardDefaults.cardColors(containerColor = ColorCardBg)) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("SoundFont", color = Color.Gray, fontSize = 10.sp)
                }
            }
            Card(modifier = Modifier.height(60.dp).weight(1f), colors = CardDefaults.cardColors(containerColor = ColorCardBg)) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Presets", color = Color.Gray, fontSize = 10.sp)
                }
            }
            Card(
                onClick = onOtaClick,
                modifier = Modifier.height(60.dp).weight(1f),
                colors = CardDefaults.cardColors(containerColor = ColorCardBg)
            ) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("OTA Update", color = Color.Gray, fontSize = 10.sp)
                }
            }
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
fun MonitorScreenContent(
    fileUri: Uri?,
    midiReceiver: android.media.midi.MidiReceiver?,
    onFileSelected: (Uri) -> Unit
) {
    val context = LocalContext.current

    // Instancia o OtaManager usando o barramento reativo compartilhado
    val otaManager = remember(MidiEstadoCompartilhado.receiverMidiAtivo) {
        OtaManager(context, MidiEstadoCompartilhado.receiverMidiAtivo)
    }

    val statusAtual by otaManager.statusOta.collectAsState()
    val progressoPercentual by otaManager.progressoOta.collectAsState()
    val estaAtualizando by otaManager.estaAtualizando.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onFileSelected(uri)
        }
    }

    LaunchedEffect(fileUri, estaAtualizando) {
        if (fileUri != null && !estaAtualizando) {
            otaManager.statusOta.value = "Pronto para enviar: ${fileUri.lastPathSegment ?: "firmware.bin"}"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Atualização de Sistema (OTA)", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ColorCardBg)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = if (estaAtualizando) ColorChannel2 else Color.DarkGray,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(text = statusAtual, color = Color.LightGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // CORRIGIDO: Passando o Float direto sem as chaves lambda {}
                LinearProgressIndicator(
                    progress = progressoPercentual,
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = ColorChannel3,
                    trackColor = Color(0xFF2C2C32)
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${(progressoPercentual * 100).toInt()}%",
                    color = ColorChannel3,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = { filePickerLauncher.launch("*/*") },
            enabled = !estaAtualizando,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ColorCardBg),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Escolher Arquivo .bin", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = {
                fileUri?.let { uriValida ->
                    coroutineScope.launch {
                        otaManager.iniciarAtualizacao(uriValida)
                    }
                }
            },
            enabled = fileUri != null && !estaAtualizando,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ColorChannel1),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Enviar Novo Firmware via BLE", color = Color.White, fontWeight = FontWeight.Bold)
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

    val corFundoLinha = if (isMuted) Color(0xFF252528) else ColorCardBg
    val corCaixaCanal = if (isMuted) Color.Gray else accentColor
    val corTextoVolume = if (isMuted) Color.LightGray else accentColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(corFundoLinha, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(34.dp)
                .background(corCaixaCanal, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .alpha(if (isMuted) 0.4f else 1.0f),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    Text("${currentVolume.toInt()}%", color = corTextoVolume, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Slider(
                value = currentVolume,
                onValueChange = { currentVolume = it },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = corCaixaCanal,
                    activeTrackColor = corCaixaCanal,
                    inactiveTrackColor = Color(0xFF2C2C32)
                )
            )
        }
    }
}
