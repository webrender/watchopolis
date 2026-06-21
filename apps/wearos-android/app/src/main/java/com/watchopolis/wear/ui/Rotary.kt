package com.watchopolis.wear.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent

/**
 * Lets the rotating crown scroll a [ScrollState]. Requests focus when the screen
 * appears so rotary events are delivered to this composable.
 */
@Composable
fun Modifier.crownScroll(scrollState: ScrollState): Modifier {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    return this
        .onRotaryScrollEvent {
            scrollState.dispatchRawDelta(it.verticalScrollPixels)
            true
        }
        .focusRequester(focusRequester)
        .focusable()
}
