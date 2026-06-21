package com.watchopolis.wear.ui

import android.app.RemoteInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
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
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import androidx.wear.input.RemoteInputIntentHelper
import com.watchopolis.wear.engine.CityEntry
import com.watchopolis.wear.engine.CityCatalog
import com.watchopolis.wear.game.Game

private const val KEY_SCENARIO_NAME = "scenario_name"

@Composable
fun ScenarioScreen(
    game: Game,
    onStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    var pendingScenario by remember { mutableStateOf<CityEntry?>(null) }

    val nameLauncher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        val name = result.data
            ?.let { RemoteInput.getResultsFromIntent(it)?.getCharSequence(KEY_SCENARIO_NAME)?.toString() }
            ?: ""
        pendingScenario?.let { scenario ->
            game.loadScenario(context, scenario.asset, name.ifBlank { scenario.label })
            onStarted()
        }
        pendingScenario = null
    }

    fun pickScenario(entry: CityEntry) {
        pendingScenario = entry
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(
            intent,
            listOf(RemoteInput.Builder(KEY_SCENARIO_NAME).setLabel("City name").build()),
        )
        nameLauncher.launch(intent)
    }

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
        ListHeader { Text("Choose scenario") }

        CityCatalog.SCENARIOS.forEach { entry ->
            Chip(
                onClick = { pickScenario(entry) },
                label = { Text(entry.label) },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
