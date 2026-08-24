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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joelkanyi.platypus.app.LocalAccountActions
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.app.purgeSessionCaches
import com.joelkanyi.platypus.designsystem.PlatypusIcons
import com.joelkanyi.platypus.designsystem.expand
import com.joelkanyi.platypus.domain.model.Account
import com.joelkanyi.platypus.domain.model.AuthMode
import io.github.joelkanyi.jenga.component.avatar.JengaAvatar
import io.github.joelkanyi.jenga.component.avatar.JengaAvatarSize
import io.github.joelkanyi.jenga.component.button.JengaButton
import io.github.joelkanyi.jenga.component.button.JengaButtonVariant
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.feedback.JengaDialog
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.menu.JengaDropdownMenu
import io.github.joelkanyi.jenga.component.menu.JengaDropdownMenuItem
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlinx.coroutines.launch

private const val ATLASSIAN_ACCOUNT_URL = "https://id.atlassian.com/manage-profile/account-preferences"

@Composable
fun ProfileScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    val dependencies = LocalPlatypusDependencies.current
    val accounts by dependencies.authRepository.accounts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    ProfileContent(
        accounts = accounts,
        onSignOut = { accountId ->
            scope.launch {
                dependencies.authRepository.signOut(accountId)
                dependencies.purgeSessionCaches()
            }
        },
        onDeleteAccount = { accountId ->
            scope.launch {
                dependencies.watchlistRepository.clearAccount(accountId)
                dependencies.authRepository.signOut(accountId)
                dependencies.purgeSessionCaches()
            }
        },
        onManageAtlassian = { dependencies.openUrl(ATLASSIAN_ACCOUNT_URL) },
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

@Composable
internal fun ProfileContent(
    accounts: List<Account>,
    onSignOut: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onDeleteAccount: (String) -> Unit = {},
    onManageAtlassian: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    val itemModifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
    val addAccount = LocalAccountActions.current.addAccount

    var menuAccountId by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<Account?>(null) }

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
                        Box {
                            JengaIconButton(onClick = { menuAccountId = account.id }) {
                                JengaIcon(
                                    PlatypusIcons.MoreVertical,
                                    contentDescription = "Manage ${account.user.displayName}",
                                )
                            }
                            JengaDropdownMenu(
                                expanded = menuAccountId == account.id,
                                onDismissRequest = { menuAccountId = null },
                            ) {
                                JengaDropdownMenuItem(
                                    text = "Sign out",
                                    onClick = {
                                        menuAccountId = null
                                        onSignOut(account.id)
                                    },
                                    leadingIcon = { JengaIcon(JengaIcons.Logout, contentDescription = null) },
                                )
                                JengaDropdownMenuItem(
                                    text = "Delete account",
                                    onClick = {
                                        menuAccountId = null
                                        deleteTarget = account
                                    },
                                )
                            }
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

            item {
                JengaListItem(
                    headline = "Settings",
                    supporting = "Appearance and defaults",
                    leadingContent = { JengaIcon(JengaIcons.Sliders, contentDescription = null) },
                    trailingContent = { JengaIcon(JengaIcons.ChevronRight, contentDescription = null) },
                    onClick = onOpenSettings,
                    modifier = itemModifier,
                )
            }
        }
    }

    val target = deleteTarget
    if (target != null) {
        JengaDialog(
            onDismissRequest = { deleteTarget = null },
            title = "Delete account?",
            text = "This removes ${target.user.displayName} and all of its data (watched repositories and " +
                "cached pull requests) from this device. Your Atlassian account itself is not affected. " +
                "To delete your Atlassian account, manage it at id.atlassian.com.",
            confirmButton = {
                JengaButton(
                    text = "Delete",
                    variant = JengaButtonVariant.Danger,
                    onClick = {
                        onDeleteAccount(target.id)
                        deleteTarget = null
                    },
                )
            },
            dismissButton = {
                JengaButton(
                    text = "Manage on Atlassian",
                    variant = JengaButtonVariant.Outline,
                    onClick = {
                        onManageAtlassian()
                        deleteTarget = null
                    },
                )
            },
        )
    }
}

private fun AuthMode.label(): String = when (this) {
    AuthMode.API_TOKEN -> "API token"
    AuthMode.OAUTH -> "Bitbucket"
}
