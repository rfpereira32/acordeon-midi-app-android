package com.robsonsmartins.androidmidisynth.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    deviceName: String,
    showBackButton: Boolean,
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit,
    backgroundColor: Color
) {

    TopAppBar(

        title = {

            Text(
                text = deviceName,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 1
            )

        },

        navigationIcon = {

            if (showBackButton) {

                IconButton(
                    onClick = onBackClick
                ) {

                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White
                    )

                }

            } else {

                IconButton(
                    onClick = onMenuClick
                ) {

                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )

                }

            }

        },

        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = backgroundColor
        )

    )

}