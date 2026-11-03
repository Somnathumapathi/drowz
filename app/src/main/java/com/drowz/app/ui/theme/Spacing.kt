package com.drowz.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An 8pt spacing scale, exposed via CompositionLocal so components read spacing
 * semantically (`spacing.lg`) instead of sprinkling magic dp values.
 */
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
    val screen: Dp = 22.dp,   // default horizontal screen padding
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

/** Corner radii, kept in one place for consistency across cards/chips/buttons. */
object Radii {
    val chip: Dp = 999.dp
    val button: Dp = 999.dp
    val card: Dp = 22.dp
    val cardSmall: Dp = 16.dp
}
