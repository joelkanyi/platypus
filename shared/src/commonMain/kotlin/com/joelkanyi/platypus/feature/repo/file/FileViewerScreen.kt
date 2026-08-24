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
package com.joelkanyi.platypus.feature.repo.file

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.core.syntax.highlighterFor
import com.joelkanyi.platypus.core.syntax.outlineOf
import com.joelkanyi.platypus.designsystem.PlatypusBreadcrumb
import com.joelkanyi.platypus.designsystem.PlatypusCodeView
import com.joelkanyi.platypus.designsystem.PlatypusMarkdown
import com.joelkanyi.platypus.designsystem.crumbsFor
import com.joelkanyi.platypus.designsystem.highlightLine
import com.joelkanyi.platypus.designsystem.rememberSyntaxColors
import com.joelkanyi.platypus.domain.model.AccountId
import com.joelkanyi.platypus.domain.model.RepoFile
import com.joelkanyi.platypus.domain.model.RepoRef
import com.joelkanyi.platypus.domain.model.RepoSlug
import com.joelkanyi.platypus.domain.model.WorkspaceSlug
import com.joelkanyi.platypus.domain.repository.RepoContentRepository
import com.joelkanyi.platypus.ui.toSp
import io.github.joelkanyi.jenga.component.button.JengaIconButton
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class FileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val file: RepoFile? = null,
    val findActive: Boolean = false,
    val findQuery: String = "",
    val matches: List<Int> = emptyList(),
    val matchIndex: Int = 0,
    val outlineOpen: Boolean = false,
    val isMarkdown: Boolean = false,
    val preview: Boolean = false,
    val defaultBranch: String? = null,
) {
    val currentMatchLine: Int? get() = matches.getOrNull(matchIndex)
}

class FileViewerViewModel(
    private val repoContentRepository: RepoContentRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
    private val ref: String,
    private val path: String,
    renderMarkdownDefault: Boolean,
    private val fromSearch: Boolean = false,
) : ViewModel() {

    private val isMarkdown = path.substringAfterLast('.', "").lowercase() in MARKDOWN_EXTENSIONS

    private val repoRef = RepoRef(AccountId(accountId), WorkspaceSlug(workspace), RepoSlug(repoSlug))

    private val _uiState =
        MutableStateFlow(FileUiState(isMarkdown = isMarkdown, preview = isMarkdown && renderMarkdownDefault))
    val uiState: StateFlow<FileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun togglePreview() = _uiState.update { it.copy(preview = !it.preview) }

    fun toggleFind() = _uiState.update {
        if (it.findActive) {
            it.copy(findActive = false, findQuery = "", matches = emptyList(), matchIndex = 0)
        } else {
            it.copy(findActive = true)
        }
    }

    fun onFindQuery(query: String) = _uiState.update { state ->
        val lines = state.file?.lines ?: emptyList()
        val jumpLine = query.removePrefix(":").toIntOrNull()?.takeIf { query.startsWith(":") }
        val matches = when {
            query.isBlank() -> emptyList()
            jumpLine != null -> listOf((jumpLine - 1).coerceIn(0, (lines.size - 1).coerceAtLeast(0)))
            else -> lines.indices.filter { lines[it].contains(query, ignoreCase = true) }
        }
        state.copy(findQuery = query, matches = matches, matchIndex = 0)
    }

    fun toggleOutline() = _uiState.update { it.copy(outlineOpen = !it.outlineOpen) }

    fun jumpTo(line: Int) = _uiState.update {
        it.copy(outlineOpen = false, matches = listOf(line), matchIndex = 0)
    }

    fun nextMatch() = _uiState.update {
        if (it.matches.isEmpty()) it else it.copy(matchIndex = (it.matchIndex + 1) % it.matches.size)
    }

    fun previousMatch() = _uiState.update {
        if (it.matches.isEmpty()) it else it.copy(matchIndex = (it.matchIndex - 1 + it.matches.size) % it.matches.size)
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repoContentRepository.file(repoRef, ref, path)) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, file = result.data) }
                is NetworkResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.userMessage()) }
            }
        }
        if (fromSearch) loadDefaultBranch()
    }

    private fun loadDefaultBranch() {
        viewModelScope.launch {
            val result = repoContentRepository.repository(repoRef)
            if (result is NetworkResult.Success) {
                _uiState.update { it.copy(defaultBranch = result.data.defaultBranch) }
            }
        }
    }
}

@Composable
fun FileViewerScreen(
    accountId: String,
    workspace: String,
    repoSlug: String,
    ref: String,
    path: String,
    onNavigateToPath: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    fromSearch: Boolean = false,
    onViewLatest: (defaultRef: String) -> Unit = {},
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "$accountId/$workspace/$repoSlug/$ref/$path") {
        FileViewerViewModel(
            dependencies.repoContentRepository,
            accountId,
            workspace,
            repoSlug,
            ref,
            path,
            dependencies.settingsStore.settings.value.renderMarkdownByDefault,
            fromSearch,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by dependencies.settingsStore.settings.collectAsStateWithLifecycle()

    FileViewerContent(
        fileName = path.substringAfterLast('/'),
        repoLabel = repoSlug,
        path = path,
        wrap = settings.wrapCode,
        fontSize = settings.codeFontSize.toSp(),
        onNavigateToPath = onNavigateToPath,
        onBack = onBack,
        searchedVersion = if (fromSearch) ref else null,
        onViewLatest = { state.defaultBranch?.let(onViewLatest) },
        state = state,
        onRetry = viewModel::retry,
        onTogglePreview = viewModel::togglePreview,
        onToggleWrap = { dependencies.settingsStore.update(settings.copy(wrapCode = !settings.wrapCode)) },
        onToggleFind = viewModel::toggleFind,
        onFindQuery = viewModel::onFindQuery,
        onNextMatch = viewModel::nextMatch,
        onPreviousMatch = viewModel::previousMatch,
        onToggleOutline = viewModel::toggleOutline,
        onJumpTo = viewModel::jumpTo,
        onOpenUrl = onOpenUrl,
        modifier = modifier,
    )
}

@Composable
private fun SearchedVersionBanner(commitHash: String, onViewLatest: () -> Unit) {
    val spacing = JengaTheme.spacing
    val colors = JengaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceVariant)
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JengaText(
            text = "Showing the searched version (${commitHash.take(7)}).",
            style = JengaTheme.typography.caption,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        JengaText(
            text = "View latest",
            style = JengaTheme.typography.caption,
            color = colors.brand,
            modifier = Modifier.clickable(onClick = onViewLatest),
        )
    }
}

@Composable
internal fun FileViewerContent(
    fileName: String,
    repoLabel: String,
    path: String,
    wrap: Boolean,
    fontSize: TextUnit,
    onNavigateToPath: (String) -> Unit,
    onBack: () -> Unit,
    searchedVersion: String?,
    onViewLatest: () -> Unit,
    state: FileUiState,
    onRetry: () -> Unit,
    onTogglePreview: () -> Unit,
    onToggleWrap: () -> Unit,
    onToggleFind: () -> Unit,
    onFindQuery: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPreviousMatch: () -> Unit,
    onToggleOutline: () -> Unit,
    onJumpTo: (Int) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val file = state.file

    JengaScaffold(
        modifier = modifier,
        topBar = {
            Column {
                JengaTopAppBar(
                    title = fileName,
                    navigationIcon = {
                        JengaIconButton(onClick = onBack) {
                            JengaIcon(JengaIcons.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (file != null && file.renderable) {
                            if (state.isMarkdown) {
                                JengaIconButton(onClick = onTogglePreview) {
                                    JengaIcon(
                                        if (state.preview) JengaIcons.EyeOff else JengaIcons.Eye,
                                        contentDescription = if (state.preview) "View source" else "Preview",
                                    )
                                }
                            }
                            if (!state.preview) {
                                JengaIconButton(onClick = onToggleOutline) {
                                    JengaIcon(JengaIcons.Sliders, contentDescription = "Outline")
                                }
                                JengaIconButton(onClick = onToggleFind) {
                                    JengaIcon(JengaIcons.Search, contentDescription = "Find in file")
                                }
                                JengaIconButton(onClick = onToggleWrap) {
                                    JengaIcon(JengaIcons.Swap, contentDescription = "Toggle wrap")
                                }
                            }
                        }
                        file?.webUrl?.let { url ->
                            JengaIconButton(onClick = { onOpenUrl(url) }) {
                                JengaIcon(JengaIcons.Share, contentDescription = "Open on web")
                            }
                        }
                    },
                )
                if (searchedVersion != null) {
                    SearchedVersionBanner(commitHash = searchedVersion, onViewLatest = onViewLatest)
                }
            }
        },
    ) { innerPadding ->
        when {
            state.error != null -> JengaErrorState(
                title = "Couldn't load file",
                description = state.error,
                actionLabel = "Try again",
                onAction = onRetry,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            file != null && !file.renderable -> JengaEmptyState(
                title = "Can't preview this file",
                description = "It's binary or too large to show here.",
                actionLabel = file.webUrl?.let { "Open on web" },
                onAction = file.webUrl?.let { url -> { onOpenUrl(url) } },
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            file != null && state.preview -> MarkdownFile(
                content = file.lines.joinToString("\n"),
                repoLabel = repoLabel,
                path = path,
                truncatedAtLine = file.truncatedAtLine,
                onNavigateToPath = onNavigateToPath,
                contentPadding = innerPadding,
            )

            file != null -> RenderableFile(
                state = state,
                file = file,
                wrap = wrap,
                fontSize = fontSize,
                repoLabel = repoLabel,
                path = path,
                onNavigateToPath = onNavigateToPath,
                onFindQuery = onFindQuery,
                onNextMatch = onNextMatch,
                onPreviousMatch = onPreviousMatch,
                contentPadding = innerPadding,
            )

            else -> Unit
        }
    }

    if (state.outlineOpen && file != null && file.renderable) {
        OutlineSheet(fileName = fileName, lines = file.lines, onSelect = onJumpTo, onDismiss = onToggleOutline)
    }
}

@Composable
private fun OutlineSheet(fileName: String, lines: List<String>, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val spacing = JengaTheme.spacing
    val entries = remember(fileName, lines) { outlineOf(fileName, lines) }
    JengaBottomSheet(onDismissRequest = onDismiss) {
        JengaText(
            text = "Outline",
            style = JengaTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
        )
        if (entries.isEmpty()) {
            JengaText(
                text = "No symbols found in this file.",
                color = JengaTheme.colors.textMuted,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(entries, key = { it.line }) { entry ->
                    JengaListItem(
                        headline = entry.label,
                        supporting = "Line ${entry.line + 1}",
                        onClick = { onSelect(entry.line) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownFile(
    content: String,
    repoLabel: String,
    path: String,
    truncatedAtLine: Int?,
    onNavigateToPath: (String) -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = JengaTheme.spacing
    LazyColumn(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        item {
            PlatypusBreadcrumb(
                crumbs = crumbsFor(repoLabel, path),
                onNavigate = onNavigateToPath,
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
            )
        }
        if (truncatedAtLine != null) {
            item {
                JengaText(
                    text = "Showing the first $truncatedAtLine lines. Open on web for the full file.",
                    style = JengaTheme.typography.caption,
                    color = JengaTheme.colors.warning,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.md, vertical = spacing.xs),
                )
            }
        }
        item {
            PlatypusMarkdown(
                content = content,
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
            )
        }
    }
}

@Composable
private fun RenderableFile(
    state: FileUiState,
    file: RepoFile,
    wrap: Boolean,
    fontSize: TextUnit,
    repoLabel: String,
    path: String,
    onNavigateToPath: (String) -> Unit,
    onFindQuery: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPreviousMatch: () -> Unit,
    contentPadding: PaddingValues,
) {
    val spacing = JengaTheme.spacing
    val syntaxColors = rememberSyntaxColors()
    val highlighter = remember(file.path) { highlighterFor(file.name) }
    val annotated = remember(file.path, syntaxColors) {
        file.lines.map { highlightLine(it, highlighter, syntaxColors) }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(state.matchIndex, state.currentMatchLine) {
        state.currentMatchLine?.let { listState.animateScrollToItem(it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        PlatypusBreadcrumb(
            crumbs = crumbsFor(repoLabel, path),
            onNavigate = onNavigateToPath,
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
        )
        if (state.findActive) {
            FindBar(
                state = state,
                onFindQuery = onFindQuery,
                onNextMatch = onNextMatch,
                onPreviousMatch = onPreviousMatch,
            )
        }
        if (file.truncatedAtLine != null) {
            JengaText(
                text = "Showing the first ${file.truncatedAtLine} lines. Open on web for the full file.",
                style = JengaTheme.typography.caption,
                color = JengaTheme.colors.warning,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.md, vertical = spacing.xs),
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(syntaxColors.ground)) {
            PlatypusCodeView(
                lines = annotated,
                wrap = wrap,
                listState = listState,
                fontSize = fontSize,
                gutterColor = syntaxColors.gutter,
                highlightedLine = state.currentMatchLine,
                modifier = Modifier.fillMaxSize().padding(horizontal = spacing.sm),
            )
        }
    }
}

@Composable
private fun FindBar(
    state: FileUiState,
    onFindQuery: (String) -> Unit,
    onNextMatch: () -> Unit,
    onPreviousMatch: () -> Unit,
) {
    val spacing = JengaTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        JengaSearchField(
            value = state.findQuery,
            onValueChange = onFindQuery,
            placeholder = "Find in file, or :line",
            modifier = Modifier.weight(1f),
        )
        JengaText(
            text = if (state.matches.isEmpty()) "0" else "${state.matchIndex + 1}/${state.matches.size}",
            style = JengaTheme.typography.caption,
            color = JengaTheme.colors.textMuted,
        )
        JengaIconButton(onClick = onPreviousMatch) {
            JengaIcon(JengaIcons.ChevronUp, contentDescription = "Previous match")
        }
        JengaIconButton(onClick = onNextMatch) {
            JengaIcon(JengaIcons.ChevronDown, contentDescription = "Next match")
        }
    }
}

private val MARKDOWN_EXTENSIONS = setOf("md", "markdown", "mdown", "mkd")
