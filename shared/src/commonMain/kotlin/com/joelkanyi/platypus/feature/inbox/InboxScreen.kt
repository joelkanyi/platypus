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
package com.joelkanyi.platypus.feature.inbox

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
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.domain.model.PrRelationship
import com.joelkanyi.platypus.domain.model.PullRequest
import com.joelkanyi.platypus.domain.usecase.GetReviewInbox
import io.github.joelkanyi.jenga.component.avatar.JengaAvatar
import io.github.joelkanyi.jenga.component.badge.JengaBadge
import io.github.joelkanyi.jenga.component.badge.JengaBadgeTone
import io.github.joelkanyi.jenga.component.chip.JengaChip
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.progress.jengaShimmer
import io.github.joelkanyi.jenga.component.refresh.JengaPullToRefresh
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.state.JengaEmptyState
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun InboxScreen(onBrowseWatchlist: () -> Unit, modifier: Modifier = Modifier) {
    val dependencies = LocalPlatypusDependencies.current
    val useCase = remember(dependencies) {
        GetReviewInbox(dependencies.authRepository, dependencies.watchlistRepository)
    }
    val viewModel = viewModel { InboxViewModel(useCase, dependencies.watchlistRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    InboxContent(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenPullRequest = { url -> dependencies.openUrl(url) },
        onBrowseWatchlist = onBrowseWatchlist,
        modifier = modifier,
    )
}

@Composable
internal fun InboxContent(
    state: InboxUiState,
    onEvent: (InboxUiEvent) -> Unit,
    onOpenPullRequest: (String) -> Unit,
    onBrowseWatchlist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing

    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = "Inbox",
                subtitle = if (state.hasWatchlist) "${state.toReviewCount} awaiting your review" else null,
            )
        },
    ) { innerPadding ->
        if (!state.hasWatchlist) {
            JengaEmptyState(
                title = "Nothing to review yet",
                description = "Watch repositories to see their open pull requests here.",
                actionLabel = "Browse repositories",
                onAction = onBrowseWatchlist,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
            return@JengaScaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            FilterRow(state = state, onEvent = onEvent)

            if (state.failures.isNotEmpty()) {
                JengaText(
                    text = failuresNotice(state.failures.size),
                    style = JengaTheme.typography.caption,
                    color = JengaTheme.colors.warning,
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xs),
                )
            }

            JengaPullToRefresh(
                isRefreshing = state.isRefreshing,
                onRefresh = { onEvent(InboxUiEvent.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    when {
                        state.isLoading -> items(8) { PrSkeletonRow() }

                        state.visible.isEmpty() -> item {
                            JengaEmptyState(
                                title = emptyTitle(state.filter),
                                description = emptyDescription(state.filter),
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }

                        else -> items(state.visible, key = { it.key }) { pr ->
                            PrCard(
                                pullRequest = pr,
                                showRelationship = state.filter == InboxFilter.ALL,
                                onClick = { pr.webUrl?.let(onOpenPullRequest) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(state: InboxUiState, onEvent: (InboxUiEvent) -> Unit) {
    val spacing = JengaTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        JengaChip(
            label = "To review (${state.toReviewCount})",
            selected = state.filter == InboxFilter.TO_REVIEW,
            onClick = { onEvent(InboxUiEvent.SelectFilter(InboxFilter.TO_REVIEW)) },
        )
        JengaChip(
            label = "Mine (${state.mineCount})",
            selected = state.filter == InboxFilter.MINE,
            onClick = { onEvent(InboxUiEvent.SelectFilter(InboxFilter.MINE)) },
        )
        JengaChip(
            label = "All (${state.pullRequests.size})",
            selected = state.filter == InboxFilter.ALL,
            onClick = { onEvent(InboxUiEvent.SelectFilter(InboxFilter.ALL)) },
        )
    }
}

@Composable
private fun PrCard(pullRequest: PullRequest, showRelationship: Boolean, onClick: () -> Unit) {
    val spacing = JengaTheme.spacing
    JengaListItem(
        headline = pullRequest.title,
        supporting = "${pullRequest.repoName} · #${pullRequest.id} · ${pullRequest.authorName}",
        leadingContent = { JengaAvatar(name = pullRequest.authorName) },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                if (showRelationship) {
                    relationshipBadge(pullRequest.relationship)?.let { (label, tone) ->
                        JengaBadge(text = label, tone = tone)
                    }
                }
                if (pullRequest.commentCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                    ) {
                        JengaIcon(JengaIcons.MessageCircle, contentDescription = "Comments")
                        JengaText(
                            text = pullRequest.commentCount.toString(),
                            style = JengaTheme.typography.caption,
                            color = JengaTheme.colors.textMuted,
                        )
                    }
                }
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun PrSkeletonRow() {
    val spacing = JengaTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Box(modifier = Modifier.size(36.dp).clip(JengaTheme.shapes.pill).jengaShimmer())
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.7f).clip(JengaTheme.shapes.control).jengaShimmer())
            Box(modifier = Modifier.height(12.dp).fillMaxWidth(0.4f).clip(JengaTheme.shapes.control).jengaShimmer())
        }
    }
}

private fun relationshipBadge(relationship: PrRelationship): Pair<String, JengaBadgeTone>? = when (relationship) {
    PrRelationship.TO_REVIEW -> "Review" to JengaBadgeTone.Brand
    PrRelationship.MINE -> "Mine" to JengaBadgeTone.Neutral
    PrRelationship.OTHER -> null
}

private fun failuresNotice(count: Int): String =
    if (count == 1) "Couldn't refresh 1 source" else "Couldn't refresh $count sources"

private fun emptyTitle(filter: InboxFilter): String = when (filter) {
    InboxFilter.TO_REVIEW -> "You're all caught up"
    InboxFilter.MINE -> "No open pull requests"
    InboxFilter.ALL -> "No open pull requests"
}

private fun emptyDescription(filter: InboxFilter): String = when (filter) {
    InboxFilter.TO_REVIEW -> "No pull requests are awaiting your review in your watched repositories."
    InboxFilter.MINE -> "You have no open pull requests in your watched repositories."
    InboxFilter.ALL -> "Your watched repositories have no open pull requests."
}
