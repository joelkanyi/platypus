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
package com.joelkanyi.platypus.feature.repo.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.designsystem.PlatypusIcons
import com.joelkanyi.platypus.designsystem.PlatypusMarkdown
import com.joelkanyi.platypus.designsystem.expand
import com.joelkanyi.platypus.designsystem.formatByteSize
import com.joelkanyi.platypus.domain.model.RepositoryDetail
import com.joelkanyi.platypus.ui.BranchesSheet
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.card.JengaCard
import io.github.joelkanyi.jenga.component.chip.JengaChip
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun RepositoryOverviewScreen(
    accountId: String,
    workspace: String,
    repoSlug: String,
    repoName: String,
    onOpenFiles: (ref: String) -> Unit,
    onOpenCommits: (ref: String) -> Unit,
    onOpenBranch: (ref: String) -> Unit,
    onOpenPullRequests: () -> Unit,
    onOpenPipelines: () -> Unit,
    onOpenDeployments: () -> Unit,
    onOpenSchedules: () -> Unit,
    onSearchRepo: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "$accountId/$workspace/$repoSlug") {
        RepositoryOverviewViewModel(dependencies.repoContentRepository, accountId, workspace, repoSlug)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showBranches by rememberSaveable { mutableStateOf(false) }

    OverviewContent(
        repoName = repoName,
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onOpenFiles = onOpenFiles,
        onOpenCommits = onOpenCommits,
        onOpenPullRequests = onOpenPullRequests,
        onOpenPipelines = onOpenPipelines,
        onOpenDeployments = onOpenDeployments,
        onOpenSchedules = onOpenSchedules,
        onSearchRepo = onSearchRepo,
        onBranchClick = { showBranches = true },
        onOpenUrl = onOpenUrl,
        modifier = modifier,
    )

    val detail = state.detail
    if (showBranches && detail != null) {
        BranchesSheet(
            accountId = accountId,
            workspace = workspace,
            repoSlug = repoSlug,
            currentRef = detail.defaultBranch,
            onSelect = {
                showBranches = false
                onOpenBranch(it)
            },
            onDismiss = { showBranches = false },
        )
    }
}

@Composable
internal fun OverviewContent(
    repoName: String,
    state: OverviewUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenFiles: (ref: String) -> Unit,
    onOpenCommits: (ref: String) -> Unit,
    onOpenPullRequests: () -> Unit,
    onOpenPipelines: () -> Unit,
    onOpenDeployments: () -> Unit,
    onOpenSchedules: () -> Unit,
    onSearchRepo: () -> Unit,
    onBranchClick: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    val detail = state.detail

    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = repoName,
                subtitle = detail?.let { if (it.isPrivate) "Private" else "Public" },
                navigationIcon = {
                    JengaIconButton(onClick = onBack) {
                        JengaIcon(JengaIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    detail?.webUrl?.let { url ->
                        JengaIconButton(onClick = { onOpenUrl(url) }) {
                            JengaIcon(JengaIcons.Share, contentDescription = "Open on web")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.error != null -> JengaErrorState(
                title = "Couldn't load repository",
                description = state.error,
                actionLabel = "Try again",
                onAction = onRetry,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            detail != null -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding.expand(horizontal = spacing.lg, vertical = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                if (detail.description.isNotBlank()) {
                    item { JengaText(text = detail.description, color = JengaTheme.colors.textSecondary) }
                }
                item { FactChips(detail) }
                item {
                    JengaListItem(
                        headline = detail.defaultBranch,
                        supporting = "Branch · tap to switch",
                        leadingContent = { JengaIcon(JengaIcons.Swap, contentDescription = null) },
                        trailingContent = { JengaIcon(JengaIcons.ChevronDown, contentDescription = null) },
                        onClick = onBranchClick,
                    )
                }
                item {
                    JengaListItem(
                        headline = "Files",
                        supporting = "Browse the source",
                        leadingContent = { JengaIcon(PlatypusIcons.Folder, contentDescription = null) },
                        trailingContent = { JengaIcon(JengaIcons.ChevronRight, contentDescription = null) },
                        onClick = { onOpenFiles(detail.defaultBranch) },
                    )
                }
                item {
                    JengaListItem(
                        headline = "Search code",
                        supporting = "Find text in this repository",
                        leadingContent = { JengaIcon(JengaIcons.Search, contentDescription = null) },
                        trailingContent = { JengaIcon(JengaIcons.ChevronRight, contentDescription = null) },
                        onClick = onSearchRepo,
                    )
                }
                item {
                    JengaListItem(
                        headline = "Pull requests",
                        supporting = "Open pull requests for this repository",
                        leadingContent = { JengaIcon(JengaIcons.MessageCircle, contentDescription = null) },
                        trailingContent = { JengaIcon(JengaIcons.ChevronRight, contentDescription = null) },
                        onClick = onOpenPullRequests,
                    )
                }
                item {
                    JengaListItem(
                        headline = "Pipelines",
                        supporting = "CI/CD runs for this repository",
                        leadingContent = { JengaIcon(JengaIcons.Flash, contentDescription = null) },
                        trailingContent = { JengaIcon(JengaIcons.ChevronRight, contentDescription = null) },
                        onClick = onOpenPipelines,
                    )
                }
                item {
                    JengaListItem(
                        headline = "Deployments",
                        supporting = "Environments and recent deployments",
                        leadingContent = { JengaIcon(JengaIcons.Cloud, contentDescription = null) },
                        trailingContent = { JengaIcon(JengaIcons.ChevronRight, contentDescription = null) },
                        onClick = onOpenDeployments,
                    )
                }
                item {
                    JengaListItem(
                        headline = "Schedules",
                        supporting = "Scheduled pipeline runs",
                        leadingContent = { JengaIcon(JengaIcons.Clock, contentDescription = null) },
                        trailingContent = { JengaIcon(JengaIcons.ChevronRight, contentDescription = null) },
                        onClick = onOpenSchedules,
                    )
                }
                item {
                    JengaListItem(
                        headline = "Commits",
                        supporting = "History on ${detail.defaultBranch}",
                        leadingContent = { JengaIcon(PlatypusIcons.GitBranch, contentDescription = null) },
                        trailingContent = { JengaIcon(JengaIcons.ChevronRight, contentDescription = null) },
                        onClick = { onOpenCommits(detail.defaultBranch) },
                    )
                }
                if (state.readme != null) {
                    item { ReadmeBlock(state.readme) }
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun FactChips(detail: RepositoryDetail) {
    val spacing = JengaTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (detail.language.isNotBlank()) JengaChip(label = detail.language, selected = false, onClick = {})
        JengaChip(label = formatByteSize(detail.size), selected = false, onClick = {})
        if (detail.updatedOn.isNotBlank()) {
            JengaChip(label = "Updated ${detail.updatedOn.substringBefore('T')}", selected = false, onClick = {})
        }
    }
}

@Composable
private fun ReadmeBlock(readme: String) {
    val spacing = JengaTheme.spacing
    JengaCard(modifier = Modifier.fillMaxWidth()) {
        PlatypusMarkdown(content = readme, modifier = Modifier.padding(spacing.md))
    }
}
