package com.watchopolis.wear.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.AnchorType
import androidx.wear.compose.foundation.CurvedDirection
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.material.curvedText
import com.watchopolis.wear.engine.GameTool
import com.watchopolis.wear.engine.MicropolisEngine
import com.watchopolis.wear.game.Game
import com.watchopolis.wear.render.TileAtlas
import com.watchopolis.wear.render.TileColors
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private val MONTHS = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private fun monthName(month: Int): String = MONTHS.getOrElse(month.coerceIn(0, 11)) { "" }

/** Compact large numbers for the small HUD (1234567 -> "1.2M"). */
internal fun compact(value: Long): String = when {
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
    value >= 1_000 -> "%.1fk".format(value / 1_000.0)
    else -> value.toString()
}

/**
 * Blit the map tiles in [xMin..xMax]/[yMin..yMax] onto the canvas, scaling each
 * [tile]-px source cell to [tilePx] screen px, offset by [originX]/[originY].
 * Shared by the interactive map and the new-city preview so both look identical.
 */
internal fun DrawScope.drawCity(
    atlas: TileAtlas?,
    map: ShortArray,
    tile: Int,
    originX: Float,
    originY: Float,
    tilePx: Float,
    xMin: Int,
    xMax: Int,
    yMin: Int,
    yMax: Int,
) {
    val s = tilePx / tile
    translate(originX, originY) {
        scale(scaleX = s, scaleY = s, pivot = Offset.Zero) {
            val srcSize = IntSize(tile, tile)
            val dstSize = IntSize(tile, tile)
            for (x in xMin..xMax) {
                for (y in yMin..yMax) {
                    val raw = map[MicropolisEngine.tileIndex(x, y)].toInt()
                    val id = raw and 0x3FF
                    if (atlas != null) {
                        drawImage(
                            image = atlas.image,
                            srcOffset = IntOffset(atlas.colOf(id) * tile, atlas.rowOf(id) * tile),
                            srcSize = srcSize,
                            dstOffset = IntOffset(x * tile, y * tile),
                            dstSize = dstSize,
                            filterQuality = FilterQuality.None,
                        )
                    } else {
                        drawRect(
                            color = TileColors.forTile(raw),
                            topLeft = Offset((x * tile).toFloat(), (y * tile).toFloat()),
                            size = androidx.compose.ui.geometry.Size(tile.toFloat(), tile.toFloat()),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Translucent band ringing the whole watch edge, behind the curved HUD text.
 * Fades from transparent at its inner edge to opaque at the bezel. Shared by the
 * map and the new-city preview so both have the same edge treatment.
 */
internal fun DrawScope.drawEdgeVignette() {
    val band = 34.dp.toPx()
    val ringRadius = size.minDimension / 2f - band / 2f + 2f
    val outerRadius = ringRadius + band / 2f
    val innerRadius = ringRadius - band / 2f
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                (innerRadius / outerRadius).coerceIn(0f, 1f) to Color.Transparent,
                1f to Color(0xE6000000),
            ),
            center = center,
            radius = outerRadius,
        ),
        radius = ringRadius,
        style = Stroke(width = band),
    )
}

// Zoom presets (screen px per tile). Double-tap cycles: medium -> close ->
// overview -> medium, so the first double-tap zooms in.
// medium -> close -> closer -> overview -> (wrap) medium
private val ZOOM_PRESETS = floatArrayOf(14f, 22f, 32f, 7f)
private const val DEFAULT_ZOOM_INDEX = 0

// Accumulated crown rotation needed to step to the next/previous tool.
private const val ROTARY_STEP = 48f

/**
 * The interactive map. Crown cycles the active tool, tap builds, drag pans,
 * double-tap cycles zoom, long-press opens the menu.
 */
@Composable
fun MapScreen(
    game: Game,
    onOpenMenu: () -> Unit,
    active: Boolean,
    message: String? = null,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val atlas = remember {
        runCatching { TileAtlas.loadFromAssets(view.context, "tilesets/classic.png") }.getOrNull()
    }

    // Camera + interaction state (map-local; survives menu overlays).
    var zoomIndex by remember { mutableIntStateOf(DEFAULT_ZOOM_INDEX) }
    var tilePx by remember { mutableFloatStateOf(ZOOM_PRESETS[DEFAULT_ZOOM_INDEX]) }
    var camX by remember { mutableFloatStateOf(MicropolisEngine.WORLD_W / 2f) }
    var camY by remember { mutableFloatStateOf(MicropolisEngine.WORLD_H / 2f) }
    var toolIndex by remember { mutableIntStateOf(GameTool.ALL.indexOf(GameTool.ROAD)) }
    var rotaryAcc by remember { mutableFloatStateOf(0f) }

    val focusRequester = remember { FocusRequester() }
    // Re-claim crown focus whenever the map becomes the active screen again.
    LaunchedEffect(active) { if (active) focusRequester.requestFocus() }

    val tile = atlas?.tilePx ?: 16

    fun tileAt(screen: Offset, canvasW: Int, canvasH: Int): Pair<Int, Int> {
        val originX = canvasW / 2f - camX * tilePx
        val originY = canvasH / 2f - camY * tilePx
        return floor((screen.x - originX) / tilePx).toInt() to
            floor((screen.y - originY) / tilePx).toInt()
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onRotaryScrollEvent { event ->
                rotaryAcc += event.verticalScrollPixels
                val n = GameTool.ALL.size
                while (rotaryAcc >= ROTARY_STEP) {
                    toolIndex = (toolIndex + 1) % n
                    rotaryAcc -= ROTARY_STEP
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                while (rotaryAcc <= -ROTARY_STEP) {
                    toolIndex = (toolIndex - 1 + n) % n
                    rotaryAcc += ROTARY_STEP
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onOpenMenu() },
                    onDoubleTap = {
                        zoomIndex = (zoomIndex + 1) % ZOOM_PRESETS.size
                        tilePx = ZOOM_PRESETS[zoomIndex]
                    },
                    onTap = { offset ->
                        val (tx, ty) = tileAt(offset, size.width, size.height)
                        if (tx in 0 until MicropolisEngine.WORLD_W && ty in 0 until MicropolisEngine.WORLD_H) {
                            val result = game.engine.doTool(GameTool.ALL[toolIndex].tool, tx, ty)
                            view.performHapticFeedback(
                                if (result > 0) HapticFeedbackConstants.CONFIRM
                                else HapticFeedbackConstants.REJECT,
                            )
                            game.refresh()
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    camX = (camX - drag.x / tilePx).coerceIn(0f, MicropolisEngine.WORLD_W.toFloat())
                    camY = (camY - drag.y / tilePx).coerceIn(0f, MicropolisEngine.WORLD_H.toFloat())
                }
            },
    ) {
        game.frame // observe so the map redraws each tick

        drawRect(color = Color.Black, size = size)

        val w = MicropolisEngine.WORLD_W
        val h = MicropolisEngine.WORLD_H
        val originX = size.width / 2f - camX * tilePx
        val originY = size.height / 2f - camY * tilePx

        val xMin = floor(-originX / tilePx).toInt().coerceIn(0, w - 1)
        val xMax = ceil((size.width - originX) / tilePx).toInt().coerceIn(0, w - 1)
        val yMin = floor(-originY / tilePx).toInt().coerceIn(0, h - 1)
        val yMax = ceil((size.height - originY) / tilePx).toInt().coerceIn(0, h - 1)

        drawCity(atlas, game.map, tile, originX, originY, tilePx, xMin, xMax, yMin, yMax)
        drawEdgeVignette()
    }

    // HUD stats, each anchored at its own angle so they spread across the top
    // half (upper-left -> upper-right): 180 = 9 o'clock, 270 = top, 360 = 3 o'clock.
    // A notice temporarily takes over this same arc instead of popping up as a
    // separate banner, then the stats return once it's dismissed.
    if (message != null) {
        HudItem(270f, " $message ", Color.White)
    } else {
        val activeTool = GameTool.ALL[toolIndex]
        val hud = game.hud
        HudItem(206f, " ${activeTool.label} ", activeTool.color)
        HudItem(249f, " ${monthName(hud.month)} ${hud.year} ", Color.White)
        HudItem(291f, " $${compact(hud.funds)} ", Color(0xFF9ED99E))
        HudItem(334f, " ${compact(hud.population)} ", Color(0xFF9EC9FF))
    }

    // R/C/I demand indicators, mirrored onto the bottom half of the bezel.
    // The row is reversed (see RciItem) to keep text upright on the bottom
    // arc, which also flips left-to-right reading order, so the anchors are
    // assigned R/C/I right-to-left here to read "RCI" left-to-right on screen.
    val demands = game.hud.demands
    RciItem(133f, "R", Color(0xFF6FCB6F), demands.residential)
    RciItem(90f, "C", Color(0xFF6FA8E3), demands.commercial)
    RciItem(47f, "I", Color(0xFFE3D36F), demands.industrial)
}

/**
 * A single curved HUD label centered at [anchorDeg] along the screen edge.
 */
@Composable
private fun HudItem(anchorDeg: Float, text: String, color: Color) {
    CurvedLayout(
        modifier = Modifier.fillMaxSize(),
        anchor = anchorDeg,
        anchorType = AnchorType.Center,
    ) {
        curvedText(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * One R/C/I letter plus a row of 0-3 dots showing [demand] (-1..1, from
 * [MicropolisEngine.readDemands]): dots to the left are red and count
 * oversupply, dots to the right are green and count unmet demand. Anchored
 * on the bottom half of the bezel, so the row direction is reversed to keep
 * the text upright and left-to-right for the user.
 */
@Composable
private fun RciItem(anchorDeg: Float, label: String, labelColor: Color, demand: Float) {
    val dots = (abs(demand).coerceIn(0f, 1f) * 3f).roundToInt().coerceIn(0, 3)
    val leftDots = if (demand < 0f) "•".repeat(dots) else ""
    val rightDots = if (demand > 0f) "•".repeat(dots) else ""
    CurvedLayout(
        modifier = Modifier.fillMaxSize(),
        anchor = anchorDeg,
        anchorType = AnchorType.Center,
    ) {
        curvedRow(angularDirection = CurvedDirection.Angular.Reversed) {
            if (leftDots.isNotEmpty()) {
                curvedText(
                    text = " $leftDots",
                    color = Color(0xFFE38B8B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            curvedText(
                text = " $label ",
                modifier = CurvedModifier.padding(angular = 4.dp),
                color = labelColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            if (rightDots.isNotEmpty()) {
                curvedText(
                    text = "$rightDots ",
                    color = Color(0xFF8BE38B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
