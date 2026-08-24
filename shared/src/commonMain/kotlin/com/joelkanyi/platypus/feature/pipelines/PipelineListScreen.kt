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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.designsystem.PlatypusListRowSkeleton
import com.joelkanyi.platypus.domain.model.AccountId
import com.joelkanyi.platypus.domain.model.Pipeline
import com.joelkanyi.platypus.domain.model.PipelineTriggerRequest
import com.joelkanyi.platypus.domain.model.RepoRef
import com.joelkanyi.platypus.domain.model.RepoSlug
import com.joelkanyi.platypus.domain.model.WorkspaceSlug
import com.joelkanyi.platypus.domain.repository.PipelineRepository
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.chip.JengaChip
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.menu.JengaDropdownMenu
import io.github.joelkanyi.jenga.component.menu.JengaDropdownMenuItem
import io.github.joelkanyi.jenga.component.refresh.JengaPullToRefresh
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.state.JengaEmptyState
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.component.tabs.JengaSegmentedControl
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PipelineListFilter(val label: String) {
    ALL("All"),
    RUNNING("Running"),
    FAILED("Failed"),
    SUCCESS("Success"),
    ;

    fun matches(pipeline: Pipeline): Boolean = when (this) {
        ALL -> true
        RUNNING -> pipeline.status.visual() == PipelineVisualStatus.Running
        FAILED -> pipeline.status.visual() == PipelineVisualStatus.Failed
        SUCCESS -> pipeline.status.visual() == PipelineVisualStatus.Success
    }
}

@Immutable
data class PipelineListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val pipelines: ImmutableList<Pipeline> = persistentListOf(),
    val filter: PipelineListFilter = PipelineListFilter.ALL,
    val branchFilter: String? = null,
    val triggerFilter: String? = null,
    val isTriggering: Boolean = false,
    val triggerError: String? = null,
) {
    val visible: ImmutableList<Pipeline> get() = pipelines.filter {
        filter.matches(it) &&
            (branchFilter == null || it.refName == branchFilter) &&
            (triggerFilter == null || triggerLabel(it) == triggerFilter)
    }.toImmutableList()

    val branches: ImmutableList<String> get() =
        pipelines.mapNotNull { it.refName?.takeIf { r -> r.isNotBlank() } }.distinct().toImmutableList()

    val triggers: ImmutableList<String> get() = pipelines.map { triggerLabel(it) }.distinct().toImmutableList()
}

class PipelineListViewModel(
    private val repository: PipelineRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
) : ViewModel() {

    private val repoRef = RepoRef(AccountId(accountId), WorkspaceSlug(workspace), RepoSlug(repoSlug))

    private val _uiState = MutableStateFlow(PipelineListUiState())
    val uiState: StateFlow<PipelineListUiState> = _uiState.asStateFlow()

    init {
        load(initial = true)
        poll()
    }

    fun retry() = load(initial = true)

    fun refresh() = load(initial = false)

    fun setFilter(filter: PipelineListFilter) = _uiState.update { it.copy(filter = filter) }

    fun setBranchFilter(branch: String?) = _uiState.update { it.copy(branchFilter = branch) }

    fun setTriggerFilter(trigger: String?) = _uiState.update { it.copy(triggerFilter = trigger) }

    fun clearTriggerError() = _uiState.update { it.copy(triggerError = null) }

    fun trigger(request: PipelineTriggerRequest, onTriggered: (Pipeline) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTriggering = true, triggerError = null) }
            when (val result = repository.trigger(repoRef, request)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isTriggering = false) }
                    refresh()
                    onTriggered(result.data)
                }
                is NetworkResult.Failure ->
                    _uiState.update { it.copy(isTriggering = false, triggerError = result.userMessage()) }
            }
        }
    }

    private fun poll() {
        viewModelScope.launch {
            while (true) {
                delay(POLL_MS)
                val state = _uiState.value
                if (!state.isLoading && !state.isRefreshing && state.pipelines.any { it.status.isRunning }) {
                    silentRefresh()
                }
            }
        }
    }

    private suspend fun silentRefresh() {
        val result = repository.pipelines(repoRef)
        if (result is NetworkResult.Success) {
            _uiState.update { current ->
                if (current.pipelines ==
                    result.data
                ) {
                    current
                } else {
                    current.copy(pipelines = result.data.toImmutableList())
                }
            }
        }
    }

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = initial, isRefreshing = !initial, error = null) }
            when (val result = repository.pipelines(repoRef)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, pipelines = result.data.toImmutableList())
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = result.userMessage())
                }
            }
        }
    }

    private companion object {
        const val POLL_MS = 15_000L
    }
}

@Composable
fun PipelineListScreen(
    accountId: String,
    workspace: String,
    repoSlug: String,
    repoName: String,
    onOpenPipeline: (Pipeline) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "pipelines/$accountId/$workspace/$repoSlug") {
        PipelineListViewModel(dependencies.pipelineRepository, accountId, workspace, repoSlug)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showRunSheet by rememberSaveable { mutableStateOf(false) }

    PipelineListContent(
        repoName = repoName,
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onSelectFilter = viewModel::setFilter,
        onSelectBranch = viewModel::setBranchFilter,
        onSelectTrigger = viewModel::setTriggerFilter,
        onOpenPipeline = onOpenPipeline,
        onRunClick = { showRunSheet = true },
        modifier = modifier,
    )

    if (showRunSheet) {
        RunPipelineSheet(
            accountId = accountId,
            workspace = workspace,
            repoSlug = repoSlug,
            isTriggering = state.isTriggering,
            error = state.triggerError,
            onRun = { request ->
                viewModel.trigger(request) { pipeline ->
                    showRunSheet = false
                    onOpenPipeline(pipeline)
                }
            },
            onDismiss = {
                showRunSheet = false
                viewModel.clearTriggerError()
            },
        )
    }
}

@Composable
internal fun PipelineListContent(
    repoName: String,
    state: PipelineListUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onSelectFilter: (PipelineListFilter) -> Unit,
    onSelectBranch: (String?) -> Unit = {},
    onSelectTrigger: (String?) -> Unit = {},
    onOpenPipeline: (Pipeline) -> Unit,
    onRunClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = "Pipelines",
                subtitle = repoName.takeIf { it.isNotBlank() },
                navigationIcon = {
                    JengaIconButton(onClick = onBack) {
                        JengaIcon(JengaIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    JengaIconButton(onClick = onRunClick) {
                        JengaIcon(JengaIcons.Flash, contentDescription = "Run pipeline")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.error != null -> JengaErrorState(
                title = "Couldn't load pipelines",
                description = state.error,
                actionLabel = "Try again",
                onAction = onRetry,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            else -> JengaPullToRefresh(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (!state.isLoading && state.pipelines.isNotEmpty()) {
                        item {
                            JengaSegmentedControl(
                                selectedIndex = PipelineListFilter.entries.indexOf(state.filter),
                                segments = PipelineListFilter.entries.map { it.label },
                                onSelect = { onSelectFilter(PipelineListFilter.entries[it]) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = spacing.lg, vertical = spacing.sm),
                            )
                        }
                        if (state.branches.size > 1 || state.triggers.size > 1) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = spacing.lg)
                                        .padding(bottom = spacing.sm),
                                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                                ) {
                                    if (state.branches.size > 1) {
                                        FilterDropdownChip(
                                            label = "Branch",
                                            selected = state.branchFilter,
                                            options = state.branches,
                                            onSelect = onSelectBranch,
                                        )
                                    }
                                    if (state.triggers.size > 1) {
                                        FilterDropdownChip(
                                            label = "Trigger",
                                            selected = state.triggerFilter,
                                            options = state.triggers,
                                            onSelect = onSelectTrigger,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    when {
                        state.isLoading -> items(8) { PlatypusListRowSkeleton() }

                        state.pipelines.isEmpty() -> item {
                            JengaEmptyState(
                                title = "No pipeline runs",
                                description = "Runs appear here when pipelines execute for this repository.",
                                actionLabel = "Run pipeline",
                                onAction = onRunClick,
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }

                        state.visible.isEmpty() -> item {
                            JengaEmptyState(
                                title = "No ${state.filter.label.lowercase()} runs",
                                description = "Nothing matches this filter.",
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }

                        else -> items(state.visible, key = { it.uuid }) { pipeline ->
                            PipelineRow(pipeline = pipeline, onClick = { onOpenPipeline(pipeline) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterDropdownChip(label: String, selected: String?, options: List<String>, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        JengaChip(
            label = selected ?: label,
            selected = selected != null,
            onClick = { expanded = true },
            leadingIcon = {
                JengaIcon(JengaIcons.ChevronDown, contentDescription = null, size = 16.dp)
            },
        )
        JengaDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            JengaDropdownMenuItem(
                text = "All ${label.lowercase()}es",
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            options.forEach { option ->
                JengaDropdownMenuItem(
                    text = option,
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
