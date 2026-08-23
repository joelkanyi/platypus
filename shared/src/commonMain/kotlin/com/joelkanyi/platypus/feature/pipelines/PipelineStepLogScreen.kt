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
package com.joelkanyi.platypus.feature.pipelines

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.joelkanyi.platypus.designsystem.PlatypusCodeView
import com.joelkanyi.platypus.designsystem.toSp
import com.joelkanyi.platypus.domain.repository.PipelineRepository
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.chip.JengaChip
import io.github.joelkanyi.jenga.component.divider.JengaDivider
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.search.JengaSearchField
import io.github.joelkanyi.jenga.component.state.JengaEmptyState
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.component.status.JengaStatusPill
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_LOG_LINES = 20_000

@Immutable
data class StepLogUiState(val isLoading: Boolean = true, val error: String? = null, val raw: String = "")

class PipelineStepLogViewModel(
    private val repository: PipelineRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
    private val pipelineUuid: String,
    private val stepUuid: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StepLogUiState())
    val uiState: StateFlow<StepLogUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.stepLog(accountId, workspace, repoSlug, pipelineUuid, stepUuid)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, raw = result.data) }
                    poll()
                }
                is NetworkResult.Failure ->
                    _uiState.update { it.copy(isLoading = false, error = result.userMessage()) }
            }
        }
    }

    private fun poll() {
        viewModelScope.launch {
            while (true) {
                delay(POLL_MS)
                val result = repository.stepLog(accountId, workspace, repoSlug, pipelineUuid, stepUuid)
                if (result !is NetworkResult.Success) continue
                if (result.data == _uiState.value.raw) break
                _uiState.update { it.copy(raw = result.data) }
            }
        }
    }

    private companion object {
        const val POLL_MS = 8_000L
    }
}

@Composable
fun PipelineStepLogScreen(
    accountId: String,
    workspace: String,
    repoSlug: String,
    pipelineUuid: String,
    stepUuid: String,
    stepName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "steplog/$pipelineUuid/$stepUuid") {
        PipelineStepLogViewModel(
            dependencies.pipelineRepository,
            accountId,
            workspace,
            repoSlug,
            pipelineUuid,
            stepUuid,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by dependencies.settingsStore.settings.collectAsStateWithLifecycle()

    StepLogContent(
        stepName = stepName,
        state = state,
        initialWrap = settings.wrapCode,
        fontSize = settings.codeFontSize.toSp(),
        onRetry = viewModel::retry,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun StepLogContent(
    stepName: String,
    state: StepLogUiState,
    initialWrap: Boolean,
    fontSize: TextUnit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    val colors = JengaTheme.colors
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var wrap by remember { mutableStateOf(initialWrap) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var matchCursor by remember { mutableStateOf(0) }
    var highlightedLine by remember { mutableStateOf<Int?>(null) }
    var landedOnError by remember { mutableStateOf(false) }

    val logView = remember(state.raw) {
        annotateLog(state.raw, colors.error, colors.warning, MAX_LOG_LINES)
    }
    val matches = remember(query, logView) {
        if (query.isBlank()) {
            emptyList()
        } else {
            logView.lines.mapIndexedNotNull { i, l -> if (l.text.contains(query, ignoreCase = true)) i else null }
        }
    }

    LaunchedEffect(logView) {
        if (!landedOnError && logView.errorLines.isNotEmpty()) {
            landedOnError = true
            val target = logView.errorLines.first()
            highlightedLine = target
            listState.scrollToItem(target)
        }
    }

    val atBottom by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            last == null || last.index >= logView.lines.lastIndex
        }
    }

    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = stepName,
                subtitle = "Log",
                navigationIcon = {
                    JengaIconButton(onClick = onBack) {
                        JengaIcon(JengaIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    JengaIconButton(onClick = { searchOpen = !searchOpen }) {
                        JengaIcon(JengaIcons.Search, contentDescription = "Search log")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.error != null -> JengaErrorState(
                title = "Couldn't load log",
                description = state.error,
                actionLabel = "Try again",
                onAction = onRetry,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            state.isLoading -> JengaEmptyState(
                title = "Loading log…",
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            logView.lines.all { it.text.isEmpty() } -> JengaEmptyState(
                title = "No output",
                description = "This step produced no log output.",
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            else -> Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LogHeaderStrip(
                        wrap = wrap,
                        errorCount = logView.errorLines.size,
                        onToggleWrap = { wrap = !wrap },
                        onJumpToError = {
                            val target = logView.errorLines.firstOrNull() ?: return@LogHeaderStrip
                            highlightedLine = target
                            scope.launch { listState.animateScrollToItem(target) }
                        },
                    )
                    if (searchOpen) {
                        LogSearchBar(
                            query = query,
                            matchCount = matches.size,
                            matchCursor = if (matches.isEmpty()) 0 else matchCursor + 1,
                            onQueryChange = {
                                query = it
                                matchCursor = 0
                            },
                            onPrev = {
                                if (matches.isNotEmpty()) {
                                    matchCursor = (matchCursor - 1 + matches.size) % matches.size
                                    highlightedLine = matches[matchCursor]
                                    scope.launch { listState.animateScrollToItem(matches[matchCursor]) }
                                }
                            },
                            onNext = {
                                if (matches.isNotEmpty()) {
                                    matchCursor = (matchCursor + 1) % matches.size
                                    highlightedLine = matches[matchCursor]
                                    scope.launch { listState.animateScrollToItem(matches[matchCursor]) }
                                }
                            },
                        )
                    }
                    if (logView.truncated) {
                        JengaText(
                            text = "Showing the last $MAX_LOG_LINES lines.",
                            style = JengaTheme.typography.caption,
                            color = JengaTheme.colors.textMuted,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceVariant)
                                .padding(horizontal = spacing.md, vertical = spacing.xs),
                        )
                    }
                    PlatypusCodeView(
                        lines = logView.lines,
                        wrap = wrap,
                        listState = listState,
                        fontSize = fontSize,
                        highlightedLine = highlightedLine,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.surfaceSunk)
                            .padding(horizontal = spacing.sm),
                    )
                }
                if (!atBottom && logView.lines.size > 1) {
                    JengaStatusPill(
                        label = "Jump to latest",
                        onClick = { scope.launch { listState.scrollToItem(logView.lines.lastIndex) } },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = spacing.lg),
                    )
                }
            }
        }
    }
}

@Composable
private fun LogHeaderStrip(wrap: Boolean, errorCount: Int, onToggleWrap: () -> Unit, onJumpToError: () -> Unit) {
    val spacing = JengaTheme.spacing
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(JengaTheme.colors.surface)
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            if (errorCount > 0) {
                JengaChip(
                    label = if (errorCount == 1) "First error" else "Errors ($errorCount)",
                    selected = false,
                    onClick = onJumpToError,
                    leadingIcon = {
                        JengaIcon(
                            JengaIcons.Close,
                            contentDescription = null,
                            tint = JengaTheme.colors.error,
                            size = 14.dp,
                        )
                    },
                )
            }
            Box(modifier = Modifier.weight(1f))
            JengaChip(label = if (wrap) "Wrap" else "No wrap", selected = wrap, onClick = onToggleWrap)
        }
        JengaDivider()
    }
}

@Composable
private fun LogSearchBar(
    query: String,
    matchCount: Int,
    matchCursor: Int,
    onQueryChange: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val spacing = JengaTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JengaTheme.colors.surface)
            .padding(horizontal = spacing.md, vertical = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        JengaSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "Search log",
            modifier = Modifier.weight(1f),
        )
        JengaText(
            text = if (query.isBlank()) "" else "$matchCursor/$matchCount",
            style = JengaTheme.typography.caption,
            color = JengaTheme.colors.textMuted,
        )
        JengaIconButton(onClick = onPrev) {
            JengaIcon(JengaIcons.ChevronUp, contentDescription = "Previous match")
        }
        JengaIconButton(onClick = onNext) {
            JengaIcon(JengaIcons.ChevronDown, contentDescription = "Next match")
        }
    }
}
