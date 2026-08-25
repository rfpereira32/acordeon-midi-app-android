package com.robsonsmartins.androidmidisynth

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.robsonsmartins.androidmidisynth.viewmodel.MainViewModel
import com.robsonsmartins.androidmidisynth.soundfont.SoundFontInfo
import com.robsonsmartins.androidmidisynth.ui.components.InstrumentPickerDialog

private enum class TelaMixer {

    MIXER,
    SOUNDFONTS,
    PRESETS,
    CONFIG

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixerScreenContent(
    nomeInstrumento: String,
    isConnected: Boolean,
    onOtaClick: () -> Unit,
    midiManager: MidiManager,
    viewModel: MainViewModel
) {

    var telaAtual by remember {
        mutableStateOf(TelaMixer.MIXER)
    }

    var modoSetupOtaAtivado by remember {
        mutableStateOf(false)
    }

    var canal1ComoMaster by remember {
        mutableStateOf(
            viewModel.isChannel1AsMaster()
        )
    }

    var exibirInstrumentPicker by remember {
        mutableStateOf(false)
    }

    var exibirControlSourcePicker by remember {
        mutableStateOf(false)
    }

    var canalSelecionado by remember {
        mutableIntStateOf(0)
    }

    val context =
        LocalContext.current

    val launcherSoundFont =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            uri?.let {

                if (!viewModel.importarSoundFont(it)) {

                    Toast.makeText(
                        context,
                        "Não foi possível importar. Selecione uma SoundFont .sf2 válida que ainda não esteja na lista.",
                        Toast.LENGTH_SHORT
                    ).show()

                }

            }

        }

    val canais = viewModel.getChannels()

    /*
     * Atualiza o estado visual do Master caso ele tenha
     * sido restaurado pela sessão.
     */
    LaunchedEffect(
        viewModel.isChannel1AsMaster()
    ) {

        canal1ComoMaster =
            viewModel.isChannel1AsMaster()

    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // =====================================================================
        // CABEÇALHO
        // =====================================================================

        val conectado =
            MidiEstadoCompartilhado.isDispositivoConectado

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

                    MidiEstadoCompartilhado
                        .percentualBateria > 60 ->
                        Color(0xFF4CAF50)

                    MidiEstadoCompartilhado
                        .percentualBateria > 30 ->
                        Color(0xFFFFC107)

                    MidiEstadoCompartilhado
                        .percentualBateria > 15 ->
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
                    top = 0.dp,
                    bottom = 2.dp
                ),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    painter =
                        painterResource(
                            R.drawable.ic_bluetooth
                        ),
                    contentDescription =
                        "Bluetooth",
                    tint = corBluetooth
                )

                Spacer(
                    modifier =
                        Modifier.width(6.dp)
                )

                Text(
                    text = "BLE",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    fontWeight =
                        FontWeight.Medium
                )

            }

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        painter =
                            painterResource(
                                R.drawable.ic_battery
                            ),
                        contentDescription =
                            "Bateria",
                        tint = corBateria
                    )

                    Spacer(
                        modifier =
                            Modifier.width(4.dp)
                    )

                    Text(
                        text = textoPercentual,
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                }

                Spacer(
                    modifier =
                        Modifier.width(12.dp)
                )

                Text(
                    text = textoTensao,
                    color = Color.Gray,
                    fontSize = 13.sp
                )

            }

        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        // =====================================================================
        // CONTEÚDO PRINCIPAL
        // =====================================================================

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {

            when (telaAtual) {

                // =================================================================
                // MIXER
                // =================================================================

                TelaMixer.MIXER -> {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(
                                rememberScrollState()
                            )
                            .padding(
                                bottom = 8.dp
                            )
                    ) {

                        Column(
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {

                            canais.forEachIndexed {
                                    index,
                                    canal ->

                                StaticChannelRow(
                                    number =
                                        (index + 1)
                                            .toString(),

                                    name =
                                        nomePadraoCanal(
                                            index,
                                            canal.preset?.nome
                                        ),

                                    accentColor =
                                        if (index < 3)
                                            ColorChannel1
                                        else
                                            ColorChannel4,

                                    volume =
                                        canal.volume *
                                                100f / 127f,

                                    isMuted =
                                        canal.muted,

                                    isIndicatorOn = canal.led,

                                    sliderEnabled =
                                        if (index == 0)
                                            true
                                        else
                                            !canal1ComoMaster,

                                    showMasterButton =
                                        index == 0,

                                    isMaster =
                                        if (index == 0)
                                            canal1ComoMaster
                                        else
                                            false,

                                    onMasterChanged = {
                                            ativado ->

                                        canal1ComoMaster =
                                            ativado

                                        viewModel
                                            .setChannel1AsMaster(
                                                ativado
                                            )

                                    },

                                    onControlSourceClick = {

                                        canalSelecionado =
                                            index

                                        exibirControlSourcePicker =
                                            true

                                    },

                                    onInstrumentClick = {

                                        canalSelecionado =
                                            index

                                        exibirInstrumentPicker =
                                            true

                                    },

                                    onVolumeChanged = {
                                            novoVol ->

                                        val valorMidi =
                                            percentToMidi(
                                                novoVol
                                            )

                                        viewModel
                                            .setChannelVolume(
                                                index,
                                                valorMidi
                                            )

                                    },

                                    onMuteChanged = {
                                            mute ->

                                        viewModel
                                            .setChannelMute(
                                                index,
                                                mute
                                            )

                                    }

                                )

                            }

                        }

                    }

                }

                // =================================================================
                // SOUNDFONTS
                // =================================================================

                TelaMixer.SOUNDFONTS -> {

                    SoundFontsScreenContent(
                        viewModel = viewModel,
                        onImport = {
                            launcherSoundFont.launch(
                                arrayOf("*/*")
                            )
                        }
                    )

                }

                // =================================================================
                // PRESETS
                // =================================================================

                TelaMixer.PRESETS -> {

                    PresetsScreenContent(
                        viewModel = viewModel,
                        onPresetCarregado = {
                            telaAtual = TelaMixer.MIXER
                        }
                    )

                }

                // =================================================================
                // CONFIGURAÇÕES
                // =================================================================

                TelaMixer.CONFIG -> {

                    ConfigScreenContent(
                        nomeInstrumento =
                            nomeInstrumento,

                        isConnected =
                            isConnected,

                        midiManager =
                            midiManager,

                        viewModel =
                            viewModel,

                        modoSetupOtaAtivado =
                            modoSetupOtaAtivado,

                        onModoSetupOtaChanged = {
                            modoSetupOtaAtivado =
                                it
                        }
                    )

                }

            }

        }

        // =====================================================================
        // NAVEGAÇÃO INFERIOR
        // =====================================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    top = 4.dp,
                    bottom = 8.dp,
                    start = 4.dp,
                    end = 4.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            BotaoNavegacao(
                texto = "Mixer",
                icone = Icons.Default.Menu,
                selecionado =
                    telaAtual ==
                            TelaMixer.MIXER,
                onClick = {

                    telaAtual =
                        TelaMixer.MIXER

                },
                modifier =
                    Modifier.weight(1f)
            )

            BotaoNavegacao(
                texto = "SoundFont",
                icone = Icons.Default.PlayArrow,
                selecionado =
                    telaAtual ==
                            TelaMixer.SOUNDFONTS,
                onClick = {

                    telaAtual =
                        TelaMixer.SOUNDFONTS

                },
                modifier =
                    Modifier.weight(1f)
            )

            BotaoNavegacao(
                texto = "Presets",
                icone = Icons.Default.Star,
                selecionado =
                    telaAtual ==
                            TelaMixer.PRESETS,
                onClick = {

                    telaAtual =
                        TelaMixer.PRESETS

                },
                modifier =
                    Modifier.weight(1f)
            )

            BotaoNavegacao(
                texto = "Config",
                icone = Icons.Default.Settings,
                selecionado =
                    telaAtual ==
                            TelaMixer.CONFIG,
                onClick = {

                    telaAtual =
                        TelaMixer.CONFIG

                },
                modifier =
                    Modifier.weight(1f)
            )

        }

    }

    // ========================================================================
    // PICKER DE INSTRUMENTOS
    // ========================================================================

    if (exibirInstrumentPicker) {

        InstrumentPickerDialog(

            instrumentos =
                viewModel.listarInstrumentos(),

            onDismiss = {

                exibirInstrumentPicker =
                    false

            },

            onInstrumentSelected = { item ->

                viewModel.setChannelInstrument(

                    canalSelecionado,

                    item.soundFont,

                    item.preset

                )

                exibirInstrumentPicker =
                    false

            }

        )

    }

    // ========================================================================
    // PICKER DO CONTROLE DO ACORDEÃO
    // ========================================================================

    if (exibirControlSourcePicker) {

        ControlSourceDialog(

            canal =
                canalSelecionado,

            controlSource =
                viewModel.getChannelControlSource(
                    canalSelecionado
                ),

            onDismiss = {

                exibirControlSourcePicker =
                    false

            },

            onControlSelected = { controlSource ->

                viewModel.setChannelControlSource(
                    canalSelecionado,
                    controlSource
                )

                exibirControlSourcePicker =
                    false

            }

        )

    }

}


// =============================================================================
// NOME PADRÃO DOS CANAIS
// =============================================================================

private fun nomePadraoCanal(
    index: Int,
    nomePreset: String?
): String {

    if (!nomePreset.isNullOrBlank()) {
        return nomePreset
    }

    return "Canal MIDI ${index + 1}"
}


// =============================================================================
// BOTÃO DE NAVEGAÇÃO
// =============================================================================

@Composable
private fun BotaoNavegacao(
    texto: String,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    selecionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier
            .height(58.dp)
            .clickable {
                onClick()
            },

        colors = CardDefaults.cardColors(
            containerColor =
                if (selecionado)
                    Color(0xFF303038)
                else
                    ColorCardBg
        ),

        border =
            if (selecionado)
                androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFF5A5A64)
                )
            else
                null,

        shape =
            RoundedCornerShape(8.dp)
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize(),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {

            Icon(
                imageVector = icone,
                contentDescription = texto,
                tint =
                    if (selecionado)
                        Color.White
                    else
                        Color.LightGray,
                modifier =
                    Modifier.size(20.dp)
            )

            Spacer(
                modifier =
                    Modifier.height(3.dp)
            )

            Text(
                text = texto,
                color =
                    if (selecionado)
                        Color.White
                    else
                        Color.Gray,
                fontSize = 10.sp,
                fontWeight =
                    if (selecionado)
                        FontWeight.Bold
                    else
                        FontWeight.Normal
            )

        }

    }

}


// =============================================================================
// DIÁLOGO DE SELEÇÃO DO CONTROLE DO ACORDEÃO
// =============================================================================

@Composable
private fun ControlSourceDialog(
    canal: Int,
    controlSource: Int,
    onDismiss: () -> Unit,
    onControlSelected: (Int) -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,

        containerColor = ColorCardBg,

        title = {

            Text(
                text =
                    "Controle do canal ${canal + 1}",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

        },

        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {

                (1..4).forEach { controle ->

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {

                                    onControlSelected(
                                        controle
                                    )

                                }
                                .padding(
                                    vertical = 8.dp,
                                    horizontal = 4.dp
                                ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        RadioButton(
                            selected =
                                controlSource ==
                                        controle,

                            onClick = {

                                onControlSelected(
                                    controle
                                )

                            },

                            colors =
                                RadioButtonDefaults.colors(
                                    selectedColor =
                                        ColorChannel2,
                                    unselectedColor =
                                        Color.Gray
                                )
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(
                            text =
                                when (controle) {
                                    1 -> "Teclado"
                                    2 -> "Baixos fundamentais"
                                    3 -> "Acordes"
                                    4 -> "Baixos + acordes"
                                    else -> "Controle $controle"
                                },
                            color =
                                Color.White,
                            fontSize = 15.sp
                        )
                    }

                }

            }

        },

        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text(
                    text = "Cancelar",
                    color = Color.LightGray
                )

            }

        }

    )

}


// =============================================================================
// TELA DE SOUNDFONTS
// =============================================================================

@Composable
private fun SoundFontsScreenContent(
    viewModel: MainViewModel,
    onImport: () -> Unit
) {

    val soundFonts =
        viewModel.listarSoundFonts()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                start = 8.dp,
                end = 8.dp,
                bottom = 12.dp
            )
    ) {

        // =====================================================================
        // CABEÇALHO
        // =====================================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Column {

                Text(
                    text = "SoundFonts",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        "${soundFonts.size} SoundFont(s)",
                    color = Color.Gray,
                    fontSize = 12.sp
                )

            }

            Button(
                onClick = onImport,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF3A3A42)
                    ),
                shape =
                    RoundedCornerShape(8.dp),
                contentPadding =
                    PaddingValues(
                        horizontal = 12.dp
                    )
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Add,
                    contentDescription =
                        "Importar SoundFont",
                    modifier =
                        Modifier.size(18.dp)
                )

                Spacer(
                    modifier =
                        Modifier.width(4.dp)
                )

                Text(
                    "Importar",
                    fontSize = 12.sp
                )

            }

        }

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        if (soundFonts.isEmpty()) {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            ColorCardBg
                    )
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.PlayArrow,
                        contentDescription =
                            null,
                        tint = Color.Gray,
                        modifier =
                            Modifier.size(40.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            "Nenhuma SoundFont encontrada",
                        color =
                            Color.LightGray,
                        fontSize = 14.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "Toque em Importar para adicionar uma SoundFont.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign =
                            TextAlign.Center
                    )

                }

            }

        } else {

            soundFonts.forEach { soundFont ->

                SoundFontListItem(
                    soundFont = soundFont,
                    viewModel = viewModel
                )

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

            }

        }

    }

}


// =============================================================================
// ITEM DE SOUNDFONT
// =============================================================================

@Composable
private fun SoundFontListItem(
    soundFont: SoundFontInfo,
    viewModel: MainViewModel
) {

    val carregada =
        soundFont.carregada

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    ColorCardBg
            ),
        shape =
            RoundedCornerShape(8.dp)
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 4.dp,
                        end = 4.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Checkbox(
                checked = carregada,

                onCheckedChange = {

                    viewModel
                        .alternarSoundFont(
                            soundFont.id
                        )

                }
            )

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable {

                            viewModel
                                .alternarSoundFont(
                                    soundFont.id
                                )

                        }
                        .padding(
                            vertical = 10.dp
                        )
            ) {

                Text(
                    text =
                        soundFont.nome,
                    color =
                        Color.White,
                    fontSize = 15.sp,
                    fontWeight =
                        FontWeight.Medium
                )

                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )

                Text(
                    text =
                        if (
                            soundFont.quantidadePresets > 0
                        ) {

                            "${soundFont.quantidadePresets} presets"

                        } else {

                            "Nenhum preset carregado"

                        },

                    color =
                        Color.Gray,

                    fontSize = 11.sp
                )

            }

            if (
                viewModel.podeExcluirSoundFont(
                    soundFont.id
                )
            ) {

                IconButton(
                    onClick = {

                        viewModel.excluirSoundFont(
                            soundFont.id
                        )

                    }
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Delete,

                        contentDescription =
                            "Excluir SoundFont",

                        tint =
                            Color(0xFF77777F),

                        modifier =
                            Modifier.size(20.dp)
                    )

                }

            }

        }

    }

}

// =============================================================================
// PRESETS
// =============================================================================

@Composable
private fun PresetsScreenContent(
    viewModel: MainViewModel,
    onPresetCarregado: () -> Unit
) {

    var mostrarDialogoSalvar by remember {
        mutableStateOf(false)
    }

    var nomePreset by remember {
        mutableStateOf("")
    }

    val context =
        LocalContext.current

    var atualizarListaPresets by remember {
        mutableIntStateOf(0)
    }

    val launcherExportarBackup =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.CreateDocument(
                    "application/json"
                )
        ) { uri: Uri? ->

            uri?.let {

                val sucesso =
                    viewModel.exportarBackup(
                        it
                    )

                Toast.makeText(
                    context,
                    if (sucesso)
                        "Backup de presets exportado."
                    else
                        "Não foi possível exportar o backup.",
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

    val launcherImportarBackup =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            uri?.let {

                val sucesso =
                    viewModel.importarBackup(
                        it
                    )

                if (sucesso) {

                    atualizarListaPresets++

                }

                Toast.makeText(
                    context,
                    if (sucesso)
                        "Backup de presets importado."
                    else
                        "Não foi possível importar o backup.",
                    Toast.LENGTH_SHORT
                ).show()

            }

        }

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    val coroutineScope =
        rememberCoroutineScope()

    LaunchedEffect(Unit) {

        snapshotFlow {
            viewModel.erroPreset
        }.collect { mensagem ->

            if (mensagem != null) {

                viewModel.limparErroPreset()

                snackbarHostState.showSnackbar(
                    mensagem
                )
            }
        }
    }

    val presets =
        remember(
            atualizarListaPresets
        ) {
            viewModel.listarPresets()
        }

    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(
                        start = 8.dp,
                        end = 8.dp,
                        bottom = 12.dp
                    )
        ) {

        // =====================================================================
        // TÍTULO
        // =====================================================================

        Text(
            text = "Presets",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(
            text =
                "Salve e carregue configurações completas do acordeão.",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        // =====================================================================
        // SALVAR PRESET
        // =====================================================================

        Button(
            onClick = {

                nomePreset = ""

                mostrarDialogoSalvar = true

            },

            modifier =
                Modifier.fillMaxWidth(),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        Color(0xFF3A3A42)
                ),

            shape =
                RoundedCornerShape(8.dp)
        ) {

            Icon(
                imageVector =
                    Icons.Default.Add,

                contentDescription =
                    null,

                modifier =
                    Modifier.size(20.dp)
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Text(
                text = "Salvar preset"
            )

        }

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        // =====================================================================
        // BACKUP
        // =====================================================================

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            OutlinedButton(
                onClick = {

                    launcherExportarBackup.launch(
                        "AcordeonMidi_Presets_Backup.json"
                    )

                },

                modifier =
                    Modifier.weight(1f),

                shape =
                    RoundedCornerShape(8.dp)
            ) {

                Text(
                    text = "Exportar backup"
                )

            }

            OutlinedButton(
                onClick = {

                    launcherImportarBackup.launch(
                        arrayOf(
                            "application/json",
                            "text/json",
                            "*/*"
                        )
                    )

                },

                modifier =
                    Modifier.weight(1f),

                shape =
                    RoundedCornerShape(8.dp)
            ) {

                Text(
                    text = "Importar backup"
                )

            }

        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        // =====================================================================
        // LISTA
        // =====================================================================

        if (presets.isEmpty()) {

            Card(
                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            ColorCardBg
                    ),

                shape =
                    RoundedCornerShape(10.dp)
            ) {

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Star,

                        contentDescription =
                            null,

                        tint =
                            Color.Gray,

                        modifier =
                            Modifier.size(36.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Nenhum preset salvo",

                        color =
                            Color.LightGray,

                        fontSize = 14.sp
                    )

                }

            }

        } else {

            presets.forEach { preset ->

                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {

                                val carregado =
                                    viewModel.carregarPreset(
                                        preset.nome
                                    )

                                if (carregado) {

                                    onPresetCarregado()

                                }

                            },

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                ColorCardBg
                        ),

                    shape =
                        RoundedCornerShape(8.dp)
                ) {

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 14.dp,
                                    end = 4.dp
                                ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            modifier =
                                Modifier.weight(1f)
                                    .padding(
                                        vertical = 12.dp
                                    )
                        ) {

                            Text(
                                text =
                                    preset.nome,

                                color =
                                    Color.White,

                                fontSize = 15.sp,

                                fontWeight =
                                    FontWeight.Medium
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(3.dp)
                            )

                            Text(
                                text =
                                    "${preset.mixer.channels.size} canais",

                                color =
                                    Color.Gray,

                                fontSize = 11.sp
                            )

                        }

                        IconButton(
                            onClick = {

                                val excluido =
                                    viewModel.excluirPreset(
                                        preset.nome
                                    )

                                if (excluido) {
                                    atualizarListaPresets++
                                }

                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Delete,

                                contentDescription =
                                    "Excluir preset",

                                tint =
                                    Color(0xFF77777F),

                                modifier =
                                    Modifier.size(20.dp)
                            )

                        }

                    }

                }

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

            }

        }
    }
    SnackbarHost(
        hostState =
            snackbarHostState,

        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = 16.dp
                )
    )
    }

    // =========================================================================
    // DIÁLOGO PARA SALVAR
    // =========================================================================

    if (mostrarDialogoSalvar) {

        AlertDialog(

            onDismissRequest = {

                mostrarDialogoSalvar = false

            },

            title = {

                Text(
                    text = "Salvar preset"
                )

            },

            text = {

                OutlinedTextField(

                    value = nomePreset,

                    onValueChange = {

                        nomePreset = it

                    },

                    label = {

                        Text(
                            text = "Nome do preset"
                        )

                    },

                    singleLine = true

                )

            },

            confirmButton = {

                TextButton(

                    onClick = {

                        val nome =
                            nomePreset.trim()

                        if (nome.isNotEmpty()) {

                            viewModel.salvarPreset(
                                nome
                            )

                            mostrarDialogoSalvar = false

                        }

                    }

                ) {

                    Text(
                        text = "Salvar"
                    )

                }

            },

            dismissButton = {

                TextButton(

                    onClick = {

                        mostrarDialogoSalvar = false

                    }

                ) {

                    Text(
                        text = "Cancelar"
                    )

                }

            }

        )

    }

}


// =============================================================================
// CONFIGURAÇÕES
// =============================================================================

@Composable
private fun ConfigScreenContent(
    nomeInstrumento: String,
    isConnected: Boolean,
    midiManager: MidiManager,
    viewModel: MainViewModel,
    modoSetupOtaAtivado: Boolean,
    onModoSetupOtaChanged: (Boolean) -> Unit
) {

    var menuQuantidadeCanaisAberto by remember {
        mutableStateOf(false)
    }

    val quantidadeCanais =
        viewModel.getNumberOfChannels()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 12.dp
                )
    ) {

        Text(
            text =
                "Configurações",
            color =
                Color.White,
            fontSize = 20.sp,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Text(
            text =
                "Configurações e manutenção do acordeão",
            color =
                Color.Gray,
            fontSize = 12.sp
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        // =====================================================================
        // QUANTIDADE DE CANAIS
        // =====================================================================

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        ColorCardBg
                ),
            shape =
                RoundedCornerShape(10.dp)
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),

                verticalAlignment =
                    Alignment.CenterVertically,

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "Quantidade de canais",
                        color =
                            Color.White,
                        fontSize = 16.sp,
                        fontWeight =
                            FontWeight.Medium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )

                    Text(
                        text =
                            "Escolha de 5 a 16 canais MIDI",
                        color =
                            Color.Gray,
                        fontSize = 11.sp
                    )

                }

                Box {

                    Button(
                        onClick = {

                            menuQuantidadeCanaisAberto =
                                true

                        },

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    Color(0xFF3A3A42)
                            ),

                        shape =
                            RoundedCornerShape(8.dp),

                        contentPadding =
                            PaddingValues(
                                horizontal = 14.dp
                            )
                    ) {

                        Text(
                            text =
                                "$quantidadeCanais canais",
                            fontSize = 12.sp
                        )

                    }

                    DropdownMenu(
                        expanded =
                            menuQuantidadeCanaisAberto,

                        onDismissRequest = {

                            menuQuantidadeCanaisAberto =
                                false

                        }
                    ) {

                        (5..16).forEach { quantidade ->

                            DropdownMenuItem(

                                text = {

                                    Text(
                                        text =
                                            "$quantidade canais"
                                    )

                                },

                                onClick = {

                                    viewModel
                                        .setNumberOfChannels(
                                            quantidade
                                        )

                                    menuQuantidadeCanaisAberto =
                                        false

                                },

                                trailingIcon = {

                                    if (
                                        quantidade ==
                                        quantidadeCanais
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Default.Check,
                                            contentDescription =
                                                null
                                        )

                                    }

                                }

                            )

                        }

                    }

                }

            }

        }

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        // =====================================================================
        // OTA
        // =====================================================================

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        ColorCardBg
                ),
            shape =
                RoundedCornerShape(10.dp)
        ) {

            Column(
                modifier =
                    Modifier.padding(14.dp)
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Refresh,
                        contentDescription =
                            null,
                        tint =
                            ColorChannel2,
                        modifier =
                            Modifier.size(26.dp)
                    )

                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )

                    Column {

                        Text(
                            text =
                                "OTA Update",
                            color =
                                Color.White,
                            fontSize = 16.sp,
                            fontWeight =
                                FontWeight.Medium
                        )

                        Text(
                            text =
                                "Atualização do firmware via Wi-Fi AP",
                            color =
                                Color.Gray,
                            fontSize = 11.sp
                        )

                    }

                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                if (!modoSetupOtaAtivado) {

                    Button(
                        onClick = {

                            onModoSetupOtaChanged(
                                true
                            )

                            midiManager
                                .enviarComandoIniciarOtaWifi()

                        },

                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    Color(0xFFFF1744)
                            ),

                        shape =
                            RoundedCornerShape(8.dp),

                        enabled =
                            isConnected,

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                    ) {

                        Text(
                            "Iniciar OTA via Wi-Fi AP",
                            color =
                                Color.White,
                            fontWeight =
                                FontWeight.Bold
                        )

                    }

                    if (!isConnected) {

                        Text(
                            text =
                                "Conecte o acordeão via BLE para iniciar o modo OTA.",

                            color =
                                Color.Gray,

                            fontSize = 11.sp,

                            textAlign =
                                TextAlign.Center,

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = 8.dp
                                    )
                        )

                    }

                } else {

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFF2A1A1A),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    Color(0xFFFF1744)
                                        .copy(
                                            alpha = 0.5f
                                        ),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),

                        horizontalAlignment =
                            Alignment.CenterHorizontally,

                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Text(
                            "⚠️ SINAL ENVIADO COM SUCESSO!",
                            color =
                                Color(0xFFFF1744),
                            fontWeight =
                                FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        Text(
                            "O rádio BLE foi suspenso. Conecte seu celular ou PC no Access Point gerado pelo acordeão:",
                            color =
                                Color.LightGray,
                            fontSize = 12.sp,
                            textAlign =
                                TextAlign.Center
                        )

                        Text(
                            "SSID: ${
                                nomeInstrumento.ifEmpty {
                                    "Acordeon_MIDI_AP"
                                }
                            }",
                            color =
                                Color(0xFF4CAF50),
                            fontWeight =
                                FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Text(
                            "Abra o navegador e acesse o IP para carregar o binário (.bin):",
                            color =
                                Color.LightGray,
                            fontSize = 12.sp,
                            textAlign =
                                TextAlign.Center
                        )

                        Surface(
                            color =
                                Color.Black,
                            shape =
                                RoundedCornerShape(4.dp),
                            modifier =
                                Modifier.padding(
                                    vertical = 4.dp
                                )
                        ) {

                            Text(
                                text =
                                    "http://192.168.4.1",
                                color =
                                    Color(0xFF00E5FF),
                                fontWeight =
                                    FontWeight.Bold,
                                modifier =
                                    Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                fontSize = 15.sp
                            )

                        }

                        Button(
                            onClick = {

                                onModoSetupOtaChanged(
                                    false
                                )

                            },

                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        Color(0xFF424242)
                                ),

                            shape =
                                RoundedCornerShape(6.dp),

                            modifier =
                                Modifier.height(32.dp)
                        ) {

                            Text(
                                "Voltar",
                                color =
                                    Color.White,
                                fontSize = 12.sp
                            )

                        }

                    }

                }

            }

        }

    }

}


// =============================================================================
// TELA DE MONITOR / OTA ANTIGA
// =============================================================================

@Composable
fun MonitorScreenContent(
    fileUri: Uri?,
    midiReceiver:
    android.media.midi.MidiReceiver?,
    onFileSelected: (Uri) -> Unit
) {

    val context =
        LocalContext.current

    val otaManager =
        remember(
            MidiEstadoCompartilhado
                .receiverMidiAtivo
        ) {

            OtaManager(
                context,
                MidiEstadoCompartilhado
                    .receiverMidiAtivo
            )

        }

    val statusAtual by
    otaManager.statusOta
        .collectAsState()

    val progressoPercentual by
    otaManager.progressoOta
        .collectAsState()

    val estaAtualizando by
    otaManager.estaAtualizando
        .collectAsState()

    val coroutineScope =
        rememberCoroutineScope()

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            if (uri != null)
                onFileSelected(uri)

        }

    LaunchedEffect(
        fileUri,
        estaAtualizando
    ) {

        if (
            fileUri != null &&
            !estaAtualizando
        ) {

            otaManager.statusOta.value =
                "Pronto para enviar: ${
                    fileUri.lastPathSegment
                        ?: "firmware.bin"
                }"

        }

    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    top = 16.dp
                ),

        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {

        Text(
            "Atualização de Sistema (OTA)",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight =
                FontWeight.Bold
        )

        Card(
            modifier =
                Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        ColorCardBg
                )
        ) {

            Column(
                modifier =
                    Modifier.padding(16.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Refresh,
                    contentDescription =
                        null,

                    tint =
                        if (estaAtualizando)
                            ColorChannel2
                        else
                            Color.DarkGray,

                    modifier =
                        Modifier.size(40.dp)
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                Text(
                    text =
                        statusAtual,
                    color =
                        Color.LightGray,
                    fontSize = 14.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                LinearProgressIndicator(
                    progress =
                        progressoPercentual,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp),

                    color =
                        ColorChannel3,

                    trackColor =
                        Color(0xFF2C2C32)
                )

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )

                Text(
                    text =
                        "${(
                                progressoPercentual * 100
                                ).toInt()}%",

                    color =
                        ColorChannel3,

                    fontSize = 12.sp,

                    fontWeight =
                        FontWeight.Bold
                )

            }

        }

        Button(
            onClick = {

                filePickerLauncher.launch(
                    "*/*"
                )

            },

            enabled =
                !estaAtualizando,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(50.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        ColorCardBg
                ),

            shape =
                RoundedCornerShape(8.dp)
        ) {

            Icon(
                Icons.Default.Menu,
                contentDescription =
                    null,
                tint =
                    Color.White
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Text(
                "Escolher Arquivo .bin",
                color =
                    Color.White,
                fontWeight =
                    FontWeight.Bold
            )

        }

        Button(
            onClick = {

                fileUri?.let { uri ->

                    coroutineScope.launch {

                        otaManager
                            .iniciarAtualizacao(
                                uri
                            )

                    }

                }

            },

            enabled =
                fileUri != null &&
                        !estaAtualizando,

            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(50.dp),

            colors =
                ButtonDefaults.buttonColors(
                    containerColor =
                        ColorChannel1
                ),

            shape =
                RoundedCornerShape(8.dp)
        ) {

            Icon(
                Icons.Default.PlayArrow,
                contentDescription =
                    null,
                tint =
                    Color.White,
                modifier =
                    Modifier.size(20.dp)
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Text(
                "Enviar Novo Firmware via BLE",
                color =
                    Color.White,
                fontWeight =
                    FontWeight.Bold
            )

        }

    }

}


// =============================================================================
// LINHA DE CANAL
// =============================================================================

@Composable
fun StaticChannelRow(
    number: String,
    name: String,
    accentColor: Color,
    volume: Float,
    isMuted: Boolean,
    isIndicatorOn: Boolean = false,
    sliderEnabled: Boolean = true,

    showMasterButton: Boolean = false,

    isMaster: Boolean = false,

    onMasterChanged:
        (Boolean) -> Unit = {},

    onControlSourceClick:
        () -> Unit = {},

    onInstrumentClick:
    (() -> Unit)? = null,

    onVolumeChanged:
        (Float) -> Unit,

    onMuteChanged:
        (Boolean) -> Unit
) {

    val corFundoLinha =
        if (isMuted)
            Color(0xFF252528)
        else
            ColorCardBg

    val corCaixaCanal =
        if (isMuted)
            Color.Gray
        else
            accentColor

    val corTextoVolume =
        if (isMuted)
            Color.LightGray
        else
            accentColor

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(
                    corFundoLinha,
                    RoundedCornerShape(8.dp)
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .width(34.dp)
                    .background(
                        corCaixaCanal,
                        RoundedCornerShape(
                            topStart = 8.dp,
                            bottomStart = 8.dp
                        )
                    )
                    .clickable {

                        onControlSourceClick()

                    },

            contentAlignment =
                Alignment.Center
        ) {

            Text(
                number,
                color =
                    Color.White,
                fontSize = 24.sp,
                fontWeight =
                    FontWeight.Bold
            )

        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 12.dp
                    )
                    .alpha(
                        if (isMuted)
                            0.4f
                        else
                            1.0f
                    ),

            verticalArrangement =
                Arrangement.SpaceBetween
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween,

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight =
                        FontWeight.Medium,

                    modifier =
                        Modifier.clickable {

                            Log.d(
                                "InstrumentPicker",
                                "Nome clicado"
                            )

                            onInstrumentClick
                                ?.invoke()

                        }
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically,

                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    // =========================================================
                    // MASTER / GERAL
                    // =========================================================

                    if (showMasterButton) {

                        Box(
                            modifier =
                                Modifier
                                    .size(22.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            3.dp
                                        )
                                    )
                                    .background(
                                        if (isMaster)
                                            accentColor
                                        else
                                            Color(0xFF505050)
                                    )
                                    .clickable {

                                        onMasterChanged(
                                            !isMaster
                                        )

                                    },

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text = "G",
                                color =
                                    Color.White,
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall
                            )

                        }

                    }

                    // =========================================================
                    // MUTE
                    // =========================================================

                    Box(
                        modifier =
                            Modifier
                                .size(22.dp)
                                .clip(
                                    RoundedCornerShape(
                                        3.dp
                                    )
                                )
                                .background(
                                    if (isMuted)
                                        MaterialTheme
                                            .colorScheme
                                            .error
                                    else
                                        Color(0xFF505050)
                                )
                                .clickable {

                                    onMuteChanged(
                                        !isMuted
                                    )

                                },

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text = "M",
                            color =
                                Color.White,
                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall
                        )

                    }

                    Box(
                        modifier =
                            Modifier
                                .size(10.dp)
                                .background(
                                    if (isIndicatorOn)
                                        Color(0xFF4CAF50)
                                    else
                                        Color.DarkGray,
                                    RoundedCornerShape(
                                        4.dp
                                    )
                                )
                    )

                    Box(
                        modifier =
                            Modifier.width(38.dp),
                        contentAlignment =
                            Alignment.CenterEnd
                    ) {

                        Text(
                            "${volume.toInt()}%",
                            color =
                                corTextoVolume,
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                    }

                }

            }

            Slider(
                value = volume,

                onValueChange =
                    onVolumeChanged,

                valueRange =
                    0f..100f,

                enabled =
                    sliderEnabled,

                modifier =
                    Modifier.fillMaxWidth(),

                colors =
                    SliderDefaults.colors(
                        thumbColor =
                            corCaixaCanal,

                        activeTrackColor =
                            corCaixaCanal,

                        inactiveTrackColor =
                            Color(0xFF2C2C32)
                    )
            )

        }

    }

}


// =============================================================================
// CONVERSÃO DE VOLUME
// =============================================================================

private fun percentToMidi(
    volume: Float
): Int {

    return (
            (volume / 100f) * 127f
            )
        .toInt()
        .coerceIn(
            0,
            127
        )

}
