package com.watchopolis.wear

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.wear.compose.material.MaterialTheme
import com.watchopolis.wear.engine.MessageText
import com.watchopolis.wear.engine.ZoneStatus
import com.watchopolis.wear.game.Game
import com.watchopolis.wear.game.SoundManager
import com.watchopolis.wear.ui.AboutScreen
import com.watchopolis.wear.ui.BudgetScreen
import com.watchopolis.wear.ui.CitiesScreen
import com.watchopolis.wear.ui.EvaluationScreen
import com.watchopolis.wear.ui.LoadCityScreen
import com.watchopolis.wear.ui.MapScreen
import com.watchopolis.wear.ui.MenuScreen
import com.watchopolis.wear.ui.NewCityScreen
import com.watchopolis.wear.ui.ScenarioScreen
import com.watchopolis.wear.ui.ZoneStatusScreen
import com.watchopolis.wear.ui.MenuTarget
import com.watchopolis.wear.ui.VgaTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** ~8 ticks/sec keeps the watch responsive without burning battery. */
private const val TICK_DELAY_MS = 120L

private enum class Screen { Map, Menu, Budget, Evaluation, Cities, NewCity, Scenarios, LoadCity, ZoneStatus, About }

/** Cycled by the second hardware button: paused -> normal -> fast -> super fast -> paused. */
private enum class GameSpeed(val label: String, val ticksPerInterval: Int) {
    PAUSED("Paused", 0),
    NORMAL("Normal", 1),
    FAST("Fast", 2),
    SUPER_FAST("Super Fast", 4),
}

private fun GameSpeed.next(): GameSpeed = when (this) {
    GameSpeed.PAUSED -> GameSpeed.NORMAL
    GameSpeed.NORMAL -> GameSpeed.FAST
    GameSpeed.FAST -> GameSpeed.SUPER_FAST
    GameSpeed.SUPER_FAST -> GameSpeed.PAUSED
}

class MainActivity : ComponentActivity() {
    /** Set by the UI; returns true if it handled the hardware key. */
    var onStemKey: ((Int) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MicropolisApp() }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (onStemKey?.invoke(keyCode) == true) return true
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
private fun MicropolisApp() {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val game = remember { Game() }
    val soundManager = remember { SoundManager(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var screen by remember { mutableStateOf(Screen.Cities) }
    var zoneStatus by remember { mutableStateOf<ZoneStatus?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var speed by remember { mutableStateOf(GameSpeed.NORMAL) }
    var speedToast by remember { mutableStateOf<String?>(null) }

    // Auto-dismiss the transient message banner.
    LaunchedEffect(message) {
        if (message != null) {
            delay(3500)
            message = null
        }
    }

    // Auto-dismiss the speed banner, except "Paused" which stays until unpaused.
    LaunchedEffect(speedToast, speed) {
        if (speedToast != null && speed != GameSpeed.PAUSED) {
            delay(3500)
            speedToast = null
        }
    }

    // Second hardware button (STEM_2) cycles simulation speed. The other STEM
    // buttons toggle the menu; crown press (STEM_PRIMARY) is usually reserved
    // by the system but we still try it.
    DisposableEffect(activity) {
        activity?.onStemKey = { code ->
            when (code) {
                KeyEvent.KEYCODE_STEM_2 -> {
                    speed = speed.next()
                    speedToast = speed.label
                    true
                }
                KeyEvent.KEYCODE_STEM_1,
                KeyEvent.KEYCODE_STEM_3,
                KeyEvent.KEYCODE_STEM_PRIMARY -> {
                    screen = if (screen == Screen.Map) Screen.Menu else Screen.Map
                    true
                }
                else -> false
            }
        }
        onDispose { activity?.onStemKey = null }
    }

    LaunchedEffect(game) {
        game.engine.soundListener = { _, sound, _, _ -> soundManager.play(sound) }
        game.engine.messageListener = { idx, _, _ -> MessageText.of(idx)?.let { message = it } }
        // Only tick while the app is actually on-screen (pauses in ambient /
        // when the wrist drops to the watch face) to save battery.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                // Any non-map screen (menus included) pauses the simulation; returning
                // to the map resumes at whatever speed was last selected.
                if (screen == Screen.Map) {
                    repeat(speed.ticksPerInterval) { game.tick() }
                }
                delay(TICK_DELAY_MS)
            }
        }
    }
    DisposableEffect(game) {
        onDispose {
            game.destroy()
            soundManager.release()
        }
    }

    MaterialTheme(typography = VgaTypography) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Map stays composed underneath so its camera/tool state survives.
            MapScreen(
                game = game,
                onOpenMenu = { screen = Screen.Menu },
                onQuery = { zoneStatus = it; screen = Screen.ZoneStatus },
                active = screen == Screen.Map,
                message = speedToast ?: message,
            )

            when (screen) {
                Screen.Map -> Unit
                Screen.Menu -> MenuScreen(
                    onSelect = { target ->
                        screen = when (target) {
                            MenuTarget.BUDGET -> Screen.Budget
                            MenuTarget.EVALUATION -> Screen.Evaluation
                            MenuTarget.CITIES -> Screen.Cities
                            MenuTarget.ABOUT -> Screen.About
                        }
                    },
                )
                Screen.Budget -> BudgetScreen(engine = game.engine, onChanged = { game.refresh() })
                Screen.Evaluation -> EvaluationScreen(engine = game.engine)
                Screen.Cities -> CitiesScreen(
                    game = game,
                    onNewRandomCity = { screen = Screen.NewCity },
                    onScenario = { screen = Screen.Scenarios },
                    onLoad = { screen = Screen.LoadCity },
                )
                Screen.Scenarios -> ScenarioScreen(
                    game = game,
                    onStarted = { screen = Screen.Map },
                )
                Screen.LoadCity -> LoadCityScreen(
                    game = game,
                    onLoaded = { screen = Screen.Map },
                )
                Screen.NewCity -> NewCityScreen(game = game, onStarted = { screen = Screen.Map })
                Screen.ZoneStatus -> zoneStatus?.let { ZoneStatusScreen(it) }
                Screen.About -> AboutScreen()
            }
        }
    }

    // System back navigation:
    //   Scenarios / LoadCity -> Cities
    //   Cities (city loaded) -> Menu
    //   Cities (no city loaded) -> exit app (handler disabled)
    //   Menu / ZoneStatus -> Map
    //   anything else -> Menu
    BackHandler(enabled = screen != Screen.Map && (screen != Screen.Cities || game.currentCity.isNotEmpty())) {
        screen = when (screen) {
            Screen.Scenarios, Screen.LoadCity -> Screen.Cities
            Screen.Menu, Screen.ZoneStatus -> Screen.Map
            else -> Screen.Menu
        }
    }
}
