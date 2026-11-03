package com.drowz.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drowz.app.domain.model.Weekday
import com.drowz.app.ui.theme.DrowzColors

/** Top bar with a text back affordance (no icon dependency) + optional trailing content. */
@Composable
fun DrowzTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Text(
                text = "←",
                style = MaterialTheme.typography.titleLarge,
                color = DrowzColors.Ink,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onBack).padding(8.dp),
            )
            Spacer(Modifier.size(6.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            color = DrowzColors.Ink,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/** A label (+ optional subtitle) with trailing control; optionally the whole row is tappable. */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = DrowzColors.Ink)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = DrowzColors.InkMute)
            }
        }
        Spacer(Modifier.size(12.dp))
        trailing()
    }
}

@Composable
fun DrowzSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = DrowzColors.Space,
            checkedTrackColor = DrowzColors.Lavender,
            checkedBorderColor = DrowzColors.Lavender,
            uncheckedThumbColor = DrowzColors.InkMute,
            uncheckedTrackColor = DrowzColors.SpaceRaised,
            uncheckedBorderColor = DrowzColors.GlassLine,
        ),
    )
}

/** Seven circular day toggles (M T W T F S S). */
@Composable
fun DayPicker(selected: Set<Weekday>, onToggle: (Weekday) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Weekday.entries.forEach { day ->
            val on = day in selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (on) DrowzColors.Lavender.copy(alpha = 0.16f) else Color.Transparent)
                    .border(
                        BorderStroke(1.dp, if (on) DrowzColors.Lavender else DrowzColors.GlassLine),
                        CircleShape,
                    )
                    .clickable { onToggle(day) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.name.first().toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) DrowzColors.Ink else DrowzColors.InkMute,
                )
            }
        }
    }
}

/** A −/value/+ stepper. */
@Composable
fun Stepper(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    format: (Int) -> String,
    step: Int = 1,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StepButton("−") { onChange((value - step).coerceIn(range.first, range.last)) }
        Text(
            text = format(value),
            style = MaterialTheme.typography.titleLarge,
            color = DrowzColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 72.dp),
        )
        StepButton("+") { onChange((value + step).coerceIn(range.first, range.last)) }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .border(BorderStroke(1.dp, DrowzColors.GlassLine), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge, color = DrowzColors.Ink)
    }
}

/** A permission row: title, description, status, and a grant button (hidden once granted). */
@Composable
fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
    GlassCard(modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = DrowzColors.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = DrowzColors.InkSoft)
                }
                Spacer(Modifier.size(12.dp))
                Text(
                    text = if (granted) "Ready" else "Off",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (granted) DrowzColors.Lavender else DrowzColors.InkMute,
                )
            }
            if (!granted) {
                Spacer(Modifier.height(14.dp))
                GhostPillButton(actionLabel, onAction, Modifier.fillMaxWidth())
            }
            if (note != null) {
                Spacer(Modifier.height(10.dp))
                Text(note, style = MaterialTheme.typography.bodyMedium, color = DrowzColors.InkMute)
            }
        }
    }
}

/** Monospace box for the ADB grayscale command. */
@Composable
fun CodeBlock(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp),
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = DrowzColors.Amber,
        )
    }
}
