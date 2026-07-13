package com.robsonsmartins.androidmidisynth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BatteryIndicator(

    voltage: Float,

    level: Int

) {

    Row(

        verticalAlignment = Alignment.CenterVertically,

        horizontalArrangement = Arrangement.spacedBy(6.dp)

    ) {

        BatteryIcon(level)

        Text(

            text = String.format("%.2f V", voltage),

            style = MaterialTheme.typography.bodyMedium,

            color = Color.White

        )

    }

}

@Composable
private fun BatteryIcon(

    level: Int

) {

    Row(

        verticalAlignment = Alignment.CenterVertically

    ) {

        Box(

            modifier = Modifier

                .width(22.dp)

                .height(12.dp)

                .border(

                    width = 1.dp,

                    color = Color.White,

                    shape = RoundedCornerShape(2.dp)

                )

                .padding(1.dp)

        ) {

            Row(

                modifier = Modifier.matchParentSize(),

                horizontalArrangement = Arrangement.spacedBy(1.dp)

            ) {

                repeat(5) { index ->

                    Box(

                        modifier = Modifier

                            .weight(1f)

                            .height(10.dp)

                            .background(

                                if (index < level)

                                    Color(0xFF4CAF50)

                                else

                                    Color.Transparent

                            )

                    )

                }

            }

        }

        Spacer(

            modifier = Modifier.width(2.dp)

        )

        Box(

            modifier = Modifier

                .width(2.dp)

                .height(6.dp)

                .background(

                    Color.White,

                    RoundedCornerShape(1.dp)

                )

        )

    }

}