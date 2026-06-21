package com.watchopolis.wear.render

import androidx.compose.ui.graphics.Color

/**
 * Maps a raw 16-bit tile word to a flat category color. Used by the first
 * rendering pass (before the real tileset atlas). Category boundaries come from
 * the Tiles enum in packages/micropolis-engine/src/micropolis.h.
 */
object TileColors {
    private val DIRT = Color(0xFF6B5A3E)
    private val WATER = Color(0xFF2C5BA8)
    private val FLOOD = Color(0xFF5B86C9)
    private val TREE = Color(0xFF2E6B2E)
    private val RUBBLE = Color(0xFF8A8A7A)
    private val RADIATION = Color(0xFFB13CC4)
    private val FIRE = Color(0xFFE5642A)
    private val ROAD = Color(0xFF5A5A5A)
    private val RAIL_POWER = Color(0xFF3A3A3A)
    private val RESIDENTIAL = Color(0xFF34C759)
    private val COMMERCIAL = Color(0xFF3A8DFF)
    private val INDUSTRIAL = Color(0xFFE6C300)
    private val SPECIAL = Color(0xFFB0B0B0)

    fun forTile(raw: Int): Color {
        return when (raw and 0x3FF) {
            0 -> DIRT
            in 2..20 -> WATER
            in 21..43 -> TREE
            in 44..47 -> RUBBLE
            in 48..51 -> FLOOD
            in 52..55 -> RADIATION
            in 56..63 -> FIRE
            in 64..206 -> ROAD
            in 207..238 -> RAIL_POWER
            in 240..422 -> RESIDENTIAL
            in 423..611 -> COMMERCIAL
            in 612..692 -> INDUSTRIAL
            in 693..826 -> SPECIAL
            else -> DIRT
        }
    }
}
