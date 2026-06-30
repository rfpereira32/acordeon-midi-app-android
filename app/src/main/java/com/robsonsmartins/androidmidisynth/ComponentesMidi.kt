package com.robsonsmartins.androidmidisynth

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixerScreenContent(
    nomeInstrumento: String,
    isConnected: Boolean,
    onOtaClick: () -> Unit,
    midiManager: MidiManager // Injetado de forma estável para conectar o layout ao barramento de rádio
) {
    var exibirGavetaConfig by remember { mutableStateOf(false) }
    var modoSetupOtaAtivado by remember { mutableStateOf(false) }

    // Estados locais controlando os faders em tempo real
    var volDiscant by remember { mutableStateOf(85f) }
    var volBaixos by remember { mutableStateOf(70f) }
    var volAcordes by remember { mutableStateOf(60f) }
    var volExtras1 by remember { mutableStateOf(40f) }
    var volExtras2 by remember { mutableStateOf(50f) }

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

        // OS 5 SLIDERS INTEGRADOS À ESCALA MIDI DE 7 BITS (0 A 127) VIA STRING CHAVEADA
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StaticChannelRow("1", "Teclado / Melodia", ColorChannel1, volDiscant, false) { novoVol ->
                volDiscant = novoVol
                val valorMidi = ((novoVol / 100f) * 127f).toInt().coerceIn(0, 127)
                midiManager.despacharComandoMixerSysEx(0, valorMidi)
            }
            StaticChannelRow("2", "Baixos Fundamentais", ColorChannel2, volBaixos, true) { novoVol ->
                volBaixos = novoVol
                val valorMidi = ((novoVol / 100f) * 127f).toInt().coerceIn(0, 127)
                midiManager.despacharComandoMixerSysEx(1, valorMidi)
            }
            StaticChannelRow("3", "Acordes / Harmonia", ColorChannel3, volAcordes, false) { novoVol ->
                volAcordes = novoVol
                val valorMidi = ((novoVol / 100f) * 127f).toInt().coerceIn(0, 127)
                midiManager.despacharComandoMixerSysEx(2, valorMidi)
            }
            StaticChannelRow("4", "Instrumentos Extras 1", ColorChannel4, volExtras1, false) { novoVol ->
                volExtras1 = novoVol
                val valorMidi = ((novoVol / 100f) * 127f).toInt().coerceIn(0, 127)
                midiManager.despacharComandoMixerSysEx(3, valorMidi)
            }
            StaticChannelRow("5", "Instrumentos Extras 2", ColorChannel5, volExtras2, true) { novoVol ->
                volExtras2 = novoVol
                val valorMidi = ((novoVol / 100f) * 127f).toInt().coerceIn(0, 127)
                midiManager.despacharComandoMixerSysEx(4, valorMidi)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        // OS 4 BOTÕES DE AÇÕES RÁPIDAS NO RODAPÉ DO MIXER (CONFIG AGORA ATIVA A GAVETA)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
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
            Card(
                onClick = { exibirGavetaConfig = !exibirGavetaConfig },
                modifier = Modifier.height(60.dp).weight(1f),
                colors = CardDefaults.cardColors(containerColor = ColorCardBg)
            ) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = if (exibirGavetaConfig) ColorChannel2 else Color.LightGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Config", color = if (exibirGavetaConfig) ColorChannel2 else Color.Gray, fontSize = 10.sp)
                }
            }
        }

        // GAVETA ESTILO ACCORDION EXPANSÍVEL CONTENDO O BOTÃO SOLICITADO
        AnimatedVisibility(
            visible = exibirGavetaConfig,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = ColorCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Configurações Avançadas do Instrumento", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())

                    if (!modoSetupOtaAtivado) {
                        Button(
                            onClick = {
                                modoSetupOtaAtivado = true
                                // Invocação do disparo SysEx unificado na linha estável de rádio
                                midiManager.enviarComandoIniciarOtaWifi()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                            shape = RoundedCornerShape(8.dp),
                            enabled = isConnected,
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("OTA via Wifi AP", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        if (!isConnected) {
                            Text("Conecte o acordeon via BLE para gerenciar infraestrutura física.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2A1A1A), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFF1744).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚠️ SINAL ENVIADO COM SUCESSO!", color = Color(0xFFFF1744), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("O rádio BLE foi suspenso. Conecte seu celular ou PC no Access Point gerado pelo fole:", color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Text("SSID: ${nomeInstrumento.ifEmpty { "Acordeon_MIDI_AP" }}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Abra o navegador e acesse o IP para carregar o binário (.bin):", color = Color.LightGray, fontSize = 12.sp, textAlign = TextAlign.Center)

                            Surface(color = Color.Black, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(text = "http://192.168.4.1", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 15.sp)
                            }
                            Button(onClick = { modoSetupOtaAtivado = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242)), shape = RoundedCornerShape(6.dp), modifier = Modifier.height(32.dp)) {
                                Text("Voltar", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
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
    val otaManager = remember(MidiEstadoCompartilhado.receiverMidiAtivo) {
        OtaManager(context, MidiEstadoCompartilhado.receiverMidiAtivo)
    }

    val statusAtual by otaManager.statusOta.collectAsState()
    val progressoPercentual by otaManager.progressoOta.collectAsState()
    val estaAtualizando by otaManager.estaAtualizando.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> if (uri != null) onFileSelected(uri) }

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

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ColorCardBg)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = if (estaAtualizando) ColorChannel2 else Color.DarkGray, modifier = Modifier.size(40.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = statusAtual, color = Color.LightGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // CORRIGIDO: Passando o Float bruto diretamente para calar o erro de sobrecarga
                LinearProgressIndicator(
                    progress = progressoPercentual,
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = ColorChannel3,
                    trackColor = Color(0xFF2C2C32)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "${(progressoPercentual * 100).toInt()}%", color = ColorChannel3, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Button(onClick = { filePickerLauncher.launch("*/*") }, enabled = !estaAtualizando, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorCardBg), shape = RoundedCornerShape(8.dp)) {
            Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Escolher Arquivo .bin", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { fileUri?.let { uri -> coroutineScope.launch { otaManager.iniciarAtualizacao(uri) } } },
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
    isIndicatorOn: Boolean = false,
    onVolumeChanged: (Float) -> Unit
) {
    var currentVolume by remember { mutableStateOf(initialVolume) }
    var isMuted by remember { mutableStateOf(false) }

    val corFundoLinha = if (isMuted) Color(0xFF252528) else ColorCardBg
    val corCaixaCanal = if (isMuted) Color.Gray else accentColor
    val corTextoVolume = if (isMuted) Color.LightGray else accentColor

    Row(
        modifier = Modifier.fillMaxWidth().height(110.dp).background(corFundoLinha, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.fillMaxHeight().width(34.dp).background(corCaixaCanal, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)), contentAlignment = Alignment.Center) {
            Text(number, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(horizontal = 14.dp, vertical = 12.dp).alpha(if (isMuted) 0.4f else 1.0f), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(onClick = { isMuted = !isMuted; onVolumeChanged(if(isMuted) 0f else currentVolume) }, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = if (isMuted) Icons.Default.Clear else Icons.Default.Check, contentDescription = null, tint = if (isMuted) Color.Red else Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    Box(modifier = Modifier.size(8.dp).background(if (isIndicatorOn) Color(0xFF4CAF50) else Color.DarkGray, RoundedCornerShape(4.dp)))
                    Text("${currentVolume.toInt()}%", color = corTextoVolume, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Slider(value = currentVolume, onValueChange = { currentVolume = it; if (!isMuted) onVolumeChanged(it) }, valueRange = 0f..100f, modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = corCaixaCanal, activeTrackColor = corCaixaCanal, inactiveTrackColor = Color(0xFF2C2C32)))
        }
    }
}
