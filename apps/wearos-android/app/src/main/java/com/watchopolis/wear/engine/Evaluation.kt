package com.watchopolis.wear.engine

/** City evaluation snapshot. [problems] is (problemId, votePercent), most-voted first. */
data class Evaluation(
    val score: Int,
    val scoreDelta: Int,
    val cityClass: Int,
    val approval: Int,
    val problems: List<Pair<Int, Int>>,
) {
    val className: String get() = CITY_CLASSES.getOrElse(cityClass) { "City" }

    companion object {
        private val CITY_CLASSES = arrayOf(
            "Village", "Town", "City", "Capital", "Metropolis", "Megalopolis",
        )

        // Problem ids from evaluate.cpp.
        private val PROBLEMS = arrayOf(
            "Crime", "Pollution", "Housing", "Taxes", "Traffic", "Unemployment", "Fire",
        )

        fun problemName(id: Int): String = PROBLEMS.getOrElse(id) { "—" }
    }
}
