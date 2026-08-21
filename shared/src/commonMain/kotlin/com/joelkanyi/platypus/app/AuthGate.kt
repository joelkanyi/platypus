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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joelkanyi.platypus.domain.model.AuthStatus
import com.joelkanyi.platypus.domain.model.Workspace
import com.joelkanyi.platypus.feature.auth.signin.AddAccountSheet
import com.joelkanyi.platypus.feature.auth.signin.SignInScreen
import com.joelkanyi.platypus.feature.auth.workspace.WorkspacePickScreen
import com.joelkanyi.platypus.navigation.PlatypusShell
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme

private val WorkspaceStateSaver = listSaver<Workspace?, Any?>(
    save = { workspace ->
        workspace?.let { listOf(it.uuid, it.slug, it.name, it.avatarUrl) } ?: emptyList()
    },
    restore = { values ->
        if (values.isEmpty()) {
            null
        } else {
            Workspace(
                uuid = values[0] as String,
                slug = values[1] as String,
                name = values[2] as String,
                avatarUrl = values[3] as String?,
            )
        }
    },
)

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

            AuthStatus.SignedOut -> SignInScreen(
                authRepository = dependencies.authRepository,
                oauthDeepLinks = dependencies.oauthDeepLinks,
                openUrl = dependencies::openUrl,
            )

            AuthStatus.SignedIn -> {
                SignedInFlow()
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

@Composable
private fun SignedInFlow() {
    val dependencies = LocalPlatypusDependencies.current
    val accounts by dependencies.authRepository.accounts.collectAsStateWithLifecycle()

    var activeAccountId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(accounts) {
        if (accounts.none { it.id == activeAccountId }) {
            activeAccountId = accounts.firstOrNull()?.id
        }
    }

    var selected by rememberSaveable(stateSaver = WorkspaceStateSaver) {
        mutableStateOf<Workspace?>(null)
    }

    val activeId = activeAccountId
    val workspace = selected
    when {
        activeId == null -> LoadingGate()
        workspace == null -> WorkspacePickScreen(
            authRepository = dependencies.authRepository,
            accounts = accounts,
            activeAccountId = activeId,
            onSwitchAccount = {
                activeAccountId = it
                selected = null
            },
            onSelected = { selected = it },
        )
        else -> PlatypusShell()
    }
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
