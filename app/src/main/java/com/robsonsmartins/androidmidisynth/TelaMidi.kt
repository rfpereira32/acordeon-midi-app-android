package com.robsonsmartins.androidmidisynth

import android.media.midi.MidiReceiver
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robsonsmartins.androidmidisynth.viewmodel.MainViewModel

val ColorBgDark = Color(0xFF0F0F11)
val ColorCardBg = Color(0xFF1A1A1E)
val ColorChannel1 = Color(0xFF8A46E6)
val ColorChannel2 = Color(0xFFF27405)
val ColorChannel3 = Color(0xFF63C324)
val ColorChannel4 = Color(0xFF2589F5)
val ColorChannel5 = Color(0xFFFAB802)

val cpuTelemetryFlow =
    kotlinx.coroutines.flow.MutableStateFlow(0f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaMidiSintetizador(
    midiReceiver: MidiReceiver? = null,
    instanciaMidiManager: MidiManager,
    viewModel: MainViewModel
) {

    var mostrarMonitor by remember {
        mutableStateOf(false)
    }

    var selectedFileUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val tituloDispositivo =
        MidiEstadoCompartilhado.nomeDispositivoPareado

    val ledVerdeAtivo =
        MidiEstadoCompartilhado.isDispositivoConectado

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ColorBgDark)
    ) {

        // =====================================================================
        // CABEÇALHO
        // =====================================================================

        TopAppBar(

            title = {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Row(
                        modifier =
                            Modifier.weight(1f),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        if (mostrarMonitor) {

                            IconButton(
                                onClick = {
                                    mostrarMonitor = false
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.ArrowBack,

                                    contentDescription =
                                        "Voltar",

                                    tint =
                                        Color.White
                                )

                            }

                        }

                        Spacer(
                            modifier =
                                Modifier.width(
                                    if (mostrarMonitor)
                                        8.dp
                                    else
                                        0.dp
                                )
                        )

                        Text(
                            text =
                                tituloDispositivo,

                            style =
                                MaterialTheme
                                    .typography
                                    .headlineMedium,

                            fontSize =
                                18.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                Color.White,

                            maxLines =
                                1
                        )

                    }

                }

            },

            colors =
                TopAppBarDefaults
                    .topAppBarColors(
                        containerColor =
                            ColorBgDark
                    )
        )

        // =====================================================================
        // CONTEÚDO
        // =====================================================================

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 16.dp
                    )
        ) {

            if (mostrarMonitor) {

                MonitorScreenContent(

                    fileUri =
                        selectedFileUri,

                    midiReceiver =
                        midiReceiver,

                    onFileSelected = {
                            uri ->
                        selectedFileUri =
                            uri
                    }

                )

            } else {

                MixerScreenContent(

                    nomeInstrumento =
                        tituloDispositivo,

                    isConnected =
                        ledVerdeAtivo,

                    onOtaClick = {
                        mostrarMonitor = true
                    },

                    midiManager =
                        instanciaMidiManager,

                    viewModel =
                        viewModel

                )

            }

        }

    }

}