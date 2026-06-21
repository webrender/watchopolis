package com.watchopolis.wear.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.watchopolis.wear.engine.CityAssets
import com.watchopolis.wear.engine.Demands
import com.watchopolis.wear.engine.MicropolisEngine
import java.io.File

/** Snapshot of headline stats for the HUD and screens. */
data class Hud(
    val year: Int = 0,
    val month: Int = 0,
    val funds: Long = 0,
    val population: Long = 0,
    val demands: Demands = Demands(0f, 0f, 0f),
)

/**
 * Owns the single engine instance, the shared tile buffer, and the latest stat
 * snapshot. Hoisted above the screens so the map, budget, evaluation, etc. all
 * read and write the same simulation.
 */
class Game {
    val engine = MicropolisEngine().apply { create() }
    val map = ShortArray(MicropolisEngine.MAP_SIZE)

    /** Bumped on every change to [map] so Canvas observers redraw. */
    var frame by mutableIntStateOf(0)
        private set
    var hud by mutableStateOf(Hud())
        private set

    /** Name of the active city; empty when no city is loaded. */
    var currentCity: String by mutableStateOf("")
        private set

    private fun savesDir(context: Context) =
        File(context.filesDir, "saves").apply { mkdirs() }

    private fun savePath(context: Context, name: String = currentCity): File =
        File(savesDir(context), "${name.ifBlank { "quicksave" }}.cty")

    fun listSaves(context: Context): List<String> {
        val dir = savesDir(context)
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "cty" }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?: emptyList()
    }

    fun saveGame(context: Context) {
        engine.saveCity(savePath(context).absolutePath)
    }

    fun loadSave(context: Context, name: String) {
        val f = savePath(context, name)
        if (f.exists()) loadPath(f.absolutePath, name)
    }

    /** Load a bundled scenario asset and give the city a user-chosen save name. */
    fun loadScenario(context: Context, assetName: String, saveName: String) {
        val label = saveName.ifBlank { assetName }
        loadPath(CityAssets.ensureCity(context, assetName), label)
        if (saveName.isNotBlank()) engine.setCityName(saveName)
    }

    private fun loadPath(path: String, label: String) {
        engine.loadCity(path)
        currentCity = label
        // Unpause BEFORE setSpeed: setSpeed() forces speed to 0 while paused.
        engine.setPaused(false)
        engine.setSpeed(3)
        engine.setPasses(1)
        engine.setSound(true)
        refresh()
    }

    /** Generate fresh terrain to preview. Paused; funds/difficulty not set yet. */
    fun previewRandomCity() {
        engine.generateRandomCity()
        engine.setPaused(true)
        refresh()
    }

    /** Commit the previewed terrain and begin play at [level]'s starting funds. */
    fun startPreviewedCity(level: Int, name: String) {
        engine.setGameLevelFunds(level)
        if (name.isNotBlank()) engine.setCityName(name)
        currentCity = name.ifBlank { "New City" }
        // Unpause BEFORE setSpeed: setSpeed() forces speed to 0 while paused.
        engine.setPaused(false)
        engine.setSpeed(3)
        engine.setPasses(1)
        engine.setSound(true)
        refresh()
    }

    /** Advance the simulation one step and refresh derived state. */
    fun tick() {
        engine.simTick()
        refresh()
    }

    /** Re-read map + stats without advancing time (after a tool edit, etc.). */
    fun refresh() {
        engine.copyMap(map)
        hud = Hud(
            engine.cityYear, engine.cityMonth, engine.totalFunds, engine.population,
            engine.readDemands(),
        )
        frame++
    }

    fun destroy() = engine.destroy()
}
