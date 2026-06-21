package com.watchopolis.wear.engine

/** Short, watch-friendly text for engine message indices (MessageNumber in text.h). */
object MessageText {
    // Index 0 unused; 1..57 match the MessageNumber enum.
    private val TEXT = arrayOf(
        "",
        "More residential zones needed",   // 1
        "More commercial zones needed",
        "More industrial zones needed",
        "More roads required",
        "Inadequate rail system",          // 5
        "Build a power plant",
        "Residents demand a stadium",
        "Industry requires a seaport",
        "Commerce requires an airport",
        "Pollution very high",             // 10
        "Crime very high",
        "Frequent traffic jams",
        "Citizens demand a fire dept",
        "Citizens demand a police dept",
        "Blackouts reported",              // 15
        "Tax rate too high",
        "Roads deteriorating — fund them",
        "Fire depts need funding",
        "Police depts need funding",
        "Fire reported!",                  // 20
        "A monster has been sighted!!",
        "Tornado reported!!",
        "Major earthquake!!!",
        "A plane has crashed!",
        "Shipwreck reported!",             // 25
        "A train crashed!",
        "A helicopter crashed!",
        "Unemployment is high",
        "YOUR CITY IS BROKE!",
        "Firebombing reported!",           // 30
        "Need more parks",
        "Explosion detected!",
        "Insufficient funds",
        "Bulldoze the area first",
        "Population reached 2,000",        // 35
        "Population reached 10,000",
        "Population reached 50,000",
        "Population reached 100,000",
        "Population reached 500,000",
        "Brownouts — build a power plant", // 40
        "Heavy traffic reported",
        "Flooding reported!!",
        "Nuclear meltdown!!!",
        "Rioting in the streets!!",
        "Started a new city",              // 45
        "Restored a saved city",
        "You won the scenario!",
        "You lost the scenario",
        "About Micropolis",
        "Dullsville scenario",             // 50
        "San Francisco scenario",
        "Hamburg scenario",
        "Bern scenario",
        "Tokyo scenario",
        "Detroit scenario",                // 55
        "Boston scenario",
        "Rio de Janeiro scenario",         // 57
    )

    fun of(index: Int): String? = TEXT.getOrNull(index)?.takeIf { it.isNotEmpty() }
}
