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
package com.joelkanyi.platypus.feature.pr.list

import androidx.compose.foundation.layout.Arrangement
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
import com.joelkanyi.platypus.designsystem.PlatypusPullRequestRow
import com.joelkanyi.platypus.domain.model.PullRequest
import com.joelkanyi.platypus.domain.repository.PullRequestRepository
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.refresh.JengaPullToRefresh
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.state.JengaEmptyState
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class RepoPullRequestsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val pullRequests: List<PullRequest> = emptyList(),
)

class RepoPullRequestsViewModel(
    private val repository: PullRequestRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
    private val repoName: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoPullRequestsUiState())
    val uiState: StateFlow<RepoPullRequestsUiState> = _uiState.asStateFlow()

    init {
        load(initial = true)
    }

    fun retry() = load(initial = true)

    fun refresh() = load(initial = false)

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = initial, isRefreshing = !initial, error = null) }
            when (val result = repository.pullRequests(accountId, workspace, repoSlug, repoName)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, pullRequests = result.data)
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = result.userMessage())
                }
            }
        }
    }
}

@Composable
fun RepoPullRequestsScreen(
    accountId: String,
    workspace: String,
    repoSlug: String,
    repoName: String,
    onOpenPullRequest: (PullRequest) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "repoprs/$accountId/$workspace/$repoSlug") {
        RepoPullRequestsViewModel(dependencies.pullRequestRepository, accountId, workspace, repoSlug, repoName)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    RepoPullRequestsContent(
        repoName = repoName,
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onOpenPullRequest = onOpenPullRequest,
        modifier = modifier,
    )
}

@Composable
internal fun RepoPullRequestsContent(
    repoName: String,
    state: RepoPullRequestsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onOpenPullRequest: (PullRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = "Pull requests",
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
                title = "Couldn't load pull requests",
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
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    when {
                        state.isLoading -> items(8) { PlatypusListRowSkeleton() }

                        state.pullRequests.isEmpty() -> item {
                            JengaEmptyState(
                                title = "No open pull requests",
                                description = "This repository has no open pull requests.",
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }

                        else -> items(state.pullRequests, key = { it.key }) { pr ->
                            PlatypusPullRequestRow(
                                pullRequest = pr,
                                onClick = { onOpenPullRequest(pr) },
                                showRelationship = true,
                            )
                        }
                    }
                }
            }
        }
    }
}
