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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.watchopolis.wear.engine.ZoneStatus

/** Read-only zone query result for a single tile (see the Query tool). */
@Composable
fun ZoneStatusScreen(
    status: ZoneStatus,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .crownScroll(scroll)
            .verticalScroll(scroll)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ListHeader { Text("Zone") }

        status.categoryName?.let {
            Text(it, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Stat("Density", status.densityLabel)
        Stat("Land Value", status.landValueLabel)
        Stat("Crime", status.crimeLabel)
        Stat("Pollution", status.pollutionLabel)
        Stat("Growth", status.growthLabel)
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Color.Gray, fontSize = 10.sp)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
