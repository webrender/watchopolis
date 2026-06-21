package com.watchopolis.wear.engine

import androidx.compose.ui.graphics.Color

/**
 * A buildable tool. [tool] is the EditingTool ordinal expected by
 * [MicropolisEngine.doTool] (see tool.h in the engine).
 */
enum class GameTool(val label: String, val tool: Int, val color: Color) {
    BULLDOZER("Bulldoze", 7, Color(0xFFE0A030)),
    ROAD("Road", 9, Color(0xFF9A9A9A)),
    RAIL("Rail", 8, Color(0xFF6A6A6A)),
    WIRE("Power", 6, Color(0xFFE6D24A)),
    RESIDENTIAL("Residential", 0, Color(0xFF34C759)),
    COMMERCIAL("Commercial", 1, Color(0xFF3A8DFF)),
    INDUSTRIAL("Industrial", 2, Color(0xFFE6C300)),
    PARK("Park", 11, Color(0xFF2E9B4E)),
    FIRE_STATION("Fire Station", 3, Color(0xFFE5402A)),
    POLICE_STATION("Police", 4, Color(0xFF3A6DFF)),
    COAL_POWER("Coal Plant", 13, Color(0xFF8A6A4A)),
    QUERY("Query", 5, Color(0xFFB0B0B0));

    companion object {
        val ALL: List<GameTool> = entries
    }
}
