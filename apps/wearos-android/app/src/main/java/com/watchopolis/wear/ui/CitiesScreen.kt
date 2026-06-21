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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.watchopolis.wear.engine.CityCatalog
import com.watchopolis.wear.game.Game

@Composable
fun CitiesScreen(
    game: Game,
    onLoaded: () -> Unit,
    onNewCity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    var saved by remember { mutableStateOf(false) }
    val hasSave = remember { game.hasSave(context) }

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
        ListHeader { Text("Cities") }

        Chip(
            onClick = onNewCity,
            label = { Text("New random city") },
            colors = ChipDefaults.primaryChipColors(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (game.currentCity.isNotEmpty()) {
            Chip(
                onClick = { game.saveGame(context); saved = true },
                label = { Text(if (saved) "Saved ✓" else "Save game") },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (hasSave) {
            Chip(
                onClick = { game.loadSaved(context); onLoaded() },
                label = { Text("Continue saved") },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text("New game", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        CityCatalog.SCENARIOS.forEach { entry ->
            Chip(
                onClick = { game.loadCity(context, entry.asset); onLoaded() },
                label = { Text(entry.label) },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
