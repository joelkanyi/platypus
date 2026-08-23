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
package com.joelkanyi.platypus.feature.repo.commits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.joelkanyi.platypus.designsystem.PlatypusCommitRow
import com.joelkanyi.platypus.designsystem.expand
import com.joelkanyi.platypus.domain.model.Commit
import com.joelkanyi.platypus.domain.repository.RepoContentRepository
import io.github.joelkanyi.jenga.component.button.JengaButton
import io.github.joelkanyi.jenga.component.button.JengaButtonVariant
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class CommitsUiState(
    val isLoading: Boolean = true,
    val isPaginating: Boolean = false,
    val error: String? = null,
    val commits: List<Commit> = emptyList(),
    val nextCursor: String? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isPaginating && !isLoading
}

class CommitsViewModel(
    private val repoContentRepository: RepoContentRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
    private val ref: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommitsUiState())
    val uiState: StateFlow<CommitsUiState> = _uiState.asStateFlow()

    init {
        load(reset = true)
    }

    fun retry() = load(reset = true)

    fun loadMore() {
        if (_uiState.value.canLoadMore) load(reset = false)
    }

    private fun load(reset: Boolean) {
        viewModelScope.launch {
            _uiState.update { if (reset) it.copy(isLoading = true, error = null) else it.copy(isPaginating = true) }
            val cursor = if (reset) null else _uiState.value.nextCursor
            when (val result = repoContentRepository.commits(accountId, workspace, repoSlug, ref, cursor)) {
                is NetworkResult.Success -> _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isPaginating = false,
                        commits = if (reset) result.data.commits else state.commits + result.data.commits,
                        nextCursor = result.data.next,
                    )
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, isPaginating = false, error = result.userMessage())
                }
            }
        }
    }
}

@Composable
fun CommitsScreen(
    accountId: String,
    workspace: String,
    repoSlug: String,
    ref: String,
    onOpenCommit: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "$accountId/$workspace/$repoSlug/$ref/commits") {
        CommitsViewModel(dependencies.repoContentRepository, accountId, workspace, repoSlug, ref)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CommitsContent(
        ref = ref,
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        onOpenCommit = onOpenCommit,
        modifier = modifier,
    )
}

@Composable
internal fun CommitsContent(
    ref: String,
    state: CommitsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing

    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = "Commits",
                subtitle = ref,
                navigationIcon = {
                    JengaIconButton(onClick = onBack) {
                        JengaIcon(JengaIcons.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.error != null && state.commits.isEmpty() -> JengaErrorState(
                title = "Couldn't load commits",
                description = state.error,
                actionLabel = "Try again",
                onAction = onRetry,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding.expand(horizontal = spacing.lg, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                items(state.commits, key = { it.hash }) { commit ->
                    PlatypusCommitRow(commit = commit, onClick = { onOpenCommit(commit.hash) })
                }
                if (state.nextCursor != null) {
                    item {
                        JengaButton(
                            text = if (state.isPaginating) "Loading..." else "Load more",
                            onClick = onLoadMore,
                            variant = JengaButtonVariant.Outline,
                            modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
                        )
                    }
                }
            }
        }
    }
}
