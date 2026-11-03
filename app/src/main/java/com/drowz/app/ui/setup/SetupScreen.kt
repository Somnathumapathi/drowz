package com.drowz.app.ui.setup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.drowz.app.domain.effects.EffectCapabilities
import com.drowz.app.platform.effects.SystemPermissions
import com.drowz.app.ui.components.CodeBlock
import com.drowz.app.ui.components.DrowzTopBar
import com.drowz.app.ui.components.GlassCard
import com.drowz.app.ui.components.PermissionCard
import com.drowz.app.ui.components.PrimaryPillButton
import com.drowz.app.ui.theme.DrowzColors

@Composable
fun SetupScreen(
    capabilities: EffectCapabilities,
    onFinish: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    // Re-read permission state each time we return from a system settings screen.
    var recheck by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { recheck++ }

    val notifNeeded = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val notifGranted = remember(recheck) {
        !notifNeeded || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
    val exactGranted = remember(recheck) { SystemPermissions.canScheduleExactAlarms(context) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { recheck++ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DrowzColors.Space)
            .systemBarsPadding()
            .padding(horizontal = 22.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        DrowzTopBar(title = "Set up Drowz", onBack = onBack)
        Spacer(Modifier.height(8.dp))
        Text(
            "Drowz nudges a few system settings at bedtime, then puts them all back by " +
                "morning. Grant what you like — everything is optional, and Drowz simply skips " +
                "whatever you skip.",
            style = MaterialTheme.typography.bodyLarge,
            color = DrowzColors.InkSoft,
        )
        Spacer(Modifier.height(20.dp))

        if (notifNeeded) {
            PermissionCard(
                title = "Bedtime reminder",
                description = "Show the gentle wind-down notification when it's time.",
                granted = notifGranted,
                actionLabel = "Allow notifications",
                onAction = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
            )
            Spacer(Modifier.height(12.dp))
        }

        PermissionCard(
            title = "Do Not Disturb",
            description = "Draw the curtains — silence calls and notifications overnight.",
            granted = capabilities.canDnd,
            actionLabel = "Grant access",
            onAction = { context.startActivity(SystemPermissions.dndAccessIntent()) },
        )
        Spacer(Modifier.height(12.dp))

        PermissionCard(
            title = "Dim the screen",
            description = "Lower your brightness on a slow ramp as you wind down.",
            granted = capabilities.canDim,
            actionLabel = "Grant access",
            onAction = { context.startActivity(SystemPermissions.writeSettingsIntent(context)) },
        )
        Spacer(Modifier.height(12.dp))

        PermissionCard(
            title = "Extra dim",
            description = "Lay a dark overlay over the screen to dim it below the panel's minimum.",
            granted = capabilities.canDarken,
            actionLabel = "Allow overlay",
            onAction = { context.startActivity(SystemPermissions.overlayIntent(context)) },
        )
        Spacer(Modifier.height(12.dp))

        PermissionCard(
            title = "Exact bedtime",
            description = "Fire precisely at your bedtime, even while the phone is idle.",
            granted = exactGranted,
            actionLabel = "Grant access",
            onAction = { context.startActivity(SystemPermissions.exactAlarmIntent(context)) },
        )
        Spacer(Modifier.height(12.dp))

        GrayscaleCard(granted = capabilities.canGrayscale)

        Spacer(Modifier.height(24.dp))
        PrimaryPillButton("Finish setup", onFinish, Modifier.fillMaxWidth())
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun GrayscaleCard(granted: Boolean) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("True grayscale", style = MaterialTheme.typography.titleLarge, color = DrowzColors.Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Power-user: drains the whole phone to black & white. Optional.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DrowzColors.InkSoft,
                    )
                }
                Text(
                    text = if (granted) "Ready" else "Off",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (granted) DrowzColors.Lavender else DrowzColors.InkMute,
                )
            }
            if (!granted) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Android reserves grayscale for the system, so it needs a one-time command " +
                        "over ADB from a computer:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DrowzColors.InkMute,
                )
                Spacer(Modifier.height(10.dp))
                CodeBlock("adb shell pm grant com.drowz.app \\\n  android.permission.WRITE_SECURE_SETTINGS")
            }
        }
    }
}
