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
package com.joelkanyi.platypus.feature.pr.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.designsystem.PlatypusIcons
import com.joelkanyi.platypus.designsystem.PlatypusMarkdown
import com.joelkanyi.platypus.designsystem.expand
import com.joelkanyi.platypus.designsystem.rememberGeistMonoFontFamily
import com.joelkanyi.platypus.domain.model.ActivityItem
import com.joelkanyi.platypus.domain.model.MergeStrategy
import com.joelkanyi.platypus.domain.model.PrApproval
import com.joelkanyi.platypus.domain.model.PrComment
import com.joelkanyi.platypus.domain.model.PrReviewer
import com.joelkanyi.platypus.domain.model.PrState
import com.joelkanyi.platypus.domain.model.PullRequestDetail
import io.github.joelkanyi.jenga.component.avatar.JengaAvatar
import io.github.joelkanyi.jenga.component.avatar.JengaAvatarSize
import io.github.joelkanyi.jenga.component.badge.JengaBadgeTone
import io.github.joelkanyi.jenga.component.banner.JengaBanner
import io.github.joelkanyi.jenga.component.banner.JengaBannerTone
import io.github.joelkanyi.jenga.component.button.JengaButton
import io.github.joelkanyi.jenga.component.button.JengaButtonSize
import io.github.joelkanyi.jenga.component.button.JengaButtonVariant
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.card.JengaCard
import io.github.joelkanyi.jenga.component.card.JengaCardVariant
import io.github.joelkanyi.jenga.component.divider.JengaDivider
import io.github.joelkanyi.jenga.component.feedback.JengaBottomSheet
import io.github.joelkanyi.jenga.component.feedback.JengaDialog
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.menu.JengaDropdownMenu
import io.github.joelkanyi.jenga.component.menu.JengaDropdownMenuItem
import io.github.joelkanyi.jenga.component.progress.jengaShimmer
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.selection.JengaToggle
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.component.status.JengaStatusPill
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.component.textfield.JengaTextField
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun PrDetailScreen(
    accountId: String,
    workspace: String,
    repoSlug: String,
    prId: Long,
    repoName: String,
    onBack: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenCommits: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "$accountId/$workspace/$repoSlug/$prId") {
        val settings = dependencies.settingsStore.settings.value
        PrDetailViewModel(
            dependencies.pullRequestRepository,
            accountId,
            workspace,
            repoSlug,
            prId,
            settings.defaultMergeStrategy,
            settings.closeSourceBranchOnMerge,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PrDetailContent(
        repoName = repoName,
        prId = prId,
        state = state,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        onOpenFiles = onOpenFiles,
        onOpenCommits = onOpenCommits,
        onOpenUrl = onOpenUrl,
        modifier = modifier,
    )
}

@Composable
internal fun PrDetailContent(
    repoName: String,
    prId: Long,
    state: PrDetailUiState,
    onEvent: (PrDetailEvent) -> Unit,
    onBack: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenCommits: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    val detail = state.detail

    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = "#$prId",
                subtitle = repoName.takeIf { it.isNotBlank() },
                navigationIcon = {
                    JengaIconButton(onClick = onBack) {
                        JengaIcon(JengaIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = { if (detail != null) TopBarActions(state, detail, onEvent, onOpenUrl) },
            )
        },
    ) { innerPadding ->
        when {
            state.error != null -> JengaErrorState(
                title = "Couldn't load pull request",
                description = state.error,
                actionLabel = "Try again",
                onAction = { onEvent(PrDetailEvent.Retry) },
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            detail != null -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding.expand(horizontal = spacing.lg, vertical = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                state.actionError?.let { message ->
                    item {
                        JengaBanner(
                            message = message,
                            tone = JengaBannerTone.Error,
                            title = "Action failed",
                            action = {
                                JengaText(
                                    text = "Dismiss",
                                    style = JengaTheme.typography.caption,
                                    color = JengaTheme.colors.brand,
                                    modifier = Modifier.clickable { onEvent(PrDetailEvent.DismissActionError) },
                                )
                            },
                        )
                    }
                }

                item { Header(detail) }

                if (!detail.isOpen) {
                    item { OutcomeBanner(detail) }
                }

                if (detail.isOpen && state.hasConflicts) {
                    item {
                        JengaBanner(
                            message = "This pull request has merge conflicts with " +
                                "${detail.destinationBranch} and cannot be merged until they are resolved.",
                            tone = JengaBannerTone.Warning,
                            title = "Merge conflicts",
                        )
                    }
                }

                item {
                    SectionCard(padded = false) { NavRows(onOpenFiles, onOpenCommits) }
                }

                if (detail.reviewers.isNotEmpty()) {
                    item {
                        SectionCard { ReviewersCompact(detail.reviewers, state.reviewersExpanded, onEvent) }
                    }
                }

                if (detail.description.isNotBlank()) {
                    item {
                        SectionCard {
                            DescriptionBlock(
                                description = detail.description,
                                expanded = state.descriptionExpanded,
                                onEvent = onEvent,
                            )
                        }
                    }
                }

                item { SectionLabel("Activity") }
                if (state.activity.isEmpty()) {
                    item {
                        JengaText(
                            text = "No activity yet.",
                            style = JengaTheme.typography.bodySmall,
                            color = JengaTheme.colors.textMuted,
                        )
                    }
                }
                itemsIndexed(state.activity, key = { index, _ -> index }) { index, item ->
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                        if (index > 0) JengaDivider(modifier = Modifier.fillMaxWidth())
                        ActivityRow(
                            item = item,
                            onReply = { onEvent(PrDetailEvent.StartReply(it)) },
                            onResolve = { onEvent(PrDetailEvent.ResolveComment(it)) },
                        )
                    }
                }

                if (detail.isOpen) {
                    item { CommentComposer(state = state, onEvent = onEvent) }
                }
            }

            else -> PrDetailSkeleton(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding.expand(horizontal = spacing.lg, vertical = spacing.md),
            )
        }
    }

    if (state.showReviewSheet && detail != null) {
        ReviewSheet(state = state, onEvent = onEvent)
    }
    if (state.showMergeSheet && detail != null) {
        MergeSheet(state = state, onEvent = onEvent)
    }
    if (state.showDeclineDialog && detail != null) {
        DeclineDialog(onEvent = onEvent)
    }
}

@Composable
private fun TopBarActions(
    state: PrDetailUiState,
    detail: PullRequestDetail,
    onEvent: (PrDetailEvent) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    if (detail.isOpen && state.canReview) {
        JengaButton(
            text = "Review",
            onClick = { onEvent(PrDetailEvent.OpenReviewSheet) },
            variant = JengaButtonVariant.Primary,
            size = JengaButtonSize.Small,
        )
    }
    Box {
        JengaIconButton(onClick = { menuOpen = true }) {
            JengaIcon(PlatypusIcons.MoreVertical, contentDescription = "More")
        }
        JengaDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            if (detail.isOpen) {
                JengaDropdownMenuItem(
                    text = "Merge",
                    onClick = {
                        menuOpen = false
                        onEvent(PrDetailEvent.OpenMergeSheet)
                    },
                )
                JengaDropdownMenuItem(
                    text = "Decline",
                    onClick = {
                        menuOpen = false
                        onEvent(PrDetailEvent.OpenDeclineDialog)
                    },
                )
            }
            detail.webUrl?.let { url ->
                JengaDropdownMenuItem(
                    text = "Open on web",
                    onClick = {
                        menuOpen = false
                        onOpenUrl(url)
                    },
                )
            }
        }
    }
}

@Composable
private fun SectionCard(padded: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    JengaCard(
        modifier = Modifier.fillMaxWidth(),
        variant = JengaCardVariant.Outlined,
        contentPadding = PaddingValues(if (padded) JengaTheme.spacing.md else 0.dp),
        content = content,
    )
}

@Composable
private fun Header(detail: PullRequestDetail) {
    val spacing = JengaTheme.spacing
    val mono = rememberGeistMonoFontFamily()
    val info = JengaTheme.colors.info
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        stateLozenge(detail.state)?.let { (label, tone) -> JengaStatusPill(label = label, tone = tone) }
        JengaText(
            text = buildAnnotatedString {
                val key = detail.jiraKey
                val idx = key?.let { detail.title.indexOf(it) } ?: -1
                if (key != null && idx >= 0) {
                    append(detail.title.substring(0, idx))
                    withStyle(SpanStyle(color = info, fontWeight = FontWeight.SemiBold)) { append(key) }
                    append(detail.title.substring(idx + key.length))
                } else {
                    append(detail.title)
                }
            },
            style = JengaTheme.typography.headingMedium,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            BranchChip(detail.sourceBranch, mono, Modifier.weight(1f, fill = false))
            JengaIcon(JengaIcons.ArrowRight, contentDescription = null, tint = JengaTheme.colors.textMuted)
            BranchChip(detail.destinationBranch, mono)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            JengaAvatar(name = detail.authorName, size = JengaAvatarSize.Small)
            JengaText(
                text = "${detail.authorName} · ${detail.updatedOn.substringBefore('T')}",
                style = JengaTheme.typography.caption,
                color = JengaTheme.colors.textMuted,
            )
        }
    }
}

@Composable
private fun BranchChip(name: String, mono: FontFamily, modifier: Modifier = Modifier) {
    val spacing = JengaTheme.spacing
    JengaText(
        text = name,
        style = JengaTheme.typography.caption.copy(fontFamily = mono),
        color = JengaTheme.colors.textSecondary,
        softWrap = false,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(JengaTheme.shapes.control)
            .background(JengaTheme.colors.surfaceVariant)
            .padding(horizontal = spacing.sm, vertical = spacing.xxs),
    )
}

@Composable
private fun OutcomeBanner(detail: PullRequestDetail) {
    val (title, message, tone) = when (detail.state) {
        PrState.MERGED -> Triple(
            "Merged",
            "This pull request was merged into ${detail.destinationBranch}.",
            JengaBannerTone.Success,
        )
        PrState.DECLINED -> Triple("Declined", "This pull request was declined.", JengaBannerTone.Warning)
        else -> Triple("Closed", "This pull request is no longer open.", JengaBannerTone.Info)
    }
    JengaBanner(message = message, tone = tone, title = title)
}

@Composable
private fun NavRows(onOpenFiles: () -> Unit, onOpenCommits: () -> Unit) {
    JengaListItem(
        headline = "Files changed",
        supporting = "Review the code changes",
        leadingContent = { JengaIcon(JengaIcons.Sliders, contentDescription = null) },
        trailingContent = { JengaIcon(JengaIcons.ChevronRight, contentDescription = null) },
        onClick = onOpenFiles,
    )
    JengaDivider(modifier = Modifier.fillMaxWidth())
    JengaListItem(
        headline = "Commits",
        supporting = "Commits in this pull request",
        leadingContent = { JengaIcon(PlatypusIcons.GitBranch, contentDescription = null) },
        trailingContent = { JengaIcon(JengaIcons.ChevronRight, contentDescription = null) },
        onClick = onOpenCommits,
    )
}

@Composable
private fun ReviewersCompact(reviewers: List<PrReviewer>, expanded: Boolean, onEvent: (PrDetailEvent) -> Unit) {
    val spacing = JengaTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(PrDetailEvent.ToggleReviewers(!expanded)) }
                .padding(vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            OverlappingAvatars(reviewers)
            Column(modifier = Modifier.weight(1f)) {
                JengaText(
                    text = if (reviewers.size == 1) "1 reviewer" else "${reviewers.size} reviewers",
                    style = JengaTheme.typography.bodySmall,
                    color = JengaTheme.colors.textPrimary,
                )
                JengaText(
                    text = reviewerSummary(reviewers),
                    style = JengaTheme.typography.caption,
                    color = JengaTheme.colors.textMuted,
                )
            }
            JengaIcon(
                if (expanded) JengaIcons.ChevronUp else JengaIcons.ChevronDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = JengaTheme.colors.textMuted,
            )
        }
        if (expanded) {
            reviewers.forEachIndexed { index, reviewer ->
                if (index > 0) JengaDivider()
                val (label, tone) = approvalPill(reviewer.approval)
                JengaListItem(
                    headline = reviewer.name,
                    supporting = if (reviewer.isDefault) "Default reviewer" else "Reviewer",
                    leadingContent = { JengaAvatar(name = reviewer.name, size = JengaAvatarSize.Small) },
                    trailingContent = { JengaStatusPill(label = label, tone = tone) },
                )
            }
        }
    }
}

@Composable
private fun OverlappingAvatars(reviewers: List<PrReviewer>) {
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        reviewers.take(5).forEach { reviewer ->
            Box(
                modifier = Modifier
                    .clip(JengaTheme.shapes.pill)
                    .background(JengaTheme.colors.background)
                    .padding(1.dp),
            ) {
                JengaAvatar(name = reviewer.name, size = JengaAvatarSize.Small)
            }
        }
    }
}

@Composable
private fun DescriptionBlock(description: String, expanded: Boolean, onEvent: (PrDetailEvent) -> Unit) {
    val spacing = JengaTheme.spacing
    val isLong = description.length > 160
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        SectionLabel("Description")
        Box(
            modifier = if (isLong && !expanded) {
                Modifier.heightIn(max = 120.dp).clipToBounds()
            } else {
                Modifier
            },
        ) {
            PlatypusMarkdown(content = description)
        }
        if (isLong) {
            JengaText(
                text = if (expanded) "See less" else "See more",
                style = JengaTheme.typography.bodySmall,
                color = JengaTheme.colors.brand,
                modifier = Modifier.clickable { onEvent(PrDetailEvent.ToggleDescription(!expanded)) },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    JengaText(
        text = text.uppercase(),
        style = JengaTheme.typography.caption,
        color = JengaTheme.colors.textMuted,
    )
}

@Composable
private fun ReviewSheet(state: PrDetailUiState, onEvent: (PrDetailEvent) -> Unit) {
    val spacing = JengaTheme.spacing
    JengaBottomSheet(onDismissRequest = { onEvent(PrDetailEvent.DismissReviewSheet) }) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            JengaText(text = "Review", style = JengaTheme.typography.titleMedium)
            JengaTextField(
                value = state.commentDraft,
                onValueChange = { onEvent(PrDetailEvent.CommentDraftChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Leave a note (optional)",
                singleLine = false,
            )
            JengaButton(
                text = if (state.isApproved) "Unapprove" else "Approve",
                onClick = { onEvent(PrDetailEvent.ToggleApprove) },
                modifier = Modifier.fillMaxWidth(),
                variant = JengaButtonVariant.Primary,
                enabled = state.canAct,
            )
            JengaButton(
                text = if (state.hasRequestedChanges) "Undo request changes" else "Request changes",
                onClick = { onEvent(PrDetailEvent.ToggleRequestChanges) },
                modifier = Modifier.fillMaxWidth(),
                variant = JengaButtonVariant.Outline,
                enabled = state.canAct,
            )
            JengaButton(
                text = "Comment",
                onClick = { onEvent(PrDetailEvent.PostComment) },
                modifier = Modifier.fillMaxWidth(),
                variant = JengaButtonVariant.Ghost,
                enabled = state.commentDraft.isNotBlank() && !state.postingComment,
                loading = state.postingComment,
            )
        }
    }
}

@Composable
private fun MergeSheet(state: PrDetailUiState, onEvent: (PrDetailEvent) -> Unit) {
    val spacing = JengaTheme.spacing
    JengaBottomSheet(onDismissRequest = { onEvent(PrDetailEvent.DismissMergeSheet) }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            JengaText(text = "Merge pull request", style = JengaTheme.typography.titleMedium)
            MergeStrategy.entries.forEach { strategy ->
                JengaListItem(
                    headline = mergeStrategyLabel(strategy),
                    supporting = mergeStrategyHint(strategy),
                    trailingContent = {
                        JengaIcon(
                            if (state.mergeStrategy == strategy) JengaIcons.CheckCircle else JengaIcons.ChevronRight,
                            contentDescription = null,
                            tint = if (state.mergeStrategy == strategy) {
                                JengaTheme.colors.brand
                            } else {
                                JengaTheme.colors.textMuted
                            },
                        )
                    },
                    onClick = { onEvent(PrDetailEvent.SelectMergeStrategy(strategy)) },
                )
            }
            JengaListItem(
                headline = "Close source branch",
                trailingContent = {
                    JengaToggle(
                        checked = state.closeSourceBranch,
                        onCheckedChange = { onEvent(PrDetailEvent.ToggleCloseSourceBranch(it)) },
                    )
                },
            )
            JengaButton(
                text = "Merge",
                onClick = { onEvent(PrDetailEvent.ConfirmMerge) },
                modifier = Modifier.fillMaxWidth(),
                variant = JengaButtonVariant.Primary,
                loading = state.actionInProgress,
            )
        }
    }
}

@Composable
private fun DeclineDialog(onEvent: (PrDetailEvent) -> Unit) {
    JengaDialog(
        onDismissRequest = { onEvent(PrDetailEvent.DismissDeclineDialog) },
        title = "Decline this pull request?",
        text = "Declining closes the request and notifies the author. A declined pull request can be reopened.",
        confirmButton = {
            JengaButton(
                text = "Decline",
                onClick = { onEvent(PrDetailEvent.ConfirmDecline) },
                variant = JengaButtonVariant.Danger,
            )
        },
        dismissButton = {
            JengaButton(
                text = "Keep open",
                onClick = { onEvent(PrDetailEvent.DismissDeclineDialog) },
                variant = JengaButtonVariant.Outline,
            )
        },
    )
}

@Composable
private fun CommentComposer(state: PrDetailUiState, onEvent: (PrDetailEvent) -> Unit) {
    val spacing = JengaTheme.spacing
    Column(
        modifier = Modifier.fillMaxWidth().imePadding(),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        state.replyingTo?.let { target ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                JengaText(
                    text = "Replying to ${target.authorName}",
                    style = JengaTheme.typography.caption,
                    color = JengaTheme.colors.textMuted,
                )
                JengaText(
                    text = "Cancel",
                    style = JengaTheme.typography.caption,
                    color = JengaTheme.colors.brand,
                    modifier = Modifier.clickable { onEvent(PrDetailEvent.CancelReply) },
                )
            }
        }
        JengaTextField(
            value = state.commentDraft,
            onValueChange = { onEvent(PrDetailEvent.CommentDraftChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Add a comment",
            enabled = !state.postingComment,
            trailingIcon = {
                if (state.commentDraft.isNotBlank()) {
                    JengaIconButton(onClick = { onEvent(PrDetailEvent.PostComment) }) {
                        JengaIcon(PlatypusIcons.Send, contentDescription = "Send", tint = JengaTheme.colors.brand)
                    }
                }
            },
        )
    }
}

@Composable
private fun ActivityRow(item: ActivityItem, onReply: (PrComment) -> Unit, onResolve: (PrComment) -> Unit) {
    when (item) {
        is ActivityItem.Approved -> EventRow(
            JengaIcons.CheckCircle,
            JengaTheme.colors.success,
            "${item.actorName} approved",
            item.date,
        )
        is ActivityItem.ChangesRequested -> EventRow(
            JengaIcons.Warning,
            JengaTheme.colors.warning,
            "${item.actorName} requested changes",
            item.date,
        )
        is ActivityItem.Updated -> EventRow(
            JengaIcons.Refresh,
            JengaTheme.colors.textMuted,
            "${item.actorName} updated the pull request",
            item.date,
        )
        is ActivityItem.Commented -> CommentRow(item.comment, onReply, onResolve)
    }
}

@Composable
private fun EventRow(icon: ImageVector, tint: Color, text: String, date: String) {
    val spacing = JengaTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        JengaIcon(icon, contentDescription = null, tint = tint)
        JengaText(
            text = text,
            style = JengaTheme.typography.bodySmall,
            color = JengaTheme.colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        JengaText(
            text = date.substringBefore('T'),
            style = JengaTheme.typography.caption,
            color = JengaTheme.colors.textMuted,
        )
    }
}

@Composable
private fun CommentRow(comment: PrComment, onReply: (PrComment) -> Unit, onResolve: (PrComment) -> Unit) {
    val spacing = JengaTheme.spacing
    val isReply = comment.parentId != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) spacing.lg else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        JengaAvatar(name = comment.authorName, size = JengaAvatarSize.Small)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                JengaText(
                    text = comment.authorName,
                    style = JengaTheme.typography.bodySmall,
                    color = JengaTheme.colors.textPrimary,
                )
                JengaText(
                    text = comment.createdOn.substringBefore('T'),
                    style = JengaTheme.typography.caption,
                    color = JengaTheme.colors.textMuted,
                )
                if (comment.resolved) {
                    JengaText(
                        text = "Resolved",
                        style = JengaTheme.typography.caption,
                        color = JengaTheme.colors.success,
                    )
                }
            }
            comment.inlinePath?.let { path ->
                JengaText(
                    text = "on ${path.substringAfterLast('/')}",
                    style = JengaTheme.typography.caption,
                    color = JengaTheme.colors.textMuted,
                )
            }
            PlatypusMarkdown(content = comment.content)
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                JengaText(
                    text = "Reply",
                    style = JengaTheme.typography.caption,
                    color = JengaTheme.colors.brand,
                    modifier = Modifier.clickable { onReply(comment) },
                )
                JengaText(
                    text = if (comment.resolved) "Reopen" else "Resolve",
                    style = JengaTheme.typography.caption,
                    color = if (comment.resolved) JengaTheme.colors.textMuted else JengaTheme.colors.success,
                    modifier = Modifier.clickable { onResolve(comment) },
                )
            }
        }
    }
}

@Composable
private fun PrDetailSkeleton(modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues()) {
    val spacing = JengaTheme.spacing
    val shape = JengaTheme.shapes.control
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Box(Modifier.height(20.dp).fillMaxWidth(0.35f).clip(shape).jengaShimmer())
                Box(Modifier.height(24.dp).fillMaxWidth(0.9f).clip(shape).jengaShimmer())
                Box(Modifier.height(16.dp).fillMaxWidth(0.6f).clip(shape).jengaShimmer())
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    Box(Modifier.size(24.dp).clip(JengaTheme.shapes.pill).jengaShimmer())
                    Box(Modifier.height(14.dp).fillMaxWidth(0.5f).clip(shape).jengaShimmer())
                }
            }
        }
        items(3) {
            Box(Modifier.height(64.dp).fillMaxWidth().clip(JengaTheme.shapes.card).jengaShimmer())
        }
    }
}

private fun reviewerSummary(reviewers: List<PrReviewer>): String {
    val approved = reviewers.count { it.approval == PrApproval.APPROVED }
    val changes = reviewers.count { it.approval == PrApproval.CHANGES_REQUESTED }
    val pending = reviewers.count { it.approval == PrApproval.NONE }
    return buildList {
        if (approved > 0) add("$approved approved")
        if (changes > 0) add("$changes changes requested")
        if (pending > 0) add("$pending pending")
    }.joinToString(" · ").ifEmpty { "No responses yet" }
}

private fun mergeStrategyLabel(strategy: MergeStrategy): String = when (strategy) {
    MergeStrategy.MERGE_COMMIT -> "Merge commit"
    MergeStrategy.SQUASH -> "Squash"
    MergeStrategy.FAST_FORWARD -> "Fast forward"
}

private fun mergeStrategyHint(strategy: MergeStrategy): String = when (strategy) {
    MergeStrategy.MERGE_COMMIT -> "Keeps every commit and adds a merge commit."
    MergeStrategy.SQUASH -> "Combines all commits into one on the destination."
    MergeStrategy.FAST_FORWARD -> "Moves the branch pointer, no merge commit."
}

private fun stateLozenge(state: PrState): Pair<String, JengaBadgeTone>? = when (state) {
    PrState.OPEN -> "Open" to JengaBadgeTone.Success
    PrState.MERGED -> "Merged" to JengaBadgeTone.Brand
    PrState.DECLINED -> "Declined" to JengaBadgeTone.Error
    PrState.SUPERSEDED -> "Superseded" to JengaBadgeTone.Neutral
    PrState.OTHER -> null
}

private fun approvalPill(approval: PrApproval): Pair<String, JengaBadgeTone> = when (approval) {
    PrApproval.APPROVED -> "Approved" to JengaBadgeTone.Success
    PrApproval.CHANGES_REQUESTED -> "Changes requested" to JengaBadgeTone.Warning
    PrApproval.NONE -> "Pending" to JengaBadgeTone.Neutral
}
