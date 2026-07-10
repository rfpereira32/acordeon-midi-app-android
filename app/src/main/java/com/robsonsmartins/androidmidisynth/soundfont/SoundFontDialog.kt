package com.robsonsmartins.androidmidisynth.soundfont

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ListItem
import androidx.compose.runtime.*

@Composable
fun SoundFontDialog(
    soundFonts: List<SoundFontInfo>,
    onDismiss: () -> Unit,
    onApply: (List<SoundFontInfo>) -> Unit
) {

    val lista = remember {

        soundFonts.map {
            it.copy()
        }.toMutableStateList()

    }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {

            Text("Selecionar SoundFonts")

        },

        text = {

            LazyColumn {

                items(lista) { sf ->

                    ListItem(

                        headlineContent = {

                            Text(sf.nome)

                        },

                        leadingContent = {

                            Checkbox(

                                checked = sf.carregada,

                                onCheckedChange = {

                                    sf.carregada = it

                                }

                            )

                        }

                    )

                }

            }

        },

        confirmButton = {

            TextButton(

                onClick = {

                    onApply(lista)

                }

            ) {

                Text("Aplicar")

            }

        },

        dismissButton = {

            TextButton(

                onClick = onDismiss

            ) {

                Text("Cancelar")

            }

        }

    )

}