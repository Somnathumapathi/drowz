package com.drowz.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Drowz is a night app: it is always dark. We map the palette onto a Material3
 * [darkColorScheme] so stock M3 components inherit sane colors, while bespoke surfaces
 * pull straight from [DrowzColors].
 */
private val DrowzColorScheme = darkColorScheme(
    primary = DrowzColors.Lavender,
    onPrimary = DrowzColors.Space,
    secondary = DrowzColors.Amber,
    onSecondary = DrowzColors.Space,
    background = DrowzColors.Space,
    onBackground = DrowzColors.Ink,
    surface = DrowzColors.SpaceRaised,
    onSurface = DrowzColors.Ink,
    surfaceVariant = DrowzColors.SpaceRaised,
    onSurfaceVariant = DrowzColors.InkSoft,
    outline = DrowzColors.InkMute,
    error = DrowzColors.Amber,
)

@Composable
fun DrowzTheme(
    // Present for API symmetry; Drowz intentionally ignores light mode.
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = DrowzColorScheme,
            typography = DrowzTypography,
            content = content,
        )
    }
}
