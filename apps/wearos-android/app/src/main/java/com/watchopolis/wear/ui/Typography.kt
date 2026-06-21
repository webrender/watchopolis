package com.watchopolis.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.wear.compose.material.Typography
import com.watchopolis.wear.R

/**
 * IBM PC VGA BIOS text-mode font (CP437 8x16), from VileR's "Ultimate Oldschool
 * PC Font Pack" (int10h.org), licensed CC BY-SA 4.0. See About screen for credit.
 */
val VgaFontFamily = FontFamily(Font(R.font.vga))

val VgaTypography: Typography
    @Composable get() = Typography(defaultFontFamily = VgaFontFamily)
