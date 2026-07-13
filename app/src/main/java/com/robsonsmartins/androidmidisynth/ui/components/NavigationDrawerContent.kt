package com.robsonsmartins.androidmidisynth.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background


@Composable
fun NavigationDrawerContent(

    onSoundFontsClick: () -> Unit,

    onFavoritosClick: () -> Unit,

    onOtaClick: () -> Unit,

    onConfiguracoesClick: () -> Unit,

    onSobreClick: () -> Unit

) {

    ModalDrawerSheet(

        modifier = Modifier.fillMaxWidth(0.80f)

    ) {

        Text(

            text = "Cordovox MIDI",

            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 20.dp
            ),

            style = MaterialTheme.typography.titleLarge,

            color = Color.White

        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.DarkGray)
        )

        NavigationDrawerItem(

            label = { Text("SoundFonts") },

            icon = {

                Icon(

                    Icons.Default.Build,

                    contentDescription = null

                )

            },

            selected = false,

            onClick = onSoundFontsClick

        )

        NavigationDrawerItem(

            label = { Text("Favoritos") },

            icon = {

                Icon(

                    Icons.Default.Star,

                    contentDescription = null

                )

            },

            selected = false,

            onClick = onFavoritosClick

        )

        NavigationDrawerItem(

            label = { Text("OTA Update") },

            icon = {

                Icon(

                    Icons.Default.Refresh,

                    contentDescription = null

                )

            },

            selected = false,

            onClick = onOtaClick

        )

        NavigationDrawerItem(

            label = { Text("Configurações") },

            icon = {

                Icon(

                    Icons.Default.Settings,

                    contentDescription = null

                )

            },

            selected = false,

            onClick = onConfiguracoesClick

        )

        NavigationDrawerItem(

            label = { Text("Sobre") },

            icon = {

                Icon(

                    Icons.Default.Info,

                    contentDescription = null

                )

            },

            selected = false,

            onClick = onSobreClick

        )

    }

}