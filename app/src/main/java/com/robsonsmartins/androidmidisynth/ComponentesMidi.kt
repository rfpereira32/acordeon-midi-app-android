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
import com.robsonsmartins.androidmidisynth.viewmodel.MainViewModel
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontDialog
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontInfo
import androidx.compose.foundation.clickable
import com.robsonsmartins.androidmidisynth.ui.components.InstrumentPickerDialog
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixerScreenContent(
    nomeInstrumento: String,
    isConnected: Boolean,
    onOtaClick: () -> Unit,
    midiManager: MidiManager, // Injetado de forma estável para conectar o layout ao barramento de rádio
    viewModel: MainViewModel
) {
    var exibirGavetaConfig by remember { mutableStateOf(false) }
    var modoSetupOtaAtivado by remember { mutableStateOf(false) }

    var exibirDialogoSoundFont by remember { mutableStateOf(false) }

    var exibirInstrumentPicker by remember { mutableStateOf(false) }

    var canalSelecionado by remember { mutableIntStateOf(0) }

    val soundFonts = viewModel.listarSoundFonts()

    val launcherSoundFont =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            uri?.let {

                viewModel.importarSoundFont(it)

            }

        }

    // Estados locais controlando os faders em tempo real
    val canais = viewModel.getChannels()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val conectado = MidiEstadoCompartilhado.isDispositivoConectado

        val corBluetooth =
            if (conectado)
                Color(0xFF2196F3)
            else
                Color.Gray

        val corBateria =
            if (!conectado) {
                Color.Gray
            } else {
                when {
                    MidiEstadoCompartilhado.percentualBateria > 60 ->
                        Color(0xFF4CAF50)

                    MidiEstadoCompartilhado.percentualBateria > 30 ->
                        Color(0xFFFFC107)

                    MidiEstadoCompartilhado.percentualBateria > 15 ->
                        Color(0xFFFF9800)

                    else ->
                        Color.Red
                }
            }

        val textoPercentual =
            if (conectado)
                "${MidiEstadoCompartilhado.percentualBateria}%"
            else
                "-%"

        val textoTensao =
            if (conectado)
                String.format(
                    "%.2f V",
                    MidiEstadoCompartilhado.tensaoBateria
                )
            else
                "--.-- V"

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 2.dp,
                    bottom = 4.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    painter = painterResource(R.drawable.ic_bluetooth),
                    contentDescription = "Bluetooth",
                    tint = corBluetooth
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "BLE",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        painter = painterResource(R.drawable.ic_battery),
                        contentDescription = "Bateria",
                        tint = corBateria
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = textoPercentual,
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = textoTensao,
                    color = Color.Gray,
                    fontSize = 13.sp
                )

            }

        }

        // OS 5 SLIDERS INTEGRADOS À ESCALA MIDI DE 7 BITS (0 A 127) VIA STRING CHAVEADA
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StaticChannelRow(
                number = "1",
                name = canais[0].preset?.nome ?: "Teclado",
                accentColor = ColorChannel1,
                volume = canais[0].volume * 100f / 127f,

                isMuted = canais[0].muted,

                isIndicatorOn = false,

                onInstrumentClick = {
                    canalSelecionado = 0
                    exibirInstrumentPicker = true
                },

                onVolumeChanged = { novoVol ->

                    val valorMidi = percentToMidi(novoVol)

                    viewModel.setChannelVolume(0, valorMidi)
                },

                onMuteChanged = { mute ->

                    viewModel.setChannelMute(0, mute)

                }
            )

            StaticChannelRow(
                    number = "2",
            name = canais[1].preset?.nome ?: "Baixos fundamentais",
                accentColor = ColorChannel1,
                volume = canais[1].volume * 100f / 127f,

                isMuted = canais[1].muted,

                isIndicatorOn = false,
                onInstrumentClick = {
                    canalSelecionado = 1
                    exibirInstrumentPicker = true
                },

            onVolumeChanged = { novoVol ->

                val valorMidi = percentToMidi(novoVol)

                viewModel.setChannelVolume(1, valorMidi)

            },

            onMuteChanged = { mute ->

                viewModel.setChannelMute(1, mute)

            }
            )

            StaticChannelRow(
            number = "3",
            name = canais[2].preset?.nome ?: "Acordes",
            accentColor = ColorChannel1,
                volume = canais[2].volume * 100f / 127f,

                isMuted = canais[2].muted,

                isIndicatorOn = false,
                onInstrumentClick = {
                    canalSelecionado = 2
                    exibirInstrumentPicker = true
                },

            onVolumeChanged = { novoVol ->

                val valorMidi = percentToMidi(novoVol)

                viewModel.setChannelVolume(2, valorMidi)
            },

                onMuteChanged = { mute ->

                viewModel.setChannelMute(2, mute)

            }
        )

            StaticChannelRow(
                number = "4",
                name = canais[3].preset?.nome ?: "Instrumentos Extras 1",
            accentColor = ColorChannel4,
                volume = canais[3].volume * 100f / 127f,

                isMuted = canais[3].muted,

                isIndicatorOn = false,
                onInstrumentClick = {
                    canalSelecionado = 3
                    exibirInstrumentPicker = true
                },

                onVolumeChanged = { novoVol ->

                val valorMidi = percentToMidi(novoVol)

                viewModel.setChannelVolume(3, valorMidi)
            },

            onMuteChanged = { mute ->

                viewModel.setChannelMute(3, mute)

            }
            )

            StaticChannelRow(
            number = "5",
            name = canais[4].preset?.nome ?: "Instrumentos Extras 2",
            accentColor = ColorChannel4,
                volume = canais[4].volume * 100f / 127f,

                isMuted = canais[4].muted,

                isIndicatorOn = false,
                onInstrumentClick = {
                    canalSelecionado = 4
                    exibirInstrumentPicker = true
                },

                onVolumeChanged = { novoVol ->

                val valorMidi = percentToMidi(novoVol)

                viewModel.setChannelVolume(4, valorMidi)
            },

            onMuteChanged = { mute ->

                viewModel.setChannelMute(4, mute)

            }
        )
        }
        Spacer(modifier = Modifier.height(8.dp))
        // OS 4 BOTÕES DE AÇÕES RÁPIDAS NO RODAPÉ DO MIXER (CONFIG AGORA ATIVA A GAVETA)
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(

                onClick = {

                    exibirDialogoSoundFont = true

                },

                modifier = Modifier
                    .height(60.dp)
                    .weight(1f),

                colors = CardDefaults.cardColors(
                    containerColor = ColorCardBg
                )

            ) {
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
    if (exibirDialogoSoundFont) {

        SoundFontDialog(

            soundFonts = soundFonts,

            onDismiss = {

                exibirDialogoSoundFont = false

            },

            onApply = {

                viewModel.sincronizarBiblioteca()

                exibirDialogoSoundFont = false

            },

            onImport = {

                launcherSoundFont.launch(
                    arrayOf("*/*")
                )

            },

            podeExcluir = {

                viewModel.podeExcluirSoundFont(it)

            },

            onExcluir = {

                viewModel.excluirSoundFont(it)

            }

        )
        }

        if (exibirInstrumentPicker) {

            InstrumentPickerDialog(

                instrumentos = viewModel.listarInstrumentos(),

                onDismiss = {

                    exibirInstrumentPicker = false

                },

                onInstrumentSelected = { item ->

                    viewModel.setChannelInstrument(

                        canalSelecionado,

                        item.soundFont,

                        item.preset

                    )

                    exibirInstrumentPicker = false

                }

            )

        }
    //}
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
    volume: Float,
    isMuted: Boolean,
    isIndicatorOn: Boolean = false,
    onInstrumentClick: (() -> Unit)? = null,
    onVolumeChanged: (Float) -> Unit,
    onMuteChanged: (Boolean) -> Unit
) {
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
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable {
                        Log.d("InstrumentPicker", "Nome clicado")
                        onInstrumentClick?.invoke()

                    }
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (isMuted)
                                    MaterialTheme.colorScheme.error
                                else
                                    Color(0xFF505050)
                            )
                            .clickable {

                                onMuteChanged(!isMuted)

                            },
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "M",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )

                    }
                    Box(modifier = Modifier.size(8.dp).background(if (isIndicatorOn) Color(0xFF4CAF50) else Color.DarkGray, RoundedCornerShape(4.dp)))
                    Text("${volume.toInt()}%", color = corTextoVolume, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Slider(value = volume, onValueChange = onVolumeChanged, valueRange = 0f..100f, modifier = Modifier.fillMaxWidth(), colors = SliderDefaults.colors(thumbColor = corCaixaCanal, activeTrackColor = corCaixaCanal, inactiveTrackColor = Color(0xFF2C2C32)))
        }
    }
}

private fun percentToMidi(volume: Float): Int {
    return ((volume / 100f) * 127f)
        .toInt()
        .coerceIn(0,127)
}