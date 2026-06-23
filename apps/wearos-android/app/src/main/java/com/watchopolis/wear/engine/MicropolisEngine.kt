package com.watchopolis.wear.engine

import androidx.annotation.Keep

/**
 * Kotlin wrapper around the native C++ Micropolis engine.
 *
 * Each instance owns a native handle (a pointer to the simulation + its
 * callback). Call [create] before use and [destroy] when finished.
 */
class MicropolisEngine {

    private var handle: Long = 0L

    /** Invoked from native code (engine thread) when the sim requests a sound. */
    var soundListener: ((channel: String, sound: String, x: Int, y: Int) -> Unit)? = null

    /** Invoked from native code when the sim posts a message/notice. */
    var messageListener: ((messageIndex: Int, x: Int, y: Int) -> Unit)? = null

    @Keep
    fun onNativeSound(channel: String, sound: String, x: Int, y: Int) {
        soundListener?.invoke(channel, sound, x, y)
    }

    @Keep
    fun onNativeMessage(messageIndex: Int, x: Int, y: Int) {
        messageListener?.invoke(messageIndex, x, y)
    }

    val isCreated: Boolean get() = handle != 0L

    fun create() {
        if (handle == 0L) handle = nativeCreate()
    }

    fun generateRandomCity() {
        check(handle != 0L) { "Engine not created" }
        nativeGenerateRandomCity(handle)
    }

    /** Set starting funds + difficulty for a new game (GameLevel ordinal). */
    fun setGameLevelFunds(level: Int) = nativeSetGameLevelFunds(handle, level)

    /** Set the city's name (stored in the engine, shown/saved with the city). */
    fun setCityName(name: String) = nativeSetCityName(handle, name)

    /** Raw 16-bit tile value at (x, y); low 10 bits are the tile id. */
    fun getTile(x: Int, y: Int): Int {
        check(handle != 0L) { "Engine not created" }
        return nativeGetTile(handle, x, y)
    }

    /**
     * Copy the whole map into [dest] (size WORLD_W * WORLD_H). Values are the raw
     * 16-bit tile words; index a tile with [tileIndex]. Low 10 bits are the id.
     */
    fun copyMap(dest: ShortArray) {
        check(handle != 0L) { "Engine not created" }
        nativeCopyMap(handle, dest)
    }

    /** Load a .cty city from an absolute filesystem path. */
    fun loadCity(path: String): Boolean {
        check(handle != 0L) { "Engine not created" }
        return nativeLoadCity(handle, path)
    }

    /** Save the current city to an absolute filesystem path. */
    fun saveCity(path: String) {
        check(handle != 0L) { "Engine not created" }
        nativeSaveCity(handle, path)
    }

    /** Advance the simulation one step and animate tiles. */
    fun simTick() {
        check(handle != 0L) { "Engine not created" }
        nativeSimTick(handle)
    }

    /** Apply [tool] (an EditingTool ordinal) at tile (x, y); returns ToolResult. */
    fun doTool(tool: Int, x: Int, y: Int): Int {
        check(handle != 0L) { "Engine not created" }
        return nativeDoTool(handle, tool, x, y)
    }

    /**
     * Run the query tool at tile (x, y) and return its zone status (category +
     * density/value/crime/pollution/growth indices), or null if the tile is out
     * of bounds. Query is read-only; it does not modify the map.
     */
    fun queryZone(x: Int, y: Int): ZoneStatus? {
        check(handle != 0L) { "Engine not created" }
        val out = IntArray(6)
        if (!nativeQueryZone(handle, x, y, out)) return null
        return ZoneStatus(out[0], out[1], out[2], out[3], out[4], out[5])
    }

    fun readBudget(): Budget {
        val l = LongArray(6)
        val f = FloatArray(3)
        nativeGetBudget(handle, l, f)
        return Budget(
            tax = l[0].toInt(),
            funds = l[1],
            taxIncome = l[2],
            roadFund = l[3], fireFund = l[4], policeFund = l[5],
            roadPercent = f[0], firePercent = f[1], policePercent = f[2],
        )
    }

    fun setTax(tax: Int) = nativeSetTax(handle, tax)
    fun setFunding(road: Float, fire: Float, police: Float) =
        nativeSetFunding(handle, road, fire, police)

    fun readEvaluation(): Evaluation {
        val v = IntArray(12)
        nativeGetEvaluation(handle, v)
        val problems = (0 until 4)
            .map { v[4 + it] to v[8 + it] }
            .filter { it.first in 0..6 }
        return Evaluation(
            score = v[0], scoreDelta = v[1], cityClass = v[2], approval = v[3],
            problems = problems,
        )
    }

    fun readDemands(): Demands {
        val f = FloatArray(3)
        nativeGetDemands(handle, f)
        return Demands(residential = f[0], commercial = f[1], industrial = f[2])
    }

    fun setPasses(passes: Int) = nativeSetPasses(handle, passes)
    fun setSpeed(speed: Int) = nativeSetSpeed(handle, speed)
    fun setPaused(paused: Boolean) = nativeSetPaused(handle, paused)
    fun setSound(enabled: Boolean) = nativeSetSound(handle, enabled)

    val cityYear: Int get() = nativeGetYear(handle)
    val cityMonth: Int get() = nativeGetMonth(handle)
    val totalFunds: Long get() = nativeGetFunds(handle)
    val population: Long get() = nativeGetPopulation(handle)
    val cityName: String get() = nativeGetCityName(handle)

    fun destroy() {
        if (handle != 0L) {
            nativeDestroy(handle)
            handle = 0L
        }
    }

    private external fun nativeCreate(): Long
    private external fun nativeGenerateRandomCity(handle: Long)
    private external fun nativeSetGameLevelFunds(handle: Long, level: Int)
    private external fun nativeSetCityName(handle: Long, name: String)
    private external fun nativeGetTile(handle: Long, x: Int, y: Int): Int
    private external fun nativeCopyMap(handle: Long, dest: ShortArray)
    private external fun nativeLoadCity(handle: Long, path: String): Boolean
    private external fun nativeSaveCity(handle: Long, path: String)
    private external fun nativeSimTick(handle: Long)
    private external fun nativeDoTool(handle: Long, tool: Int, x: Int, y: Int): Int
    private external fun nativeQueryZone(handle: Long, x: Int, y: Int, out6: IntArray): Boolean
    private external fun nativeGetBudget(handle: Long, out6: LongArray, out3: FloatArray)
    private external fun nativeGetEvaluation(handle: Long, out12: IntArray)
    private external fun nativeGetDemands(handle: Long, out3: FloatArray)
    private external fun nativeSetTax(handle: Long, tax: Int)
    private external fun nativeSetFunding(handle: Long, road: Float, fire: Float, police: Float)
    private external fun nativeSetPasses(handle: Long, passes: Int)
    private external fun nativeSetSpeed(handle: Long, speed: Int)
    private external fun nativeSetPaused(handle: Long, paused: Boolean)
    private external fun nativeSetSound(handle: Long, enabled: Boolean)
    private external fun nativeGetYear(handle: Long): Int
    private external fun nativeGetMonth(handle: Long): Int
    private external fun nativeGetFunds(handle: Long): Long
    private external fun nativeGetPopulation(handle: Long): Long
    private external fun nativeGetCityName(handle: Long): String
    private external fun nativeDestroy(handle: Long)

    companion object {
        const val WORLD_W = 120
        const val WORLD_H = 100
        const val MAP_SIZE = WORLD_W * WORLD_H

        /** Index into a [copyMap] buffer for tile (x, y) — engine is column-major. */
        fun tileIndex(x: Int, y: Int): Int = x * WORLD_H + y

        init {
            System.loadLibrary("micropolis")
        }
    }
}
