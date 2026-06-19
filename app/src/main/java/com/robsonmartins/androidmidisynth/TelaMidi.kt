package com.robsonmartins.androidmidisynth

import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiReceiver
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

// Cores escuras customizadas
val ColorBgDark = Color(0xFF0F0F11)
val ColorCardBg = Color(0xFF1A1A1E)
val ColorChannel1 = Color(0xFF8A46E6)
val ColorChannel2 = Color(0xFFF27405)
val ColorChannel3 = Color(0xFF63C324)
val ColorChannel4 = Color(0xFF2589F5)
val ColorChannel5 = Color(0xFFFAB802)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaMidiSintetizador(
    viewModel: Any?,
    onVolumeChanged: (Float) -> Unit,
    onDispositivoSelecionado: (MidiDeviceInfo) -> Unit,
    midiReceiver: MidiReceiver? = null // Integrado o driver MIDI nativo com valor nulo padrão
) {
    var mostrarMonitor by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBgDark)
    ) {
        TopAppBar(
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (mostrarMonitor) {
                            IconButton(onClick = { mostrarMonitor = false }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                            }
                        } else {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = if (mostrarMonitor) "Atualização OTA" else "Cordovox MIDI",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Build, contentDescription = "Status", tint = Color(0xFF4CAF50))
                        Text(" 87%", color = Color.LightGray, fontSize = 14.sp)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorBgDark)
        )

        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            if (mostrarMonitor) {
                // Despacha o driver MIDI real para o monitor de gravação
                MonitorScreenContent(
                    fileUri = selectedFileUri,
                    midiReceiver = midiReceiver,
                    onFileSelected = { uri -> selectedFileUri = uri }
                )
            } else {
                MixerScreenContent(onOtaClick = { mostrarMonitor = true })
            }
        }
    }
}
