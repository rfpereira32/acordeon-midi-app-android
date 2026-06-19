package com.robsonmartins.androidmidisynth.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen() {

    Column(

        modifier = Modifier

            .fillMaxSize()

            .padding(16.dp)

    ) {

        Text("Cordovox MIDI")

        Spacer(

            modifier = Modifier.height(20.dp)

        )

        Text("Nova interface em construção")

    }

}