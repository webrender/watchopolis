package com.watchopolis.wear.engine

/**
 * Result of the query tool on a single tile, as raw engine string indices (see
 * doZoneStatus in tool.cpp). The five stat indices are each STR202 index + 1,
 * clamped to 1..20 (text.h); [category] is a 1-based index into the stri.219
 * tile-category list. Map them to display strings with the accessors below.
 */
data class ZoneStatus(
    val category: Int,
    val populationDensity: Int,
    val landValue: Int,
    val crimeRate: Int,
    val pollution: Int,
    val growthRate: Int,
) {
    /** Tile-category name (e.g. "Residential"), or null for the engine's
     *  out-of-range "dirt" case so the UI can simply omit a header. */
    val categoryName: String? get() = TILE_DESCRIPTIONS.getOrNull(category - 1)

    val densityLabel: String get() = str202(populationDensity)
    val landValueLabel: String get() = str202(landValue)
    val crimeLabel: String get() = str202(crimeRate)
    val pollutionLabel: String get() = str202(pollution)
    val growthLabel: String get() = str202(growthRate)

    private companion object {
        // Flat 20-entry STR202 table (text.h:85-108). The engine's five stat
        // values are STR202 index + 1, so each lands in its own sub-range and a
        // single value-1 lookup covers all of them.
        val STR202 = arrayOf(
            "Low", "Medium", "High", "Very High",                 // population density
            "Slum", "Lower Class", "Middle Class", "High",        // land value
            "Safe", "Light", "Moderate", "Dangerous",             // crime
            "None", "Moderate", "Heavy", "Very Heavy",            // pollution
            "Declining", "Stable", "Slow Growth", "Fast Growth",  // growth rate
        )

        // Tile categories (content/micropolis/data/stri.219.txt), 1-based in the
        // engine; index here with category - 1.
        val TILE_DESCRIPTIONS = arrayOf(
            "Clear", "Water", "Trees", "Rubble", "Flood", "Radioactive Waste",
            "Fire", "Road", "Power", "Rail", "Residential", "Commercial",
            "Industrial", "Seaport", "Airport", "Coal Power", "Fire Department",
            "Police Department", "Stadium", "Nuclear Power", "Draw Bridge",
            "Radar Dish", "Fountain", "Industrial", "Steelers 38  Bears 3",
            "Draw Bridge", "Ur 238",
        )

        fun str202(value: Int): String = STR202.getOrElse(value - 1) { "—" }
    }
}
