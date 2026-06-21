package com.watchopolis.wear.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock

/**
 * Plays engine sound effects with SoundPool. The engine's sound names match the
 * bundled asset file names (e.g. "Siren" -> assets/sounds/Siren.mp3).
 */
class SoundManager(context: Context) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundIds = HashMap<String, Int>()
    private val lastPlayed = HashMap<String, Long>()

    init {
        val assets = context.assets
        SOUNDS.forEach { name ->
            runCatching {
                assets.openFd("sounds/$name.mp3").use { afd ->
                    soundIds[name] = pool.load(afd, 1)
                }
            }
        }
    }

    fun play(sound: String) {
        val id = soundIds[sound] ?: return
        // Throttle repeats of the same effect (traffic honks can be frequent).
        val now = SystemClock.uptimeMillis()
        if (now - (lastPlayed[sound] ?: 0L) < THROTTLE_MS) return
        lastPlayed[sound] = now
        pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun release() = pool.release()

    companion object {
        private const val THROTTLE_MS = 400L
        private val SOUNDS = listOf(
            "ExplosionHigh", "ExplosionLow", "FogHornLow", "HeavyTraffic",
            "HonkHonkHigh", "HonkHonkLow", "HonkHonkMed", "Monster",
            "Siren", "Sorry", "UhUh",
        )
    }
}
