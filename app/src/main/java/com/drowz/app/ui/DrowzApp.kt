package com.drowz.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.drowz.app.di.AppContainer
import com.drowz.app.ui.home.HomeScreen
import com.drowz.app.ui.settings.SettingsScreen
import com.drowz.app.ui.setup.SetupScreen

private enum class Screen { Home, Settings, Setup }

/**
 * Root of the UI: one app-scoped [DrowzViewModel], simple state-based navigation, and a
 * resume hook that re-reads permission state whenever the user returns from a system
 * settings screen. First run opens Setup once (based on the persisted flag).
 */
@Composable
fun DrowzApp(container: AppContainer) {
    val vm: DrowzViewModel = viewModel(factory = DrowzViewModel.factory(container))
    val state by vm.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshCapabilities() }

    var screen by remember { mutableStateOf(Screen.Home) }
    var routed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!routed) {
            if (!vm.hasCompletedSetup()) screen = Screen.Setup
            routed = true
        }
    }

    when (screen) {
        Screen.Home -> HomeScreen(
            state = state,
            onPreview = vm::startPreview,
            onRestore = vm::restorePreview,
            onToggleArmed = vm::setArmed,
            onOpenSettings = { screen = Screen.Settings },
            onOpenSetup = { screen = Screen.Setup },
        )

        Screen.Settings -> {
            BackHandler { screen = Screen.Home }
            SettingsScreen(
                settings = state.settings,
                capabilities = state.capabilities,
                vm = vm,
                onBack = { screen = Screen.Home },
                onOpenSetup = { screen = Screen.Setup },
            )
        }

        Screen.Setup -> {
            BackHandler { screen = Screen.Home }
            SetupScreen(
                capabilities = state.capabilities,
                onFinish = { vm.markSetupComplete(); screen = Screen.Home },
                onBack = { screen = Screen.Home },
            )
        }
    }
}
