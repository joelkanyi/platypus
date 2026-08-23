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
package com.joelkanyi.platypus.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.designsystem.PlatypusListSkeleton
import com.joelkanyi.platypus.designsystem.rememberCodeFontFamily
import com.joelkanyi.platypus.domain.model.CodeLine
import com.joelkanyi.platypus.domain.model.CodeSearchResult
import io.github.joelkanyi.jenga.component.avatar.JengaAvatar
import io.github.joelkanyi.jenga.component.badge.JengaBadge
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.card.JengaCard
import io.github.joelkanyi.jenga.component.card.JengaCardVariant
import io.github.joelkanyi.jenga.component.chip.JengaChip
import io.github.joelkanyi.jenga.component.feedback.JengaBottomSheet
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.search.JengaSearchField
import io.github.joelkanyi.jenga.component.state.JengaEmptyState
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme

private const val SNIPPET_LINES = 3

@Composable
fun SearchScreen(
    accountId: String?,
    workspaceSlug: String?,
    repoSlug: String?,
    repoName: String?,
    onOpenCode: (accountId: String, CodeSearchResult) -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "search/${repoSlug ?: "workspace"}") {
        SearchViewModel(
            authRepository = dependencies.authRepository,
            searchRepository = dependencies.searchRepository,
            scopeAccountId = accountId,
            scopeWorkspaceSlug = workspaceSlug,
            scopeRepoSlug = repoSlug,
            scopeRepoName = repoName,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SearchContent(
        state = state,
        onEvent = viewModel::onEvent,
        onOpenCode = onOpenCode,
        onBack = onBack,
        modifier = modifier,
    )

    if (state.showWorkspacePicker) {
        WorkspacePickerSheet(
            workspaces = state.workspaces,
            selectedId = state.selected?.id,
            onSelect = { viewModel.onEvent(SearchUiEvent.SelectWorkspace(it)) },
            onDismiss = { viewModel.onEvent(SearchUiEvent.DismissWorkspacePicker) },
        )
    }
}

@Composable
internal fun SearchContent(
    state: SearchUiState,
    onEvent: (SearchUiEvent) -> Unit,
    onOpenCode: (accountId: String, CodeSearchResult) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = state.repoScope?.repoName ?: "Search",
                subtitle = state.repoScope?.let { "Search code" },
                navigationIcon = {
                    if (onBack != null) {
                        JengaIconButton(onClick = onBack) {
                            JengaIcon(JengaIcons.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoadingWorkspaces -> PlatypusListSkeleton(
                count = 6,
                contentPadding = PaddingValues(JengaTheme.spacing.lg),
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            state.workspacesError != null && state.workspaces.isEmpty() -> JengaErrorState(
                title = "Couldn't load workspaces",
                description = state.workspacesError,
                actionLabel = "Try again",
                onAction = { onEvent(SearchUiEvent.RetryWorkspaces) },
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            else -> Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                val accountId = state.repoScope?.accountId ?: state.selected?.accountId.orEmpty()
                SearchHeader(state = state, onEvent = onEvent)
                CodeResults(
                    state = state,
                    onEvent = onEvent,
                    onOpen = { onOpenCode(accountId, it) },
                )
            }
        }
    }
}

@Composable
private fun SearchHeader(state: SearchUiState, onEvent: (SearchUiEvent) -> Unit) {
    val spacing = JengaTheme.spacing
    if (state.repoScope == null) {
        WorkspaceCard(
            workspace = state.selected,
            onSwitch = { onEvent(SearchUiEvent.OpenWorkspacePicker) },
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
        )
    }
    JengaSearchField(
        value = state.query,
        onValueChange = { onEvent(SearchUiEvent.QueryChanged(it)) },
        placeholder = state.repoScope?.let { "Find in ${it.repoName}" }
            ?: "Search code in ${state.selected?.name.orEmpty()}",
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
    )
}

@Composable
private fun WorkspaceCard(workspace: SearchWorkspace?, onSwitch: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = JengaTheme.spacing
    val colors = JengaTheme.colors
    JengaCard(variant = JengaCardVariant.Outlined, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            JengaAvatar(name = workspace?.name ?: "?")
            Column(modifier = Modifier.weight(1f)) {
                JengaText(
                    text = workspace?.name ?: "Select workspace",
                    style = JengaTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                )
                JengaText(
                    text = workspace?.accountLabel?.let { "Workspace · $it" } ?: "Workspace",
                    style = JengaTheme.typography.caption,
                    color = colors.textMuted,
                    maxLines = 1,
                )
            }
            JengaChip(label = "Switch", selected = false, onClick = onSwitch)
        }
    }
}

@Composable
private fun CodeResults(state: SearchUiState, onEvent: (SearchUiEvent) -> Unit, onOpen: (CodeSearchResult) -> Unit) {
    val spacing = JengaTheme.spacing
    val mono = rememberCodeFontFamily()
    val scopeLabel = state.repoScope?.repoName ?: state.selected?.name.orEmpty()
    val query = state.query.trim()
    when (val status = state.status) {
        SearchStatus.Idle -> JengaEmptyState(
            title = "Search code",
            description = if (state.repoScope != null) {
                "Find text in $scopeLabel. Only the default branch is indexed."
            } else {
                "Searches file contents across $scopeLabel. " +
                    "Only the default branch of each repository is indexed. Tip: narrow with repo:name."
            },
            modifier = Modifier.fillMaxSize(),
        )

        SearchStatus.Loading -> PlatypusListSkeleton(count = 4, contentPadding = PaddingValues(spacing.lg))

        SearchStatus.NoResults -> JengaEmptyState(
            title = "No matches",
            description = "No code in $scopeLabel matches “$query”. Only default branches are indexed.",
            modifier = Modifier.fillMaxSize(),
        )

        SearchStatus.BadQuery -> JengaEmptyState(
            title = "Bitbucket rejected this query",
            description = "Check the syntax. Quotes and modifiers like repo:name must be well-formed.",
            modifier = Modifier.fillMaxSize(),
        )

        SearchStatus.Gated -> JengaEmptyState(
            title = "Code search isn't available here",
            description = "This workspace is on Bitbucket's Free plan. " +
                "Bitbucket enables code search on Standard and Premium plans only.",
            modifier = Modifier.fillMaxSize(),
        )

        SearchStatus.RateLimited -> JengaErrorState(
            title = "Slow down a moment",
            description = "Bitbucket is rate-limiting search requests for this account. Try again shortly.",
            actionLabel = "Retry",
            onAction = { onEvent(SearchUiEvent.Retry) },
            modifier = Modifier.fillMaxSize(),
        )

        is SearchStatus.Error -> JengaErrorState(
            title = "Couldn't search",
            description = status.message,
            actionLabel = "Retry",
            onAction = { onEvent(SearchUiEvent.Retry) },
            modifier = Modifier.fillMaxSize(),
        )

        SearchStatus.Loaded -> Column(modifier = Modifier.fillMaxSize()) {
            val files = state.totalFiles ?: state.results.size
            JengaText(
                text = "$files ${if (files == 1) "file" else "files"}",
                style = JengaTheme.typography.caption,
                color = JengaTheme.colors.textMuted,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                items(state.results, key = { "${it.repoSlug}/${it.path}/${it.commitHash}" }) { result ->
                    CodeResultCard(
                        result = result,
                        monoFamily = mono,
                        showRepo = state.repoScope == null,
                        onClick = { onOpen(result) },
                    )
                }
                if (state.next != null) {
                    item(key = "load-more") {
                        if (state.loadMoreError) {
                            JengaListItem(
                                headline = "Couldn't load more",
                                supporting = "Tap to retry",
                                onClick = { onEvent(SearchUiEvent.LoadMore) },
                            )
                        } else {
                            LaunchedEffect(state.next) { onEvent(SearchUiEvent.LoadMore) }
                            Box(Modifier.fillMaxWidth().padding(spacing.md), contentAlignment = Alignment.Center) {
                                JengaText(text = "Loading more…", color = JengaTheme.colors.textMuted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeResultCard(result: CodeSearchResult, monoFamily: FontFamily, showRepo: Boolean, onClick: () -> Unit) {
    val spacing = JengaTheme.spacing
    val colors = JengaTheme.colors
    val codeStyle = JengaTheme.typography.caption.copy(fontFamily = monoFamily)
    val subtitle = if (showRepo) {
        listOf(result.repoName, result.directory).filter { it.isNotBlank() }.joinToString(" · ")
    } else {
        result.directory.ifBlank { result.path }
    }
    JengaCard(variant = JengaCardVariant.Outlined, modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(spacing.md), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showRepo) JengaAvatar(name = result.repoName)
                Column(modifier = Modifier.weight(1f)) {
                    JengaText(
                        text = if (result.pathSegments.isNotEmpty() && result.snippet.isEmpty()) {
                            highlightedText(CodeLine(result.pathSegments), colors.brand)
                        } else {
                            AnnotatedString(result.fileName)
                        },
                        style = JengaTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                    )
                    if (subtitle.isNotBlank()) {
                        JengaText(
                            text = subtitle,
                            style = JengaTheme.typography.caption,
                            color = colors.textMuted,
                            maxLines = 1,
                        )
                    }
                }
                if (result.matchCount > 0) JengaBadge(text = "${result.matchCount}")
            }
            val snippet = dedent(result.snippet.take(SNIPPET_LINES))
            if (snippet.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = spacing.xs)
                        .background(colors.surfaceVariant, RoundedCornerShape(spacing.xs))
                        .padding(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    snippet.forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            JengaText(
                                text = if (line.lineNumber > 0) line.lineNumber.toString() else "",
                                style = codeStyle,
                                color = colors.textMuted,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(28.dp),
                            )
                            JengaText(
                                text = highlightedText(line, colors.brand),
                                style = codeStyle,
                                color = colors.textSecondary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            if (result.matchCount > snippet.size && snippet.isNotEmpty()) {
                JengaText(
                    text = "+${result.matchCount - snippet.size} more matches",
                    style = JengaTheme.typography.caption,
                    color = colors.brand,
                )
            }
        }
    }
}

private fun highlightedText(line: CodeLine, matchColor: Color): AnnotatedString = buildAnnotatedString {
    line.segments.forEach { segment ->
        if (segment.isMatch) {
            withStyle(SpanStyle(color = matchColor, fontWeight = FontWeight.Bold)) { append(segment.text) }
        } else {
            append(segment.text)
        }
    }
}

private fun dedent(lines: List<CodeLine>): List<CodeLine> {
    if (lines.isEmpty()) return lines
    val indents = lines.mapNotNull { line ->
        val text = line.segments.joinToString("") { it.text }
        if (text.isBlank()) null else text.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 }
    }
    val trim = indents.minOrNull() ?: 0
    if (trim == 0) return lines
    return lines.map { line ->
        var remaining = trim
        val trimmed = line.segments.map { segment ->
            if (remaining <= 0) {
                segment
            } else {
                val drop = minOf(remaining, segment.text.length)
                remaining -= drop
                segment.copy(text = segment.text.substring(drop))
            }
        }
        CodeLine(trimmed)
    }
}

@Composable
private fun WorkspacePickerSheet(
    workspaces: List<SearchWorkspace>,
    selectedId: String?,
    onSelect: (SearchWorkspace) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = JengaTheme.spacing
    JengaBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = spacing.lg)) {
            JengaText(
                text = "Workspace",
                style = JengaTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
            )
            workspaces.forEach { workspace ->
                JengaListItem(
                    headline = workspace.name,
                    supporting = workspace.accountLabel,
                    leadingContent = { JengaAvatar(name = workspace.name) },
                    trailingContent = {
                        if (workspace.id == selectedId) JengaIcon(JengaIcons.Check, contentDescription = "Selected")
                    },
                    onClick = { onSelect(workspace) },
                )
            }
        }
    }
}
