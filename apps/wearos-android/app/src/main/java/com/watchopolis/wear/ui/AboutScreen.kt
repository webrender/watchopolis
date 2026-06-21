package com.watchopolis.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text

// Single source of truth for the published source-code location (GPLv3 requires
// offering source to users).
const val SOURCE_URL = "github.com/webrender/watchopolis"

/**
 * Required legal/attribution screen. The engine is GPLv3 (EA/Maxis) and "Micropolis"
 * is a trademark licensed under the Micropolis Public Name License, both of which
 * require the notices below to be shown to users.
 */
@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .crownScroll(scroll)
            .verticalScroll(scroll)
            .padding(horizontal = 18.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ListHeader { Text("About") }

        Para("Watchopolis", color = Color.White, size = 16, weight = FontWeight.Bold)
        Para("Version 1.0", color = Color.Gray, size = 11)

        Para(
            "A Wear OS port of Micropolis, the open-source release of the classic " +
                "city-building simulator.",
        )

        Para(
            "SimCity / Micropolis © 1989–2007 Electronic Arts Inc. " +
                "Not affiliated with, sponsored, or endorsed by Electronic Arts.",
            color = Color.Gray,
            size = 10,
        )

        Para(
            "Licensed under the GNU General Public License v3.0. This program comes " +
                "with absolutely no warranty.",
            color = Color.Gray,
            size = 10,
        )
        Para("Source code:", color = Color.Gray, size = 10)
        Para(SOURCE_URL, color = Color(0xFF9EC9FF), size = 10)

        Para(
            "Micropolis is a registered trademark of Micropolis Corporation " +
                "(Micropolis GmbH) and is licensed here as a courtesy of the owner " +
                "under the Micropolis Public Name License.",
            color = Color.Gray,
            size = 10,
        )
        Para("micropolis.com", color = Color(0xFF9EC9FF), size = 10)

        Para(
            "Tilesets and sound effects are from the Micropolis GPL release.",
            color = Color.Gray,
            size = 10,
        )

        Para(
            "VGA text-mode font from the Ultimate Oldschool PC Font Pack by VileR, " +
                "licensed under CC BY-SA 4.0.",
            color = Color.Gray,
            size = 10,
        )
        Para("int10h.org/oldschool-pc-fonts", color = Color(0xFF9EC9FF), size = 10)
    }
}

@Composable
private fun Para(
    text: String,
    color: Color = Color.White,
    size: Int = 12,
    weight: FontWeight = FontWeight.Normal,
) {
    Text(
        text = text,
        color = color,
        fontSize = size.sp,
        fontWeight = weight,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
