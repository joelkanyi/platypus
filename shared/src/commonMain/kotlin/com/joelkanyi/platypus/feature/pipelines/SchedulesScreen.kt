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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.designsystem.PlatypusListRowSkeleton
import com.joelkanyi.platypus.domain.model.Schedule
import com.joelkanyi.platypus.domain.repository.PipelineRepository
import io.github.joelkanyi.jenga.component.badge.JengaBadgeTone
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.refresh.JengaPullToRefresh
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.state.JengaEmptyState
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.component.status.JengaStatusPill
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class SchedulesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val schedules: List<Schedule> = emptyList(),
)

class SchedulesViewModel(
    private val repository: PipelineRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchedulesUiState())
    val uiState: StateFlow<SchedulesUiState> = _uiState.asStateFlow()

    init {
        load(initial = true)
    }

    fun retry() = load(initial = true)

    fun refresh() = load(initial = false)

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = initial, isRefreshing = !initial, error = null) }
            when (val result = repository.schedules(accountId, workspace, repoSlug)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, schedules = result.data)
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = result.userMessage())
                }
            }
        }
    }
}

@Composable
fun SchedulesScreen(
    accountId: String,
    workspace: String,
    repoSlug: String,
    repoName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "schedules/$accountId/$workspace/$repoSlug") {
        SchedulesViewModel(dependencies.pipelineRepository, accountId, workspace, repoSlug)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SchedulesContent(
        repoName = repoName,
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
internal fun SchedulesContent(
    repoName: String,
    state: SchedulesUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = "Schedules",
                subtitle = repoName.takeIf { it.isNotBlank() },
                navigationIcon = {
                    JengaIconButton(onClick = onBack) {
                        JengaIcon(JengaIcons.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.error != null -> JengaErrorState(
                title = "Couldn't load schedules",
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.sm),
                ) {
                    when {
                        state.isLoading -> items(5) { PlatypusListRowSkeleton() }

                        state.schedules.isEmpty() -> item {
                            JengaEmptyState(
                                title = "No schedules",
                                description = "Scheduled pipeline runs will appear here.",
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }

                        else -> items(state.schedules, key = { it.uuid }) { schedule ->
                            ScheduleRow(schedule)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(schedule: Schedule) {
    val supporting = buildList {
        schedule.selectorPattern?.let { add(it) }
        add(schedule.cronPattern.ifBlank { "cron" })
    }.joinToString("  ·  ")
    JengaListItem(
        headline = schedule.refName ?: "Schedule",
        supporting = supporting,
        trailingContent = {
            JengaStatusPill(
                label = if (schedule.enabled) "Enabled" else "Disabled",
                tone = if (schedule.enabled) JengaBadgeTone.Success else JengaBadgeTone.Neutral,
            )
        },
    )
}
