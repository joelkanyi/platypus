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
package com.joelkanyi.platypus.feature.auth.workspace

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.LocalAccountActions
import com.joelkanyi.platypus.designsystem.expand
import com.joelkanyi.platypus.domain.model.Account
import com.joelkanyi.platypus.domain.model.Workspace
import com.joelkanyi.platypus.domain.repository.AuthRepository
import io.github.joelkanyi.jenga.component.avatar.JengaAvatar
import io.github.joelkanyi.jenga.component.button.JengaButton
import io.github.joelkanyi.jenga.component.button.JengaButtonVariant
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.progress.jengaShimmer
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.state.JengaEmptyState
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun WorkspacePickScreen(
    authRepository: AuthRepository,
    accounts: List<Account>,
    activeAccountId: String,
    onSwitchAccount: (String) -> Unit,
    onSelected: (Workspace) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel(key = activeAccountId) {
        WorkspacePickViewModel(authRepository, activeAccountId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(activeAccountId) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is WorkspacePickUiEffect.Selected -> onSelected(effect.workspace)
            }
        }
    }

    val activeAccount = accounts.firstOrNull { it.id == activeAccountId }

    WorkspacePickContent(
        state = state,
        accounts = accounts,
        activeAccountId = activeAccountId,
        activeLabel = activeAccount?.user?.displayName,
        onSwitchAccount = onSwitchAccount,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

@Composable
internal fun WorkspacePickContent(
    state: WorkspacePickUiState,
    accounts: List<Account>,
    activeAccountId: String,
    activeLabel: String?,
    onSwitchAccount: (String) -> Unit,
    onEvent: (WorkspacePickUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    val itemModifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
    val errorMessage = state.error
    val addAccount = LocalAccountActions.current.addAccount

    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = "Choose a workspace",
                subtitle = activeLabel?.let { "Signed in as $it" },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding.expand(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                AccountSwitcher(
                    accounts = accounts,
                    activeAccountId = activeAccountId,
                    onSwitchAccount = onSwitchAccount,
                    onAddAccount = addAccount,
                    modifier = itemModifier.padding(bottom = spacing.sm),
                )
            }

            when {
                state.isLoading -> items(6) { WorkspaceSkeletonRow(itemModifier) }

                errorMessage != null -> item {
                    JengaErrorState(
                        title = "Couldn't load workspaces",
                        description = errorMessage,
                        actionLabel = "Try again",
                        onAction = { onEvent(WorkspacePickUiEvent.Retry) },
                        modifier = itemModifier.heightIn(min = 240.dp),
                    )
                }

                state.workspaces.isEmpty() -> item {
                    JengaEmptyState(
                        title = "No workspaces",
                        description = "This account isn't a member of any workspace. Add another account below.",
                        modifier = itemModifier.heightIn(min = 240.dp),
                    )
                }

                else -> items(state.workspaces, key = { it.uuid }) { workspace ->
                    JengaListItem(
                        headline = workspace.name,
                        supporting = workspace.slug,
                        leadingContent = { JengaAvatar(name = workspace.name) },
                        onClick = { onEvent(WorkspacePickUiEvent.Select(workspace)) },
                        modifier = itemModifier,
                    )
                }
            }

            item {
                Column(
                    modifier = itemModifier.padding(top = spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    JengaButton(
                        text = "Add another account",
                        onClick = addAccount,
                        modifier = Modifier.fillMaxWidth(),
                        variant = JengaButtonVariant.Outline,
                    )
                    JengaButton(
                        text = "Sign out",
                        onClick = { onEvent(WorkspacePickUiEvent.SignOut) },
                        modifier = Modifier.fillMaxWidth(),
                        variant = JengaButtonVariant.Ghost,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountSwitcher(
    accounts: List<Account>,
    activeAccountId: String,
    onSwitchAccount: (String) -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        items(accounts, key = { it.id }) { account ->
            Column(
                modifier = Modifier.clickable { onSwitchAccount(account.id) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                val active = account.id == activeAccountId
                Box(
                    modifier = Modifier
                        .border(
                            width = if (active) 2.dp else 1.dp,
                            color = if (active) JengaTheme.colors.brand else JengaTheme.colors.border,
                            shape = JengaTheme.shapes.pill,
                        )
                        .padding(3.dp),
                ) {
                    JengaAvatar(name = account.user.displayName)
                }
                JengaText(
                    text = account.user.nickname,
                    style = JengaTheme.typography.caption,
                    color = if (account.id == activeAccountId) {
                        JengaTheme.colors.textPrimary
                    } else {
                        JengaTheme.colors.textMuted
                    },
                )
            }
        }
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                JengaIconButton(onClick = onAddAccount) {
                    JengaIcon(JengaIcons.Add, contentDescription = "Add account")
                }
                JengaText(
                    text = "Add",
                    style = JengaTheme.typography.caption,
                    color = JengaTheme.colors.textMuted,
                )
            }
        }
    }
}

@Composable
private fun WorkspaceSkeletonRow(modifier: Modifier = Modifier) {
    val spacing = JengaTheme.spacing
    Row(
        modifier = modifier.padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(JengaTheme.shapes.pill)
                .jengaShimmer(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Box(
                modifier = Modifier
                    .height(14.dp)
                    .widthIn(min = 160.dp)
                    .fillMaxWidth(0.5f)
                    .clip(JengaTheme.shapes.sm)
                    .jengaShimmer(),
            )
            Box(
                modifier = Modifier
                    .height(12.dp)
                    .widthIn(min = 90.dp)
                    .clip(JengaTheme.shapes.sm)
                    .jengaShimmer(),
            )
        }
    }
}
