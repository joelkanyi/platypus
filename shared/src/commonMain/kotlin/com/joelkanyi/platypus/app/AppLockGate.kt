/*
 * Copyright (C) 2026 Joel Kanyi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.joelkanyi.platypus.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.joelkanyi.jenga.component.button.JengaButton
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun AppLockGate(content: @Composable () -> Unit) {
    val dependencies = LocalPlatypusDependencies.current
    val settings by dependencies.settingsStore.settings.collectAsStateWithLifecycle()
    val available by produceState(false, settings.appLockEnabled) {
        value = settings.appLockEnabled && dependencies.biometrics.isAvailable()
    }

    if (!settings.appLockEnabled || !available) {
        content()
        return
    }

    var unlocked by rememberSaveable { mutableStateOf(false) }
    var authenticating by remember { mutableStateOf(false) }
    var authJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    val prompt: () -> Unit = {
        if (!authenticating) {
            authenticating = true
            authJob = scope.launch {
                try {
                    if (dependencies.biometrics.authenticate("Unlock Platypus to continue")) {
                        unlocked = true
                    }
                } finally {
                    authenticating = false
                }
            }
        }
    }
    val currentPrompt by rememberUpdatedState(prompt)

    // Prompt only while the app is actually resumed. Firing the biometric prompt
    // as the app is stopping (e.g. relocking on ON_STOP) leaves it unable to show
    // and the auth call hanging, which stranded the lock screen on a spinner.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    unlocked = false
                    authJob?.cancel()
                    authenticating = false
                }

                Lifecycle.Event.ON_RESUME -> if (!unlocked && !authenticating) currentPrompt()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Covers a cold start where composition settles after ON_RESUME was dispatched.
    LaunchedEffect(Unit) {
        if (!unlocked && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) currentPrompt()
    }

    if (unlocked) {
        content()
        return
    }
    LockScreen(onUnlock = prompt, authenticating = authenticating)
}

@Composable
private fun LockScreen(onUnlock: () -> Unit, authenticating: Boolean) {
    val spacing = JengaTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        JengaText(
            text = "Platypus is locked",
            style = JengaTheme.typography.headingMedium,
            color = JengaTheme.colors.textPrimary,
        )
        JengaText(
            text = "Unlock with your fingerprint, face, or device credential to continue.",
            style = JengaTheme.typography.bodyMedium,
            color = JengaTheme.colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.sm, bottom = spacing.xl),
        )
        JengaButton(
            text = "Unlock",
            onClick = onUnlock,
            enabled = !authenticating,
            loading = authenticating,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}
