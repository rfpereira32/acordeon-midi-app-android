package com.robsonmartins.androidmidisynth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ChannelCard() {

    Card(

        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)

    ) {

        Row(

            modifier = Modifier.fillMaxSize(),

            verticalAlignment = Alignment.CenterVertically

        ) {

            Box(

                modifier = Modifier

                    .width(55.dp)

                    .fillMaxHeight()

                    .background(Color(0xFFB066FF)),

                contentAlignment = Alignment.Center

            ) {

                Text("1")

            }

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Text("Accordion")

        }

    }

}