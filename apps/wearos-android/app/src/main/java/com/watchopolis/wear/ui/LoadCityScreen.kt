package com.watchopolis.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.watchopolis.wear.game.Game

@Composable
fun LoadCityScreen(
    game: Game,
    onLoaded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val saves = remember { game.listSaves(context) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .crownScroll(scroll)
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ListHeader { Text("Load city") }

        if (saves.isEmpty()) {
            Text(
                "No saved cities",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            saves.forEach { name ->
                Chip(
                    onClick = { game.loadSave(context, name); onLoaded() },
                    label = { Text(name) },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
