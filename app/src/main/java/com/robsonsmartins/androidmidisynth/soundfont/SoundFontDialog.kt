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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun SoundFontDialog(
    soundFonts: List<SoundFontInfo>,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    onImport: () -> Unit,
    podeExcluir: (Int) -> Boolean,
    onExcluir: (Int) -> Boolean
) {

    val carregadas = soundFonts.count { it.carregada }

    var soundFontExcluir by remember {

        mutableStateOf<SoundFontInfo?>(null)

    }

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

                            trailingContent = {

                                if (podeExcluir(sf.id)) {

                                    IconButton(

                                        onClick = {

                                            soundFontExcluir = sf

                                        }

                                    ) {

                                        Icon(

                                            imageVector = Icons.Default.Delete,

                                            contentDescription = "Excluir SoundFont"

                                        )

                                    }

                                }

                            },

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

    soundFontExcluir?.let { sf ->

        AlertDialog(

            onDismissRequest = {

                soundFontExcluir = null

            },

            title = {

                Text("Excluir SoundFont")

            },

            text = {

                Text(
                    "Deseja excluir \"${sf.nome}\"?"
                )

            },

            confirmButton = {

                TextButton(

                    onClick = {

                        onExcluir(sf.id)

                        soundFontExcluir = null

                    }

                ) {

                    Text("Excluir")

                }

            },

            dismissButton = {

                TextButton(

                    onClick = {

                        soundFontExcluir = null

                    }

                ) {

                    Text("Cancelar")

                }

            }

        )

    }

}