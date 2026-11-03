package com.drowz.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Drowz palette — lifted directly from the landing page so app and marketing read as one
 * product. Grouped by role, not by hue, so usage sites stay semantic.
 */
object DrowzColors {
    // Ink (foreground text), softest → mutest
    val Ink = Color(0xFFECEAFB)
    val InkSoft = Color(0xFFB9B4D6)
    val InkMute = Color(0xFF837FA6)

    // Accents
    val Lavender = Color(0xFFC4B5FF)
    val Amber = Color(0xFFFFCF9E)

    // Surfaces
    val Space = Color(0xFF05060F)
    val SpaceRaised = Color(0xFF0B0C1A)

    // Hairlines / glass
    val Line = Color(0x24C4B5FF)          // rgba(196,181,255,.14)
    val Glass = Color(0x0BFFFFFF)         // rgba(255,255,255,.045)
    val GlassLine = Color(0x17FFFFFF)     // rgba(255,255,255,.09)

    // Celestial-scene stops (dusk → deep night), consumed by the night-sky Canvas.
    object Sky {
        val DuskTop = Color(0xFF59689F)
        val DuskMid = Color(0xFF8E6EA6)
        val DuskHorizon = Color(0xFFF0A868)
        val DuskLow = Color(0xFF20263F)

        val NightTop = Color(0xFF04060F)
        val NightMid = Color(0xFF0A0C22)
        val NightHorizon = Color(0xFF0B0D1C)
        val NightLow = Color(0xFF04060F)

        val Star = Color(0xFFEDEBFF)
        val Moon = Color(0xFFE9EBFF)
        val Window = Color(0xFFFFD48A)
        val MountainFar = Color(0xFF464B7C)
        val MountainNear = Color(0xFF31335A)
        val MountainFarNight = Color(0xFF0C1030)
        val MountainNearNight = Color(0xFF080A20)
        val CityDusk = Color(0xFF191B30)
        val CityNight = Color(0xFF05060F)
    }
}
