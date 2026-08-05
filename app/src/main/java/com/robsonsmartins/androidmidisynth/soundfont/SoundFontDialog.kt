package com.robsonsmartins.androidmidisynth.soundfont

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SoundFontDialog(
    soundFonts: List<SoundFontInfo>,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onImport: () -> Unit
) {

    val carregadas = soundFonts.count { it.carregada }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {

            Column {

                Text(
                    text = "Selecionar SoundFonts",
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    text = "$carregadas de ${soundFonts.size} carregadas",
                    style = MaterialTheme.typography.bodySmall
                )

            }

        },

        text = {

            Column {

                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp)
                ) {

                    items(
                        items = soundFonts,
                        key = { it.id }
                    ) { sf ->

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

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {

                    TextButton(
                        onClick = onImport
                    ) {

                        Text("Importar...")

                    }

                }

            }

        },

        confirmButton = {

            TextButton(

                onClick = {

                    onApply()

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