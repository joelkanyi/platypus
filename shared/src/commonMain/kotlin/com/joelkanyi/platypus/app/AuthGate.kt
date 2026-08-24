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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.joelkanyi.platypus.domain.model.AuthStatus
import com.joelkanyi.platypus.feature.auth.signin.AddAccountSheet
import com.joelkanyi.platypus.feature.auth.signin.SignInScreen
import com.joelkanyi.platypus.navigation.PlatypusShell
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun AuthGate() {
    val dependencies = LocalPlatypusDependencies.current
    val status by dependencies.authRepository.status.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        dependencies.authRepository.restore()
    }

    var addingAccount by rememberSaveable { mutableStateOf(false) }
    val accountActions = remember { AccountActions(addAccount = { addingAccount = true }) }

    CompositionLocalProvider(LocalAccountActions provides accountActions) {
        when (status) {
            AuthStatus.Unknown -> LoadingGate()

            AuthStatus.Locked -> LoadingGate()

            AuthStatus.SignedOut -> SessionScope {
                SignInScreen(
                    authRepository = dependencies.authRepository,
                    oauthDeepLinks = dependencies.oauthDeepLinks,
                    openUrl = dependencies::openUrl,
                )
            }

            AuthStatus.SignedIn -> SessionScope {
                PlatypusShell()
                if (addingAccount) {
                    AddAccountSheet(
                        authRepository = dependencies.authRepository,
                        oauthDeepLinks = dependencies.oauthDeepLinks,
                        openUrl = dependencies::openUrl,
                        onDismiss = { addingAccount = false },
                    )
                }
            }
        }
    }
}

/**
 * Binds every [androidx.lifecycle.viewmodel.compose.viewModel] created inside [content] to a
 * ViewModelStore tied to this auth session. Leaving the branch (a sign-out, or the sign-in flow
 * completing) clears the store, so no screen keeps another session's state.
 */
@Composable
private fun SessionScope(content: @Composable () -> Unit) {
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}

@Composable
private fun LoadingGate() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        JengaText(text = "Loading...", color = JengaTheme.colors.textSecondary)
    }
}
