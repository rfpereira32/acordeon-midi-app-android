package com.robsonsmartins.androidmidisynth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiReceiver
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ColorBgDark = Color(0xFF0F0F11)
val ColorCardBg = Color(0xFF1A1A1E)
val ColorChannel1 = Color(0xFF8A46E6)
val ColorChannel2 = Color(0xFFF27405)
val ColorChannel3 = Color(0xFF63C324)
val ColorChannel4 = Color(0xFF2589F5)
val ColorChannel5 = Color(0xFFFAB802)

val cpuTelemetryFlow = kotlinx.coroutines.flow.MutableStateFlow(0f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaMidiSintetizador(
    listaDispositivos: List<MidiDeviceInfo>,
    onVolumeChanged: (Float) -> Unit,
    onDispositivoSelecionado: (MidiDeviceInfo) -> Unit,
    midiReceiver: MidiReceiver? = null,
    instanciaMidiManager: MidiManager
) {
    val context = LocalContext.current
    var mostrarMonitor by remember { mutableStateOf(false) }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var exibirPopupDispositivos by remember { mutableStateOf(false) }

    val tituloDispositivo = MidiEstadoCompartilhado.nomeDispositivoPareado
    val ledVerdeAtivo = MidiEstadoCompartilhado.isDispositivoConectado

    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == android.app.Activity.RESULT_OK) {
            Toast.makeText(context, "Bluetooth ativado!", Toast.LENGTH_SHORT).show()
        }
    }

    val launcherPermissoes = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissoes ->
        val scanConcedido = permissoes[android.Manifest.permission.BLUETOOTH_SCAN] ?: false
        val connectConcedido = permissoes[android.Manifest.permission.BLUETOOTH_CONNECT] ?: false
        if (scanConcedido && connectConcedido) {
            exibirPopupDispositivos = true
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(ColorBgDark)
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
                            IconButton(onClick = { exibirPopupDispositivos = true }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = tituloDispositivo, style = MaterialTheme.typography.headlineMedium, fontSize = if (tituloDispositivo == "Nenhum dispositivo pareado") 16.sp else 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(if (ledVerdeAtivo) Color(0xFF4CAF50) else Color.Red))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(" 87%", color = Color.LightGray, fontSize = 14.sp)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ColorBgDark)
        )

        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            if (mostrarMonitor) {
                MonitorScreenContent(fileUri = selectedFileUri, midiReceiver = midiReceiver, onFileSelected = { uri -> selectedFileUri = uri })
            } else {
                MixerScreenContent(nomeInstrumento = tituloDispositivo, isConnected = ledVerdeAtivo, onOtaClick = { mostrarMonitor = true }, midiManager = instanciaMidiManager)
            }
        }
    }
    if (exibirPopupDispositivos) {
        val midiManager = context.getSystemService(Context.MIDI_SERVICE) as android.media.midi.MidiManager
        var listaAtualizada by remember { mutableStateOf(listaDispositivos) }

        LaunchedEffect(exibirPopupDispositivos) {
            try {
                listaAtualizada = midiManager.devices.toList()
            } catch (_: Exception) {}
        }

        AlertDialog(
            onDismissRequest = { exibirPopupDispositivos = false },
            title = { Text("Selecione o Acordeon", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = ColorCardBg,
            text = {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (listaAtualizada.isNotEmpty()) {
                        Text("Instrumentos Disponíveis:", color = Color.Gray, fontSize = 12.sp)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listaAtualizada.forEach { dispositivo ->
                                val nomeDispositivo = dispositivo.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Acordeon MIDI"
                                Button(
                                    onClick = {
                                        exibirPopupDispositivos = false
                                        onDispositivoSelecionado(dispositivo)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorChannel1),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Default.Build, contentDescription = null, tint = Color.White)
                                        Text(text = nomeDispositivo)
                                    }
                                }
                            }
                        }
                        Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    Button(
                        onClick = {
                            launcherPermissoes.launch(arrayOf(android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.ACCESS_FINE_LOCATION))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorChannel2),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Buscar Instrumentos no Ar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { exibirPopupDispositivos = false }) { Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Bold) }
            }
        )
    }
}
