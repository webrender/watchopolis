package com.watchopolis.wear.render

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * A tileset atlas: a grid of [tilePx]×[tilePx] tiles. The classic Micropolis
 * atlas is 512×512 → a 32-wide grid, so tile id maps to (col = id % 32,
 * row = id / 32). Tile ids are the low 10 bits of a map word.
 */
class TileAtlas(
    val image: ImageBitmap,
    val tilePx: Int = 16,
) {
    val columns: Int = image.width / tilePx

    fun colOf(tileId: Int): Int = tileId % columns
    fun rowOf(tileId: Int): Int = tileId / columns

    companion object {
        fun loadFromAssets(context: Context, path: String, tilePx: Int = 16): TileAtlas {
            val bitmap = context.assets.open(path).use { BitmapFactory.decodeStream(it) }
            return TileAtlas(bitmap.asImageBitmap(), tilePx)
        }
    }
}
