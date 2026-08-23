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
package com.joelkanyi.platypus.feature.pr.files

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.designsystem.expand
import com.joelkanyi.platypus.domain.model.DiffFileStatus
import com.joelkanyi.platypus.domain.model.PrDiff
import com.joelkanyi.platypus.domain.model.PrDiffFile
import com.joelkanyi.platypus.domain.repository.PullRequestRepository
import io.github.joelkanyi.jenga.component.badge.JengaBadge
import io.github.joelkanyi.jenga.component.badge.JengaBadgeTone
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.progress.jengaShimmer
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
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
data class FilesChangedUiState(val isLoading: Boolean = true, val error: String? = null, val diff: PrDiff? = null)

class FilesChangedViewModel(
    private val repository: PullRequestRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
    private val prId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FilesChangedUiState())
    val uiState: StateFlow<FilesChangedUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.diff(accountId, workspace, repoSlug, prId)) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, diff = result.data) }
                is NetworkResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.userMessage()) }
            }
        }
    }
}

@Composable
fun FilesChangedScreen(
    accountId: String,
    workspace: String,
    repoSlug: String,
    prId: Long,
    onOpenFile: (path: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "files/$accountId/$workspace/$repoSlug/$prId") {
        FilesChangedViewModel(dependencies.pullRequestRepository, accountId, workspace, repoSlug, prId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FilesChangedContent(
        state = state,
        onOpenFile = onOpenFile,
        onBack = onBack,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
internal fun FilesChangedContent(
    state: FilesChangedUiState,
    onOpenFile: (path: String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    val diff = state.diff

    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = "Files changed",
                subtitle = diff?.let { summaryLine(it) },
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
                title = "Couldn't load changes",
                description = state.error,
                actionLabel = "Try again",
                onAction = onRetry,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            diff != null && diff.files.isEmpty() -> JengaEmptyState(
                title = "No changes",
                description = "This pull request has no file changes.",
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            diff != null -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding.expand(horizontal = spacing.lg, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                items(diff.files, key = { it.id }) { file ->
                    FileRow(file = file, onClick = { onOpenFile(file.path) })
                }
            }

            else -> FileListSkeleton(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding.expand(horizontal = spacing.lg, vertical = spacing.sm),
            )
        }
    }
}

@Composable
private fun FileListSkeleton(modifier: Modifier = Modifier, contentPadding: PaddingValues) {
    val spacing = JengaTheme.spacing
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        items(10) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Box(Modifier.size(20.dp).clip(JengaTheme.shapes.control).jengaShimmer())
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                    Box(Modifier.height(14.dp).fillMaxWidth(0.55f).clip(JengaTheme.shapes.control).jengaShimmer())
                    Box(Modifier.height(11.dp).fillMaxWidth(0.35f).clip(JengaTheme.shapes.control).jengaShimmer())
                }
                Box(Modifier.height(12.dp).width(36.dp).clip(JengaTheme.shapes.control).jengaShimmer())
            }
        }
    }
}

@Composable
private fun FileRow(file: PrDiffFile, onClick: () -> Unit) {
    val spacing = JengaTheme.spacing
    JengaListItem(
        headline = file.path.substringAfterLast('/'),
        supporting = file.path.substringBeforeLast('/', "").ifBlank { null },
        leadingContent = { JengaIcon(JengaIcons.Sliders, contentDescription = null) },
        trailingContent = {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                if (file.added > 0) {
                    JengaText(
                        text = "+${file.added}",
                        style = JengaTheme.typography.caption,
                        color = JengaTheme.colors.success,
                    )
                }
                if (file.removed > 0) {
                    JengaText(
                        text = "−${file.removed}",
                        style = JengaTheme.typography.caption,
                        color = JengaTheme.colors.error,
                    )
                }
                statusBadge(file.status)?.let { (label, tone) -> JengaBadge(text = label, tone = tone) }
            }
        },
        onClick = onClick,
    )
}

private fun summaryLine(diff: PrDiff): String {
    val files = diff.files.size
    val fileWord = if (files == 1) "file" else "files"
    return "$files $fileWord · +${diff.totalAdded} −${diff.totalRemoved}"
}

private fun statusBadge(status: DiffFileStatus): Pair<String, JengaBadgeTone>? = when (status) {
    DiffFileStatus.ADDED -> "New" to JengaBadgeTone.Success
    DiffFileStatus.REMOVED -> "Removed" to JengaBadgeTone.Error
    DiffFileStatus.RENAMED -> "Renamed" to JengaBadgeTone.Info
    DiffFileStatus.MODIFIED -> null
}
