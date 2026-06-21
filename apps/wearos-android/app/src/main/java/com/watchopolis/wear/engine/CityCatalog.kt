package com.watchopolis.wear.engine

/** A bundled scenario (asset file name + display label). */
data class CityEntry(val asset: String, val label: String)

object CityCatalog {
    val SCENARIOS = listOf(
        CityEntry("haight.cty", "Haight"),
        CityEntry("scenario_san_francisco.cty", "San Francisco"),
        CityEntry("scenario_tokyo.cty", "Tokyo"),
        CityEntry("scenario_dullsville.cty", "Dullsville"),
        CityEntry("kowloon.cty", "Kowloon"),
    )
}
