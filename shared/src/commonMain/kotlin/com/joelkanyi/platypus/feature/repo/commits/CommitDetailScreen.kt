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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.joelkanyi.platypus.designsystem.PlatypusDiffView
import com.joelkanyi.platypus.domain.model.CommitDetail
import com.joelkanyi.platypus.domain.repository.RepoContentRepository
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.card.JengaCard
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class CommitDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val detail: CommitDetail? = null,
)

class CommitDetailViewModel(
    private val repoContentRepository: RepoContentRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
    private val hash: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommitDetailUiState())
    val uiState: StateFlow<CommitDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repoContentRepository.commitDetail(accountId, workspace, repoSlug, hash)) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, detail = result.data) }
                is NetworkResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.userMessage()) }
            }
        }
    }
}

@Composable
fun CommitDetailScreen(
    accountId: String,
    workspace: String,
    repoSlug: String,
    hash: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "$accountId/$workspace/$repoSlug/$hash") {
        CommitDetailViewModel(dependencies.repoContentRepository, accountId, workspace, repoSlug, hash)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    CommitDetailContent(
        shortHash = hash.take(7),
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
internal fun CommitDetailContent(
    shortHash: String,
    state: CommitDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    val detail = state.detail

    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = "Commit",
                subtitle = shortHash,
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
                title = "Couldn't load commit",
                description = state.error,
                actionLabel = "Try again",
                onAction = onRetry,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            detail != null -> Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                JengaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.lg, vertical = spacing.sm),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        JengaText(text = detail.commit.message, color = JengaTheme.colors.textPrimary)
                        JengaText(
                            text = "${detail.commit.authorName} · ${detail.commit.date.substringBefore('T')}",
                            style = JengaTheme.typography.caption,
                            color = JengaTheme.colors.textMuted,
                        )
                    }
                }
                PlatypusDiffView(
                    lines = detail.diffLines,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }

            else -> Unit
        }
    }
}
