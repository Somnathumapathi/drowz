package com.drowz.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.drowz.app.ui.theme.DrowzColors
import com.drowz.app.ui.theme.Radii

/** All-caps, wide-tracked section label (the "eyebrow" from the landing page). */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = DrowzColors.Lavender) {
    Text(
        text = text.uppercase(),
        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
    )
}

/** Translucent "glass" panel — the app's primary content surface. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radii.card))
            .background(DrowzColors.Glass)
            .border(BorderStroke(1.dp, DrowzColors.GlassLine), RoundedCornerShape(Radii.card))
            .padding(padding),
    ) {
        content()
    }
}

/** Small rounded chip used to show which effects are enabled. */
@Composable
fun EffectChip(label: String, active: Boolean, modifier: Modifier = Modifier) {
    val border = if (active) DrowzColors.Line else DrowzColors.GlassLine
    val bg = if (active) DrowzColors.Lavender.copy(alpha = 0.10f) else Color.Transparent
    val content = if (active) DrowzColors.InkSoft else DrowzColors.InkMute
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Radii.chip))
            .background(bg)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(Radii.chip))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .alpha(if (active) 1f else 0.7f),
    ) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
            color = content,
        )
    }
}

/** Solid lavender primary action. */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        background = DrowzColors.Lavender,
        contentColor = Color(0xFF171334),
        border = null,
    )
}

/** Outlined secondary action. */
@Composable
fun GhostPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    PillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        background = Color.White.copy(alpha = 0.03f),
        contentColor = DrowzColors.Ink,
        border = BorderStroke(1.dp, DrowzColors.GlassLine),
    )
}

@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    background: Color,
    contentColor: Color,
    border: BorderStroke?,
) {
    val shape = RoundedCornerShape(Radii.button)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(if (enabled) background else background.copy(alpha = 0.4f))
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 15.dp)
            .alpha(if (enabled) 1f else 0.6f),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides contentColor) {
            Text(
                text = text,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
