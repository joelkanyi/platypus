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
import com.joelkanyi.platypus.designsystem.relativeTime
import com.joelkanyi.platypus.domain.model.AccountId
import com.joelkanyi.platypus.domain.model.Deployment
import com.joelkanyi.platypus.domain.model.DeploymentStatus
import com.joelkanyi.platypus.domain.model.RepoRef
import com.joelkanyi.platypus.domain.model.RepoSlug
import com.joelkanyi.platypus.domain.model.WorkspaceSlug
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class DeploymentsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val deployments: ImmutableList<Deployment> = persistentListOf(),
)

class DeploymentsViewModel(
    private val repository: PipelineRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
) : ViewModel() {

    private val repoRef = RepoRef(AccountId(accountId), WorkspaceSlug(workspace), RepoSlug(repoSlug))

    private val _uiState = MutableStateFlow(DeploymentsUiState())
    val uiState: StateFlow<DeploymentsUiState> = _uiState.asStateFlow()

    init {
        load(initial = true)
    }

    fun retry() = load(initial = true)

    fun refresh() = load(initial = false)

    private fun load(initial: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = initial, isRefreshing = !initial, error = null) }
            when (val result = repository.deployments(repoRef)) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, deployments = result.data.toImmutableList())
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = result.userMessage())
                }
            }
        }
    }
}

private fun DeploymentStatus.tone(): JengaBadgeTone = when (this) {
    DeploymentStatus.IN_PROGRESS -> JengaBadgeTone.Info
    DeploymentStatus.SUCCESSFUL -> JengaBadgeTone.Success
    DeploymentStatus.FAILED -> JengaBadgeTone.Error
    DeploymentStatus.UNDEPLOYED, DeploymentStatus.UNKNOWN -> JengaBadgeTone.Neutral
}

@Composable
fun DeploymentsScreen(
    accountId: String,
    workspace: String,
    repoSlug: String,
    repoName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "deployments/$accountId/$workspace/$repoSlug") {
        DeploymentsViewModel(dependencies.pipelineRepository, accountId, workspace, repoSlug)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DeploymentsContent(
        repoName = repoName,
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
internal fun DeploymentsContent(
    repoName: String,
    state: DeploymentsUiState,
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
                title = "Deployments",
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
                title = "Couldn't load deployments",
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
                        state.isLoading -> items(6) { PlatypusListRowSkeleton() }

                        state.deployments.isEmpty() -> item {
                            JengaEmptyState(
                                title = "No deployments",
                                description = "Deployments to environments will appear here.",
                                modifier = Modifier.fillParentMaxSize(),
                            )
                        }

                        else -> items(state.deployments, key = { it.uuid }) { deployment ->
                            DeploymentRow(deployment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeploymentRow(deployment: Deployment) {
    val supporting = buildList {
        add(deployment.statusLabel)
        deployment.commitHash?.take(7)?.let { add(it) }
        deployment.deployerName?.let { add(it) }
        deployment.updatedOn?.let { add(relativeTime(it)) }
    }.joinToString("  ·  ")
    JengaListItem(
        headline = deployment.environmentName,
        supporting = supporting,
        leadingContent = { JengaStatusPill(label = deployment.statusLabel, tone = deployment.status.tone()) },
    )
}
