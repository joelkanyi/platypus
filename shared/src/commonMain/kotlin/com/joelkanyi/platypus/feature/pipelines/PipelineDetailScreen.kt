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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.designsystem.expand
import com.joelkanyi.platypus.designsystem.formatDuration
import com.joelkanyi.platypus.designsystem.relativeTime
import com.joelkanyi.platypus.designsystem.shortDate
import com.joelkanyi.platypus.domain.model.Pipeline
import com.joelkanyi.platypus.domain.model.PipelineStep
import com.joelkanyi.platypus.domain.model.rerunRequest
import com.joelkanyi.platypus.domain.repository.PipelineRepository
import io.github.joelkanyi.jenga.component.avatar.JengaAvatar
import io.github.joelkanyi.jenga.component.avatar.JengaAvatarSize
import io.github.joelkanyi.jenga.component.button.JengaButton
import io.github.joelkanyi.jenga.component.button.JengaButtonVariant
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.card.JengaCard
import io.github.joelkanyi.jenga.component.card.JengaCardVariant
import io.github.joelkanyi.jenga.component.divider.JengaDivider
import io.github.joelkanyi.jenga.component.feedback.JengaDialog
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class PipelineDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val pipeline: Pipeline? = null,
    val steps: List<PipelineStep> = emptyList(),
    val actionInProgress: Boolean = false,
    val actionError: String? = null,
)

class PipelineDetailViewModel(
    private val repository: PipelineRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
    private val pipelineUuid: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PipelineDetailUiState())
    val uiState: StateFlow<PipelineDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        poll()
    }

    fun retry() = load()

    fun clearActionError() = _uiState.update { it.copy(actionError = null) }

    fun stop() {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionError = null) }
            when (val result = repository.stop(accountId, workspace, repoSlug, pipelineUuid)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(actionInProgress = false) }
                    load()
                }
                is NetworkResult.Failure ->
                    _uiState.update { it.copy(actionInProgress = false, actionError = result.userMessage()) }
            }
        }
    }

    fun rerun(onTriggered: (Pipeline) -> Unit) {
        val request = _uiState.value.pipeline?.rerunRequest() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionError = null) }
            when (val result = repository.trigger(accountId, workspace, repoSlug, request)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(actionInProgress = false) }
                    onTriggered(result.data)
                }
                is NetworkResult.Failure ->
                    _uiState.update { it.copy(actionInProgress = false, actionError = result.userMessage()) }
            }
        }
    }

    private fun poll() {
        viewModelScope.launch {
            while (true) {
                delay(POLL_MS)
                val state = _uiState.value
                if (!state.isLoading && !state.actionInProgress && state.pipeline?.status?.isRunning == true) {
                    silentReload()
                }
            }
        }
    }

    private suspend fun silentReload() {
        val pipeline = repository.pipeline(accountId, workspace, repoSlug, pipelineUuid)
        if (pipeline is NetworkResult.Success) {
            _uiState.update { if (it.pipeline == pipeline.data) it else it.copy(pipeline = pipeline.data) }
        }
        val steps = repository.steps(accountId, workspace, repoSlug, pipelineUuid)
        if (steps is NetworkResult.Success) {
            _uiState.update { if (it.steps == steps.data) it else it.copy(steps = steps.data) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.pipeline(accountId, workspace, repoSlug, pipelineUuid)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, pipeline = result.data) }
                    loadSteps()
                }
                is NetworkResult.Failure ->
                    _uiState.update { it.copy(isLoading = false, error = result.userMessage()) }
            }
        }
    }

    private fun loadSteps() {
        viewModelScope.launch {
            val result = repository.steps(accountId, workspace, repoSlug, pipelineUuid)
            if (result is NetworkResult.Success) _uiState.update { it.copy(steps = result.data) }
        }
    }

    private companion object {
        const val POLL_MS = 10_000L
    }
}

@Composable
fun PipelineDetailScreen(
    accountId: String,
    workspace: String,
    repoSlug: String,
    pipelineUuid: String,
    buildNumber: Long,
    onOpenStepLog: (PipelineStep) -> Unit,
    onOpenPipeline: (Pipeline) -> Unit,
    onOpenCommit: (String) -> Unit,
    onOpenPullRequest: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "pipeline/$accountId/$workspace/$repoSlug/$pipelineUuid") {
        PipelineDetailViewModel(dependencies.pipelineRepository, accountId, workspace, repoSlug, pipelineUuid)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val webUrl = "https://bitbucket.org/$workspace/$repoSlug/pipelines/results/$buildNumber"

    PipelineDetailContent(
        buildNumber = buildNumber,
        repoName = repoSlug,
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onStop = viewModel::stop,
        onRerun = { viewModel.rerun(onOpenPipeline) },
        onDismissActionError = viewModel::clearActionError,
        onOpenStepLog = onOpenStepLog,
        onOpenCommit = onOpenCommit,
        onOpenPullRequest = onOpenPullRequest,
        onOpenWeb = { dependencies.openUrl(webUrl) },
        modifier = modifier,
    )
}

@Composable
internal fun PipelineDetailContent(
    buildNumber: Long,
    repoName: String,
    state: PipelineDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onStop: () -> Unit,
    onRerun: () -> Unit,
    onDismissActionError: () -> Unit,
    onOpenStepLog: (PipelineStep) -> Unit,
    onOpenCommit: (String) -> Unit = {},
    onOpenPullRequest: (Long) -> Unit = {},
    onOpenWeb: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    val listState = rememberLazyListState()
    var showStopConfirm by remember { mutableStateOf(false) }
    var autoScrolled by remember { mutableStateOf(false) }

    val steps = state.steps
    val firstFailedIndex = steps.indexOfFirst { it.status.visual() == PipelineVisualStatus.Failed }
    LaunchedEffect(steps) {
        if (!autoScrolled && firstFailedIndex >= 0) {
            autoScrolled = true
            listState.animateScrollToItem(STEPS_HEADER_OFFSET + firstFailedIndex)
        }
    }

    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = "Pipeline #$buildNumber",
                subtitle = repoName.takeIf { it.isNotBlank() },
                navigationIcon = {
                    JengaIconButton(onClick = onBack) {
                        JengaIcon(JengaIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    JengaIconButton(onClick = onOpenWeb) {
                        JengaIcon(JengaIcons.Share, contentDescription = "Open on web")
                    }
                },
            )
        },
    ) { innerPadding ->
        val pipeline = state.pipeline
        when {
            state.error != null -> JengaErrorState(
                title = "Couldn't load pipeline",
                description = state.error,
                actionLabel = "Try again",
                onAction = onRetry,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            pipeline != null -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding.expand(horizontal = spacing.lg, vertical = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                item {
                    PipelineHero(
                        pipeline = pipeline,
                        steps = steps,
                        actionInProgress = state.actionInProgress,
                        onRerun = onRerun,
                        onStopClick = { showStopConfirm = true },
                    )
                }
                if (state.actionError != null) {
                    item { ActionErrorCard(state.actionError, onDismissActionError) }
                }
                item { PipelineMetadataCard(pipeline, onOpenCommit, onOpenPullRequest) }
                item {
                    JengaText(
                        text = "STEPS",
                        style = JengaTheme.typography.caption,
                        color = JengaTheme.colors.textMuted,
                        modifier = Modifier.padding(top = spacing.sm, bottom = spacing.xs),
                    )
                }
                if (steps.isEmpty()) {
                    item {
                        JengaText(
                            text = "No steps reported for this run.",
                            style = JengaTheme.typography.bodySmall,
                            color = JengaTheme.colors.textMuted,
                        )
                    }
                } else {
                    itemsIndexed(steps, key = { _, step -> step.uuid }) { index, step ->
                        PipelineStepRow(
                            step = step,
                            isFirst = index == 0,
                            isLast = index == steps.lastIndex,
                            onClick = { onOpenStepLog(step) },
                        )
                    }
                }
            }

            else -> Unit
        }
    }

    if (showStopConfirm && state.pipeline?.status?.isRunning == true) {
        val target = state.pipeline.refName?.takeIf { it.isNotBlank() } ?: "this branch"
        JengaDialog(
            onDismissRequest = { showStopConfirm = false },
            title = "Stop pipeline #$buildNumber?",
            text = "This stops the run on $target. Steps in progress will be halted.",
            confirmButton = {
                JengaButton(
                    text = "Stop",
                    onClick = {
                        showStopConfirm = false
                        onStop()
                    },
                    variant = JengaButtonVariant.Primary,
                )
            },
            dismissButton = {
                JengaButton(text = "Cancel", onClick = {
                    showStopConfirm = false
                }, variant = JengaButtonVariant.Outline)
            },
        )
    }
}

@Composable
private fun PipelineHero(
    pipeline: Pipeline,
    steps: List<PipelineStep>,
    actionInProgress: Boolean,
    onRerun: () -> Unit,
    onStopClick: () -> Unit,
) {
    val spacing = JengaTheme.spacing
    val colors = JengaTheme.colors
    val visual = pipeline.status.visual()
    val container: Color = when (visual) {
        PipelineVisualStatus.Success -> colors.successContainer
        PipelineVisualStatus.Failed -> colors.errorContainer
        PipelineVisualStatus.Running -> colors.infoContainer
        PipelineVisualStatus.Paused -> colors.warningContainer
        PipelineVisualStatus.Neutral -> colors.surfaceVariant
    }
    val onContainer: Color = when (visual) {
        PipelineVisualStatus.Success -> colors.onSuccessContainer
        PipelineVisualStatus.Failed -> colors.onErrorContainer
        PipelineVisualStatus.Running -> colors.onInfoContainer
        PipelineVisualStatus.Paused -> colors.onWarningContainer
        PipelineVisualStatus.Neutral -> colors.textPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PipelineStatusIcon(pipeline.status, size = 22.dp)
        Column(modifier = Modifier.weight(1f).padding(start = spacing.md)) {
            JengaText(
                text = heroHeadline(pipeline, steps),
                style = JengaTheme.typography.titleMedium,
                color = onContainer,
                maxLines = 2,
            )
            JengaText(
                text = heroSubline(pipeline),
                style = JengaTheme.typography.bodySmall,
                color = onContainer.copy(alpha = 0.8f),
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (pipeline.status.isRunning) {
            JengaButton(
                text = "Stop",
                onClick = onStopClick,
                variant = JengaButtonVariant.Outline,
                enabled = !actionInProgress,
                loading = actionInProgress,
                leadingIcon = { JengaIcon(JengaIcons.Ban, contentDescription = null, size = 16.dp) },
            )
        } else {
            JengaButton(
                text = "Re-run",
                onClick = onRerun,
                variant = JengaButtonVariant.Outline,
                enabled = !actionInProgress && pipeline.rerunRequest() != null,
                loading = actionInProgress,
                leadingIcon = { JengaIcon(JengaIcons.Refresh, contentDescription = null, size = 16.dp) },
            )
        }
    }
}

private fun heroHeadline(pipeline: Pipeline, steps: List<PipelineStep>): String = when (pipeline.status.visual()) {
    PipelineVisualStatus.Failed -> {
        val failed = steps.firstOrNull { it.status.visual() == PipelineVisualStatus.Failed }?.name
        if (failed != null) "Failed in $failed" else "Failed"
    }
    PipelineVisualStatus.Running -> {
        val running = steps.firstOrNull { it.status.visual() == PipelineVisualStatus.Running }?.name
        if (running != null) "Running · $running" else "Running"
    }
    PipelineVisualStatus.Success -> "Success"
    PipelineVisualStatus.Paused -> "Paused"
    PipelineVisualStatus.Neutral -> pipeline.status.label()
}

private fun heroSubline(pipeline: Pipeline): String {
    val parts = buildList {
        if (pipeline.durationSeconds > 0) add(formatDuration(pipeline.durationSeconds))
        pipeline.createdOn.takeIf { it.isNotBlank() }?.let { add(relativeTime(it)) }
    }
    return parts.joinToString("  ·  ").ifBlank { triggerLabel(pipeline) }
}

@Composable
private fun ActionErrorCard(message: String, onDismiss: () -> Unit) {
    val spacing = JengaTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(JengaTheme.colors.errorContainer)
            .padding(start = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JengaText(
            text = message,
            style = JengaTheme.typography.bodySmall,
            color = JengaTheme.colors.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        JengaIconButton(onClick = onDismiss) {
            JengaIcon(
                JengaIcons.Close,
                contentDescription = "Dismiss",
                tint = JengaTheme.colors.onErrorContainer,
                size = 16.dp,
            )
        }
    }
}

@Composable
private fun PipelineMetadataCard(
    pipeline: Pipeline,
    onOpenCommit: (String) -> Unit,
    onOpenPullRequest: (Long) -> Unit,
) {
    val spacing = JengaTheme.spacing
    JengaCard(variant = JengaCardVariant.Outlined, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(spacing.md)) {
            val commit = buildString {
                pipeline.commitMessage?.substringBefore('\n')?.takeIf { it.isNotBlank() }?.let { append(it) }
                pipeline.commitHash?.take(7)?.let {
                    if (isNotEmpty()) append("  ·  ")
                    append(it)
                }
            }
            if (commit.isNotBlank()) {
                val hash = pipeline.commitHash?.takeIf { it.isNotBlank() }
                PipelineMetaRow("Commit", commit, onClick = hash?.let { { onOpenCommit(it) } })
                JengaDivider()
            }
            pipeline.refName?.takeIf { it.isNotBlank() }?.let {
                PipelineMetaRow(if (pipeline.refType?.name == "TAG") "Tag" else "Branch", it)
                JengaDivider()
            }
            val prId = pipeline.pullRequestId
            PipelineMetaRow("Trigger", triggerLabel(pipeline), onClick = prId?.let { { onOpenPullRequest(it) } })
            if (pipeline.creatorName.isNotBlank()) {
                JengaDivider()
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    JengaText(
                        text = "Author",
                        style = JengaTheme.typography.caption,
                        color = JengaTheme.colors.textMuted,
                        modifier = Modifier.weight(0.4f),
                    )
                    Row(modifier = Modifier.weight(0.6f), verticalAlignment = Alignment.CenterVertically) {
                        JengaAvatar(name = pipeline.creatorName, size = JengaAvatarSize.Small)
                        JengaText(
                            text = pipeline.creatorName,
                            style = JengaTheme.typography.bodySmall,
                            color = JengaTheme.colors.textPrimary,
                            modifier = Modifier.padding(start = spacing.sm),
                        )
                    }
                }
            }
            if (pipeline.createdOn.isNotBlank()) {
                JengaDivider()
                PipelineMetaRow("Started", shortDate(pipeline.createdOn))
            }
            if (pipeline.durationSeconds > 0) {
                JengaDivider()
                PipelineMetaRow("Duration", formatDuration(pipeline.durationSeconds))
            }
        }
    }
}

private const val STEPS_HEADER_OFFSET = 3
