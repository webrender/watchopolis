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
import com.watchopolis.wear.game.Game
import com.watchopolis.wear.game.SoundManager
import com.watchopolis.wear.ui.AboutScreen
import com.watchopolis.wear.ui.BudgetScreen
import com.watchopolis.wear.ui.CitiesScreen
import com.watchopolis.wear.ui.EvaluationScreen
import com.watchopolis.wear.ui.MapScreen
import com.watchopolis.wear.ui.MenuScreen
import com.watchopolis.wear.ui.NewCityScreen
import com.watchopolis.wear.ui.MenuTarget
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** ~8 ticks/sec keeps the watch responsive without burning battery. */
private const val TICK_DELAY_MS = 120L

private enum class Screen { Map, Menu, Budget, Evaluation, Cities, NewCity, About }

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
    var screen by remember { mutableStateOf(Screen.Map) }
    var message by remember { mutableStateOf<String?>(null) }

    // Auto-dismiss the transient message banner.
    LaunchedEffect(message) {
        if (message != null) {
            delay(3500)
            message = null
        }
    }

    // Second hardware button (STEM) toggles the menu. Crown press (STEM_PRIMARY)
    // is usually reserved by the system; we still try it.
    DisposableEffect(activity) {
        activity?.onStemKey = { code ->
            when (code) {
                KeyEvent.KEYCODE_STEM_1,
                KeyEvent.KEYCODE_STEM_2,
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
        game.start(context, "haight.cty")
        // Only tick while the app is actually on-screen (pauses in ambient /
        // when the wrist drops to the watch face) to save battery.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                game.tick()
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

    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Map stays composed underneath so its camera/tool state survives.
            MapScreen(
                game = game,
                onOpenMenu = { screen = Screen.Menu },
                active = screen == Screen.Map,
                message = message,
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
                    onLoaded = { screen = Screen.Map },
                    onNewCity = { screen = Screen.NewCity },
                )
                Screen.NewCity -> NewCityScreen(game = game, onStarted = { screen = Screen.Map })
                Screen.About -> AboutScreen()
            }
        }
    }

    // System back: secondary screen -> menu -> map.
    BackHandler(enabled = screen != Screen.Map) {
        screen = if (screen == Screen.Menu) Screen.Map else Screen.Menu
    }
}
