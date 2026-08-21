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
package com.joelkanyi.platypus.feature.repositories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.domain.model.WatchedRepo
import io.github.joelkanyi.jenga.component.avatar.JengaAvatar
import io.github.joelkanyi.jenga.component.chip.JengaChip
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.progress.jengaShimmer
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.search.JengaSearchField
import io.github.joelkanyi.jenga.component.selection.JengaToggle
import io.github.joelkanyi.jenga.component.state.JengaEmptyState
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun RepositoriesScreen(onOpenRepo: (WatchedRepo) -> Unit, modifier: Modifier = Modifier) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel {
        RepositoriesViewModel(dependencies.authRepository, dependencies.watchlistRepository)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RepositoriesContent(state = state, onEvent = viewModel::onEvent, onOpenRepo = onOpenRepo, modifier = modifier)
}

@Composable
internal fun RepositoriesContent(
    state: RepositoriesUiState,
    onEvent: (RepositoriesUiEvent) -> Unit,
    onOpenRepo: (WatchedRepo) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing

    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = "Repositories",
                subtitle = "${state.watchedCount} watching",
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                JengaChip(
                    label = "Watching (${state.watchedCount})",
                    selected = state.tab == RepoTab.WATCHING,
                    onClick = { onEvent(RepositoriesUiEvent.SelectTab(RepoTab.WATCHING)) },
                )
                JengaChip(
                    label = "Browse",
                    selected = state.tab == RepoTab.BROWSE,
                    onClick = { onEvent(RepositoriesUiEvent.SelectTab(RepoTab.BROWSE)) },
                )
            }

            when (state.tab) {
                RepoTab.WATCHING -> WatchingPane(state = state, onEvent = onEvent, onOpenRepo = onOpenRepo)
                RepoTab.BROWSE -> BrowsePane(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun WatchingPane(
    state: RepositoriesUiState,
    onEvent: (RepositoriesUiEvent) -> Unit,
    onOpenRepo: (WatchedRepo) -> Unit,
) {
    val spacing = JengaTheme.spacing

    if (state.watched.isEmpty()) {
        JengaEmptyState(
            title = "Not watching anything yet",
            description = "Add repositories to your watchlist to follow their pull requests.",
            actionLabel = "Browse repositories",
            onAction = { onEvent(RepositoriesUiEvent.SelectTab(RepoTab.BROWSE)) },
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        items(state.watched, key = { it.repoUuid }) { watched ->
            WatchedRow(
                watched = watched,
                onOpen = { onOpenRepo(watched) },
                onUnwatch = { onEvent(RepositoriesUiEvent.Unwatch(watched)) },
            )
        }
    }
}

@Composable
private fun WatchedRow(watched: WatchedRepo, onOpen: () -> Unit, onUnwatch: () -> Unit) {
    JengaListItem(
        headline = watched.name,
        supporting = watched.fullName,
        leadingContent = { JengaAvatar(name = watched.name) },
        trailingContent = {
            JengaToggle(checked = true, onCheckedChange = { onUnwatch() })
        },
        onClick = onOpen,
    )
}

@Composable
private fun BrowsePane(state: RepositoriesUiState, onEvent: (RepositoriesUiEvent) -> Unit) {
    when {
        state.isLoadingWorkspaces -> LoadingRepos()

        state.workspacesError != null -> JengaErrorState(
            title = "Couldn't load workspaces",
            description = state.workspacesError,
            actionLabel = "Try again",
            onAction = { onEvent(RepositoriesUiEvent.RetryWorkspaces) },
            modifier = Modifier.fillMaxSize(),
        )

        state.workspaces.isEmpty() -> JengaEmptyState(
            title = "No repositories",
            description = "None of your accounts belong to a workspace yet.",
            modifier = Modifier.fillMaxSize(),
        )

        else -> BrowseList(state = state, onEvent = onEvent)
    }
}

@Composable
private fun BrowseList(state: RepositoriesUiState, onEvent: (RepositoriesUiEvent) -> Unit) {
    val spacing = JengaTheme.spacing
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.repos.size - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(shouldLoadMore, state.canLoadMore) {
        if (shouldLoadMore && state.canLoadMore) onEvent(RepositoriesUiEvent.LoadMore)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.workspaces.size > 1) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                items(state.workspaces, key = { it.id }) { option ->
                    JengaChip(
                        label = if (state.multiAccount) {
                            "${option.accountLabel} · ${option.workspace.name}"
                        } else {
                            option.workspace.name
                        },
                        selected = option.id == state.selected?.id,
                        onClick = { onEvent(RepositoriesUiEvent.SelectWorkspace(option)) },
                    )
                }
            }
        }

        JengaSearchField(
            value = state.query,
            onValueChange = { onEvent(RepositoriesUiEvent.QueryChanged(it)) },
            placeholder = "Search repositories",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.sm),
        )

        when {
            state.isLoadingRepos -> LoadingRepos()

            state.reposError != null -> JengaErrorState(
                title = "Couldn't load repositories",
                description = state.reposError,
                actionLabel = "Try again",
                onAction = { onEvent(RepositoriesUiEvent.RetryRepos) },
                modifier = Modifier.fillMaxSize(),
            )

            state.repos.isEmpty() -> JengaEmptyState(
                title = if (state.query.isBlank()) "No repositories" else "No matches",
                description = if (state.query.isBlank()) {
                    "This workspace has no repositories."
                } else {
                    "No repositories match \"${state.query}\"."
                },
                modifier = Modifier.fillMaxSize(),
            )

            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                items(state.repos, key = { it.repo.uuid }) { row ->
                    JengaListItem(
                        headline = row.repo.name,
                        supporting = row.repo.fullName,
                        leadingContent = {
                            if (row.repo.isPrivate) {
                                JengaIcon(JengaIcons.Lock, contentDescription = "Private")
                            } else {
                                JengaAvatar(name = row.repo.name)
                            }
                        },
                        trailingContent = {
                            JengaToggle(
                                checked = row.watched,
                                onCheckedChange = { onEvent(RepositoriesUiEvent.ToggleWatch(row.repo, it)) },
                            )
                        },
                    )
                }

                if (state.isPaginating) {
                    item(key = "paginating") { RepoSkeletonRow() }
                }
            }
        }
    }
}

@Composable
private fun LoadingRepos() {
    val spacing = JengaTheme.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        items(8) { RepoSkeletonRow() }
    }
}

@Composable
private fun RepoSkeletonRow() {
    val spacing = JengaTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Box(modifier = Modifier.size(36.dp).clip(JengaTheme.shapes.pill).jengaShimmer())
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.5f).clip(JengaTheme.shapes.control).jengaShimmer())
            Box(modifier = Modifier.height(12.dp).fillMaxWidth(0.7f).clip(JengaTheme.shapes.control).jengaShimmer())
        }
    }
}

private const val LOAD_MORE_THRESHOLD = 3
