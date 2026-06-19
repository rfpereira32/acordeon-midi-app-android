package com.robsonmartins.androidmidisynth

import android.media.midi.MidiDeviceInfo
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
    onDispositivoSelecionado: (MidiDeviceInfo) -> Unit // Ajustado para o tipo correto exigido
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = ColorBgDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Cordovox MIDI", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Build, contentDescription = "Status", tint = Color(0xFF4CAF50))
                            Text(" 87%", color = Color.LightGray, fontSize = 14.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorBgDark)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = ColorCardBg, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                    label = { Text("Início") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Monitor") },
                    label = { Text("Monitor") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedTab == 0) {
                MixerScreenContent()
            } else {
                MonitorScreenContent()
            }
        }
    }
}
