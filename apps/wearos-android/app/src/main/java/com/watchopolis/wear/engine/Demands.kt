package com.watchopolis.wear.engine

/** RCI demand snapshot. Each value is -1 (lowest demand) .. 1 (highest demand). */
data class Demands(
    val residential: Float,
    val commercial: Float,
    val industrial: Float,
)
