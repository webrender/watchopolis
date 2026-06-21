package com.watchopolis.wear.engine

import android.content.Context
import java.io.File

/**
 * The engine loads cities via stdio paths (fileio.cpp), so bundled .cty assets
 * must be copied out of the APK to a real file before [MicropolisEngine.loadCity].
 */
object CityAssets {
    /** Copy `cities/<name>` from assets into internal storage; return its path. */
    fun ensureCity(context: Context, name: String): String {
        val outDir = File(context.filesDir, "cities").apply { mkdirs() }
        val outFile = File(outDir, name)
        if (!outFile.exists()) {
            context.assets.open("cities/$name").use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return outFile.absolutePath
    }
}
