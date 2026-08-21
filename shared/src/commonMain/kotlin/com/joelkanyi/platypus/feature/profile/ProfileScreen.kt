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
package com.joelkanyi.platypus.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joelkanyi.platypus.app.LocalAccountActions
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.designsystem.expand
import com.joelkanyi.platypus.domain.model.Account
import com.joelkanyi.platypus.domain.model.AuthMode
import io.github.joelkanyi.jenga.component.avatar.JengaAvatar
import io.github.joelkanyi.jenga.component.avatar.JengaAvatarSize
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    val dependencies = LocalPlatypusDependencies.current
    val accounts by dependencies.authRepository.accounts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    ProfileContent(
        accounts = accounts,
        onSignOut = { accountId -> scope.launch { dependencies.authRepository.signOut(accountId) } },
        modifier = modifier,
    )
}

@Composable
internal fun ProfileContent(accounts: List<Account>, onSignOut: (String) -> Unit, modifier: Modifier = Modifier) {
    val spacing = JengaTheme.spacing
    val itemModifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
    val addAccount = LocalAccountActions.current.addAccount

    JengaScaffold(
        modifier = modifier,
        topBar = { JengaTopAppBar(title = "Profile") },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding.expand(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(accounts, key = { it.id }) { account ->
                JengaListItem(
                    headline = account.user.displayName,
                    supporting = "@${account.user.nickname} · ${account.mode.label()}",
                    leadingContent = {
                        JengaAvatar(name = account.user.displayName, size = JengaAvatarSize.Large)
                    },
                    trailingContent = {
                        JengaIconButton(onClick = { onSignOut(account.id) }) {
                            JengaIcon(JengaIcons.Logout, contentDescription = "Sign out ${account.user.displayName}")
                        }
                    },
                    modifier = itemModifier,
                )
            }

            item {
                JengaListItem(
                    headline = "Add account",
                    supporting = "Sign in another Bitbucket account",
                    leadingContent = { JengaIcon(JengaIcons.Add, contentDescription = null) },
                    onClick = addAccount,
                    modifier = itemModifier.padding(top = spacing.sm),
                )
            }
        }
    }
}

private fun AuthMode.label(): String = when (this) {
    AuthMode.API_TOKEN -> "API token"
    AuthMode.OAUTH -> "Bitbucket"
}
