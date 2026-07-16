package com.robsonsmartins.androidmidisynth.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.robsonsmartins.androidmidisynth.soundfont.InstrumentItem

@Composable
fun InstrumentPickerDialog(

    instrumentos: List<InstrumentItem>,

    onDismiss: () -> Unit,

    onInstrumentSelected: (InstrumentItem) -> Unit

) {

    var pesquisa by remember {

        mutableStateOf("")

    }

    val listaFiltrada = remember(
        pesquisa,
        instrumentos
    ) {

        if (pesquisa.isBlank()) {

            instrumentos

        } else {

            instrumentos.filter {

                it.preset.nome.contains(
                    pesquisa,
                    ignoreCase = true
                )

            }

        }

    }

    AlertDialog(

        onDismissRequest = onDismiss,

        confirmButton = {},

        title = {

            Text("Escolher instrumento")

        },

        text = {

            androidx.compose.foundation.layout.Column {

                OutlinedTextField(

                    value = pesquisa,

                    onValueChange = {

                        pesquisa = it

                    },

                    modifier = Modifier
                        .fillMaxWidth(),

                    label = {

                        Text("Pesquisar")

                    },

                    singleLine = true,

                    keyboardOptions = KeyboardOptions(

                        imeAction = ImeAction.Search

                    )

                )

                LazyColumn(

                    modifier = Modifier
                        .padding(top = 12.dp)
                        .heightIn(max = 450.dp)

                ) {

                    items(listaFiltrada) { item ->

                        InstrumentListItem(

                            item = item,

                            onClick = {

                                onInstrumentSelected(item)

                            }

                        )

                    }

                }

            }

        }

    )

}