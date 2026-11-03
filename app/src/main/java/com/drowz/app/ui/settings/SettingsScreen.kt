package com.drowz.app.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.drowz.app.domain.effects.EffectCapabilities
import com.drowz.app.domain.model.EffectSettings
import com.drowz.app.domain.model.SleepSettings
import com.drowz.app.domain.model.TimeOfDay
import com.drowz.app.platform.service.SleepModeService
import com.drowz.app.ui.DrowzViewModel
import com.drowz.app.ui.components.CodeBlock
import com.drowz.app.ui.components.DayPicker
import com.drowz.app.ui.components.DrowzSwitch
import com.drowz.app.ui.components.DrowzTopBar
import com.drowz.app.ui.components.Eyebrow
import com.drowz.app.ui.components.GhostPillButton
import com.drowz.app.ui.components.GlassCard
import com.drowz.app.ui.components.PrimaryPillButton
import com.drowz.app.ui.components.SettingRow
import com.drowz.app.ui.components.Stepper
import com.drowz.app.ui.home.to12h
import com.drowz.app.ui.theme.DrowzColors

@Composable
fun SettingsScreen(
    settings: SleepSettings,
    capabilities: EffectCapabilities,
    vm: DrowzViewModel,
    onBack: () -> Unit,
    onOpenSetup: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DrowzColors.Space)
            .systemBarsPadding()
            .padding(horizontal = 22.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        DrowzTopBar(title = "Settings", onBack = onBack)
        Spacer(Modifier.height(8.dp))

        // --- Schedule ---
        Eyebrow("Schedule")
        Spacer(Modifier.height(10.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column {
                SettingRow(
                    title = "Bedtime",
                    onClick = { pickTime(context, settings.bedtime, vm::setBedtime) },
                    trailing = { TimeValue(settings.bedtime) },
                )
                SettingRow(
                    title = "Wake",
                    onClick = { pickTime(context, settings.wakeTime, vm::setWakeTime) },
                    trailing = { TimeValue(settings.wakeTime) },
                )
                Spacer(Modifier.height(12.dp))
                Text("REPEAT", style = MaterialTheme.typography.labelSmall, color = DrowzColors.InkMute)
                Spacer(Modifier.height(10.dp))
                DayPicker(selected = settings.activeDays, onToggle = vm::toggleDay)
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- Effects ---
        Eyebrow("Effects")
        Spacer(Modifier.height(10.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column {
                val e = settings.effects
                EffectToggle("Dim the screen", e.dimScreen, needsPermission = e.dimScreen && !capabilities.canDim) {
                    vm.updateEffects { it.copy(dimScreen = !it.dimScreen) }
                }
                EffectToggle(
                    "Extra dim",
                    e.darkOverlay,
                    needsPermission = e.darkOverlay && !capabilities.canDarken,
                    subtitle = "A dark overlay to go below the screen's minimum brightness",
                ) { vm.updateEffects { it.copy(darkOverlay = !it.darkOverlay) } }
                EffectToggle(
                    "True grayscale",
                    e.grayscale,
                    needsPermission = e.grayscale && !capabilities.canGrayscale,
                    subtitle = "Power-user — needs a one-time ADB grant (see Setup)",
                ) { vm.updateEffects { it.copy(grayscale = !it.grayscale) } }
                EffectToggle("Do Not Disturb", e.doNotDisturb, needsPermission = e.doNotDisturb && !capabilities.canDnd) {
                    vm.updateEffects { it.copy(doNotDisturb = !it.doNotDisturb) }
                }
                EffectToggle("Lower the volume", e.lowerVolume) {
                    vm.updateEffects { it.copy(lowerVolume = !it.lowerVolume) }
                }
                EffectToggle("Soundscape", e.soundscape) {
                    vm.updateEffects { it.copy(soundscape = !it.soundscape) }
                }
                Spacer(Modifier.height(8.dp))
                SettingRow(
                    title = "Wind-down length",
                    subtitle = "How long the soundscape plays before fading out",
                    trailing = {
                        Stepper(
                            value = e.windDownMinutes,
                            range = 5..60,
                            step = 5,
                            onChange = { m -> vm.updateEffects { it.copy(windDownMinutes = m) } },
                            format = { "$it min" },
                        )
                    },
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // --- Permissions & test ---
        Eyebrow("Setup & testing")
        Spacer(Modifier.height(10.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Column {
                SettingRow(
                    title = "Permissions & setup",
                    subtitle = "Grant the permissions each effect needs",
                    onClick = onOpenSetup,
                    trailing = { Text("→", style = MaterialTheme.typography.titleLarge, color = DrowzColors.InkSoft) },
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Try it on this device — starts the real wind-down now and restores everything when you stop.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DrowzColors.InkMute,
                )
                Spacer(Modifier.height(12.dp))
                PrimaryPillButton("Start wind-down now", { SleepModeService.start(context) }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                GhostPillButton("Stop & restore", { SleepModeService.stop(context) }, Modifier.fillMaxWidth())
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun EffectToggle(
    title: String,
    checked: Boolean,
    needsPermission: Boolean = false,
    subtitle: String? = null,
    onToggle: () -> Unit,
) {
    val hint = when {
        needsPermission -> "Needs permission — open Setup"
        else -> subtitle
    }
    SettingRow(
        title = title,
        subtitle = hint,
        trailing = { DrowzSwitch(checked = checked, onCheckedChange = { onToggle() }) },
    )
}

@Composable
private fun TimeValue(time: TimeOfDay) {
    Text(time.to12h(), style = MaterialTheme.typography.headlineMedium, color = DrowzColors.Ink)
}

private fun pickTime(context: Context, initial: TimeOfDay, onPick: (Int, Int) -> Unit) {
    android.app.TimePickerDialog(
        context,
        { _, hour, minute -> onPick(hour, minute) },
        initial.hour, initial.minute, false,
    ).show()
}
