package com.watchopolis.wear.ui

import android.app.RemoteInput
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.AnchorType
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.curvedText
import androidx.wear.input.RemoteInputIntentHelper
import com.watchopolis.wear.engine.MicropolisEngine
import com.watchopolis.wear.game.Game
import com.watchopolis.wear.render.TileAtlas
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

private const val KEY_CITY_NAME = "city_name"

private enum class Step { Preview, Config }

/**
 * New-city flow in two panes:
 *  1. A full-screen preview of the generated terrain (edges cropped), styled like
 *     the map (edge vignette + curved labels). Tap regenerates, swipe left advances.
 *  2. A config pane to name the city (Wear system input) and pick difficulty, which
 *     sets the starting funds and begins play.
 */
@Composable
fun NewCityScreen(
    game: Game,
    onStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(Step.Preview) }
    var cityName by remember { mutableStateOf("") }
    // Avoid flashing the previously-loaded city before the first terrain exists.
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        game.previewRandomCity()
        ready = true
    }

    // From the config pane, back returns to the preview rather than leaving the flow.
    BackHandler(enabled = step == Step.Config) { step = Step.Preview }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            if (targetState == Step.Config) {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
            } else {
                slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
            }
        },
        label = "new-city-pane",
        modifier = modifier.fillMaxSize(),
    ) { pane ->
        when (pane) {
            Step.Preview -> PreviewPane(
                game = game,
                ready = ready,
                onRegenerate = { game.previewRandomCity() },
                onAdvance = { step = Step.Config },
            )
            Step.Config -> ConfigPane(
                cityName = cityName,
                onName = { cityName = it },
                onStart = { level -> game.startPreviewedCity(level, cityName); onStarted() },
                onBack = { step = Step.Preview },
            )
        }
    }
}

@Composable
private fun PreviewPane(
    game: Game,
    ready: Boolean,
    onRegenerate: () -> Unit,
    onAdvance: () -> Unit,
) {
    val context = LocalContext.current
    val atlas = remember {
        runCatching { TileAtlas.loadFromAssets(context, "tilesets/classic.png") }.getOrNull()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { onRegenerate() }
            }
            .pointerInput(Unit) {
                val threshold = 60.dp.toPx()
                var dx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dx = 0f },
                    onDragEnd = { if (dx < -threshold) onAdvance() },
                ) { _, amount -> dx += amount }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            game.frame // observe so a regenerate redraws
            if (!ready) return@Canvas

            val w = MicropolisEngine.WORLD_W
            val h = MicropolisEngine.WORLD_H
            val tile = atlas?.tilePx ?: 16
            // Fill the screen (crop the longer axis) rather than fit.
            val tilePx = max(size.width / w, size.height / h)
            val originX = (size.width - w * tilePx) / 2f
            val originY = (size.height - h * tilePx) / 2f

            val xMin = floor(-originX / tilePx).toInt().coerceIn(0, w - 1)
            val xMax = ceil((size.width - originX) / tilePx).toInt().coerceIn(0, w - 1)
            val yMin = floor(-originY / tilePx).toInt().coerceIn(0, h - 1)
            val yMax = ceil((size.height - originY) / tilePx).toInt().coerceIn(0, h - 1)

            drawCity(atlas, game.map, tile, originX, originY, tilePx, xMin, xMax, yMin, yMax)
            drawEdgeVignette()
        }

        // Top label (upright on the top arc).
        CurvedLayout(modifier = Modifier.fillMaxSize(), anchor = 270f, anchorType = AnchorType.Center) {
            curvedText(
                text = " New City ",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        // Bottom label: reverse the row so it reads upright on the bottom arc.
        CurvedLayout(modifier = Modifier.fillMaxSize(), anchor = 90f, anchorType = AnchorType.Center) {
            curvedRow(angularDirection = CurvedDirection.Angular.Reversed) {
                curvedText(
                    text = " Tap to regenerate, swipe to continue ",
                    color = Color(0xFFCFCFCF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ConfigPane(
    cityName: String,
    onName: (String) -> Unit,
    onStart: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        RemoteInput.getResultsFromIntent(result.data)
            ?.getCharSequence(KEY_CITY_NAME)
            ?.let { onName(it.toString()) }
    }
    fun launchNameInput() {
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        val inputs = listOf(RemoteInput.Builder(KEY_CITY_NAME).setLabel("City name").build())
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, inputs)
        launcher.launch(intent)
    }

    val scroll = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                val threshold = 60.dp.toPx()
                var dx = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dx = 0f },
                    onDragEnd = { if (dx > threshold) onBack() },
                ) { _, amount -> dx += amount }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .crownScroll(scroll)
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ListHeader { Text("New City") }

            Chip(
                onClick = { launchNameInput() },
                label = { Text(cityName.ifBlank { "Name your city" }) },
                colors = ChipDefaults.secondaryChipColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                "Start game",
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            DIFFICULTIES.forEach { (label, level) ->
                Chip(
                    onClick = { onStart(level) },
                    label = { Text(label) },
                    colors = ChipDefaults.primaryChipColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// Label -> GameLevel ordinal (Easy=0, Medium=1, Hard=2). Funds: 20k / 10k / 5k.
private val DIFFICULTIES = listOf(
    "Easy" to 0,
    "Medium" to 1,
    "Hard" to 2,
)
