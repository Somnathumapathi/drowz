package com.drowz.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drowz.app.domain.model.EffectSettings
import com.drowz.app.domain.model.SleepSettings
import com.drowz.app.domain.model.TimeOfDay
import com.drowz.app.domain.winddown.WindDownState
import com.drowz.app.ui.DrowzUiState
import com.drowz.app.ui.components.EffectChip
import com.drowz.app.ui.components.Eyebrow
import com.drowz.app.ui.components.GhostPillButton
import com.drowz.app.ui.components.GlassCard
import com.drowz.app.ui.components.NightSky
import com.drowz.app.ui.components.PrimaryPillButton
import com.drowz.app.ui.theme.DrowzColors
import com.drowz.app.ui.theme.DrowzTheme

@Composable
fun HomeScreen(
    state: DrowzUiState,
    onPreview: () -> Unit,
    onRestore: () -> Unit,
    onToggleArmed: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wd = state.windDown

    Box(modifier = modifier.fillMaxSize().background(DrowzColors.Space)) {
        NightSky(progress = wd.progress, modifier = Modifier.fillMaxSize())

        // In-app preview of the effects (kept behind the controls so they stay legible):
        // the deeper brightness dim, then the neutral dark overlay stacked on top.
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = (1f - wd.brightness) * 0.72f)))
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = wd.overlay)))
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.35f to Color.Transparent,
                    1f to DrowzColors.Space.copy(alpha = 0.62f),
                ),
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 22.dp, vertical = 16.dp),
        ) {
            TopBar(armed = state.settings.enabled, onOpenSettings = onOpenSettings)

            Spacer(Modifier.weight(1f))

            if (needsSetup(state)) {
                SetupBanner(onClick = onOpenSetup)
                Spacer(Modifier.height(16.dp))
            }

            StatusBlock(windDown = wd)
            Spacer(Modifier.height(20.dp))
            ScheduleCard(settings = state.settings)
            Spacer(Modifier.height(24.dp))
            Actions(
                isPreviewing = state.isPreviewing,
                armed = state.settings.enabled,
                onPreview = onPreview,
                onRestore = onRestore,
                onToggleArmed = onToggleArmed,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (state.isPreviewing) {
                    "This is a preview — nothing on your phone changed."
                } else {
                    "Preview the ritual, then arm it for tonight."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = DrowzColors.InkMute,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}

/** Any enabled effect whose permission isn't granted → nudge the user to finish setup. */
private fun needsSetup(state: DrowzUiState): Boolean {
    val e = state.settings.effects
    val c = state.capabilities
    return (e.dimScreen && !c.canDim) ||
        (e.darkOverlay && !c.canDarken) ||
        (e.doNotDisturb && !c.canDnd) ||
        (e.grayscale && !c.canGrayscale)
}

@Composable
private fun TopBar(armed: Boolean, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("☾", color = DrowzColors.Lavender, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(8.dp))
            Text(
                "drowz",
                color = DrowzColors.Ink,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (armed) {
                EffectChip(label = "Armed", active = true)
                Spacer(Modifier.width(10.dp))
            }
            Text(
                "⚙",
                color = DrowzColors.InkSoft,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onOpenSettings).padding(4.dp),
            )
        }
    }
}

@Composable
private fun SetupBanner(onClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Finish setup",
                    style = MaterialTheme.typography.titleLarge,
                    color = DrowzColors.Amber,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Some effects still need permission to run at bedtime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DrowzColors.InkSoft,
                )
            }
            Text("→", style = MaterialTheme.typography.titleLarge, color = DrowzColors.Amber)
        }
    }
}

@Composable
private fun StatusBlock(windDown: WindDownState) {
    Column {
        Eyebrow(text = "Tonight")
        Spacer(Modifier.height(10.dp))
        Text(windDown.phase.title, style = MaterialTheme.typography.displayLarge, color = DrowzColors.Ink)
        Spacer(Modifier.height(6.dp))
        Text(windDown.phase.caption, style = MaterialTheme.typography.bodyLarge, color = DrowzColors.InkSoft)
    }
}

@Composable
private fun ScheduleCard(settings: SleepSettings) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                TimeColumn("Bedtime", settings.bedtime, Modifier.weight(1f))
                TimeColumn("Wake", settings.wakeTime, Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            EffectChips(settings.effects)
        }
    }
}

@Composable
private fun TimeColumn(label: String, time: TimeOfDay, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = DrowzColors.InkMute)
        Spacer(Modifier.height(6.dp))
        Text(time.to12h(), style = MaterialTheme.typography.headlineMedium, color = DrowzColors.Ink)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EffectChips(effects: EffectSettings) {
    val chips = buildList {
        if (effects.dimScreen) add("Dim")
        when {
            effects.grayscale -> add("Grayscale")
            effects.darkOverlay -> add("Darker")
        }
        if (effects.doNotDisturb) add("Do Not Disturb")
        if (effects.lowerVolume) add("Quiet")
        if (effects.soundscape) add("Soundscape")
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { EffectChip(label = it, active = true) }
    }
}

@Composable
private fun Actions(
    isPreviewing: Boolean,
    armed: Boolean,
    onPreview: () -> Unit,
    onRestore: () -> Unit,
    onToggleArmed: (Boolean) -> Unit,
) {
    if (isPreviewing) {
        PrimaryPillButton("I'm awake — restore", onRestore, Modifier.fillMaxWidth())
    } else {
        Column {
            PrimaryPillButton("Preview wind-down", onPreview, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            GhostPillButton(
                text = if (armed) "Disarm" else "Arm for tonight",
                onClick = { onToggleArmed(!armed) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 12-hour display formatting kept in the UI layer (domain stays locale-free). */
internal fun TimeOfDay.to12h(): String {
    val period = if (hour < 12) "AM" else "PM"
    val h12 = when (val h = hour % 12) { 0 -> 12; else -> h }
    return "$h12:${minute.toString().padStart(2, '0')} $period"
}

@Preview(showBackground = true, backgroundColor = 0xFF05060F, heightDp = 820)
@Composable
private fun HomePreview() {
    DrowzTheme {
        HomeScreen(
            state = DrowzUiState.Initial.copy(
                settings = SleepSettings(enabled = true),
                windDown = WindDownState.Awake.copy(progress = 0.25f),
            ),
            onPreview = {}, onRestore = {}, onToggleArmed = {}, onOpenSettings = {}, onOpenSetup = {},
        )
    }
}
