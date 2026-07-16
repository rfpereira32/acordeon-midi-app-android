package com.robsonsmartins.androidmidisynth.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.robsonsmartins.androidmidisynth.soundfont.InstrumentItem

@Composable
fun InstrumentListItem(

    item: InstrumentItem,

    onClick: () -> Unit

) {

    Column(

        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(
                horizontal = 16.dp,
                vertical = 10.dp
            )

    ) {

        Text(

            text = item.preset.nome,

            style = MaterialTheme.typography.titleMedium,

            fontWeight = FontWeight.SemiBold

        )

        Text(

            text = item.soundFont.nome,

            style = MaterialTheme.typography.bodySmall,

            color = Color.Gray

        )

    }

}