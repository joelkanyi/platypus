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
package com.joelkanyi.platypus.feature.repo.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.core.search.fuzzyFilter
import com.joelkanyi.platypus.designsystem.PlatypusBreadcrumb
import com.joelkanyi.platypus.designsystem.PlatypusIcons
import com.joelkanyi.platypus.designsystem.crumbsFor
import com.joelkanyi.platypus.designsystem.formatByteSize
import com.joelkanyi.platypus.domain.model.SrcEntry
import com.joelkanyi.platypus.domain.model.SrcEntryType
import com.joelkanyi.platypus.domain.repository.RepoContentRepository
import com.joelkanyi.platypus.feature.repo.branches.BranchesSheet
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.chip.JengaChip
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.search.JengaSearchField
import io.github.joelkanyi.jenga.component.state.JengaEmptyState
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class BrowseUiState(
    val ref: String = "",
    val path: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val entries: List<SrcEntry> = emptyList(),
    val query: String = "",
    val searchLoading: Boolean = false,
    val searchError: String? = null,
    val results: List<String> = emptyList(),
) {
    val searching: Boolean get() = query.isNotBlank()
}

class RepositoryBrowseViewModel(
    private val repoContentRepository: RepoContentRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
    initialRef: String,
    initialPath: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState(ref = initialRef, path = initialPath))
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    private var allPaths: List<String>? = null
    private var searchJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    /** Drill into a folder / breadcrumb / ".." IN PLACE, without pushing a new screen. */
    fun navigateTo(path: String) {
        searchJob?.cancel()
        _uiState.update { it.copy(path = path, query = "", results = emptyList(), searchError = null) }
        load()
    }

    /** Switch branch in place: reset to the new ref's root and drop the cached path list. */
    fun switchBranch(ref: String) {
        allPaths = null
        searchJob?.cancel()
        _uiState.update { it.copy(ref = ref, path = "", query = "", results = emptyList(), searchError = null) }
        load()
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), searchError = null) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            search(query)
        }
    }

    private suspend fun search(query: String) {
        val paths = allPaths ?: run {
            _uiState.update { it.copy(searchLoading = true, searchError = null) }
            when (val result = repoContentRepository.paths(accountId, workspace, repoSlug, _uiState.value.ref)) {
                is NetworkResult.Success -> result.data.also { allPaths = it }
                is NetworkResult.Failure -> {
                    _uiState.update { it.copy(searchLoading = false, searchError = result.userMessage()) }
                    return
                }
            }
        }
        _uiState.update { it.copy(searchLoading = false, results = fuzzyFilter(query, paths)) }
    }

    private fun load() {
        val ref = _uiState.value.ref
        val path = _uiState.value.path
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repoContentRepository.directory(accountId, workspace, repoSlug, ref, path)) {
                is NetworkResult.Success ->
                    _uiState.update { it.copy(isLoading = false, entries = result.data.entries) }
                is NetworkResult.Failure ->
                    _uiState.update { it.copy(isLoading = false, error = result.userMessage()) }
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}

@Composable
fun RepositoryBrowseScreen(
    accountId: String,
    workspace: String,
    repoSlug: String,
    ref: String,
    path: String,
    onOpenFile: (ref: String, path: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "$accountId/$workspace/$repoSlug") {
        RepositoryBrowseViewModel(dependencies.repoContentRepository, accountId, workspace, repoSlug, ref, path)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showBranches by rememberSaveable { mutableStateOf(false) }

    BrowseContent(
        repoLabel = repoSlug,
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onQueryChanged = viewModel::onQueryChanged,
        onNavigateToPath = viewModel::navigateTo,
        onBranchClick = { showBranches = true },
        onOpenFile = { filePath -> onOpenFile(state.ref, filePath) },
        modifier = modifier,
    )

    if (showBranches) {
        BranchesSheet(
            accountId = accountId,
            workspace = workspace,
            repoSlug = repoSlug,
            currentRef = state.ref,
            onSelect = {
                showBranches = false
                viewModel.switchBranch(it)
            },
            onDismiss = { showBranches = false },
        )
    }
}

@Composable
internal fun BrowseContent(
    repoLabel: String,
    state: BrowseUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onNavigateToPath: (String) -> Unit,
    onBranchClick: () -> Unit,
    onOpenFile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing

    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = repoLabel,
                navigationIcon = {
                    JengaIconButton(onClick = onBack) {
                        JengaIcon(JengaIcons.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                JengaChip(
                    label = state.ref,
                    selected = false,
                    onClick = onBranchClick,
                    leadingIcon = { JengaIcon(PlatypusIcons.GitBranch, contentDescription = null) },
                )
                PlatypusBreadcrumb(
                    crumbs = crumbsFor(repoLabel, state.path),
                    onNavigate = onNavigateToPath,
                    modifier = Modifier.weight(1f),
                )
            }
            JengaSearchField(
                value = state.query,
                onValueChange = onQueryChanged,
                placeholder = "Find a file",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg, vertical = spacing.xs),
            )
            if (state.searching) {
                SearchResults(state = state, onOpenFile = onOpenFile)
            } else {
                DirectoryList(
                    state = state,
                    path = state.path,
                    onRetry = onRetry,
                    onNavigateToPath = onNavigateToPath,
                    onOpenFile = onOpenFile,
                )
            }
        }
    }
}

@Composable
private fun DirectoryList(
    state: BrowseUiState,
    path: String,
    onRetry: () -> Unit,
    onNavigateToPath: (String) -> Unit,
    onOpenFile: (String) -> Unit,
) {
    val spacing = JengaTheme.spacing
    when {
        state.error != null -> JengaErrorState(
            title = "Couldn't load files",
            description = state.error,
            actionLabel = "Try again",
            onAction = onRetry,
            modifier = Modifier.fillMaxSize(),
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            if (path.isNotBlank()) {
                item(key = "..") {
                    JengaListItem(
                        headline = "..",
                        leadingContent = {
                            JengaIcon(PlatypusIcons.LevelUp, contentDescription = "Up one folder")
                        },
                        onClick = { onNavigateToPath(path.trim('/').substringBeforeLast('/', "")) },
                    )
                }
            }
            items(state.entries, key = { it.path }) { entry ->
                EntryRow(entry = entry, onNavigateToPath = onNavigateToPath, onOpenFile = onOpenFile)
            }
        }
    }
}

@Composable
private fun SearchResults(state: BrowseUiState, onOpenFile: (String) -> Unit) {
    val spacing = JengaTheme.spacing
    when {
        state.searchError != null -> JengaErrorState(
            title = "Couldn't search files",
            description = state.searchError,
            modifier = Modifier.fillMaxSize(),
        )

        !state.searchLoading && state.results.isEmpty() -> JengaEmptyState(
            title = "No matches",
            description = "No files match \"${state.query}\".",
            modifier = Modifier.fillMaxSize(),
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.xs),
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            items(state.results, key = { it }) { resultPath ->
                JengaListItem(
                    headline = resultPath.substringAfterLast('/'),
                    supporting = resultPath,
                    leadingContent = {
                        JengaIcon(PlatypusIcons.File, contentDescription = null, tint = JengaTheme.colors.textMuted)
                    },
                    onClick = { onOpenFile(resultPath) },
                )
            }
        }
    }
}

@Composable
private fun EntryRow(entry: SrcEntry, onNavigateToPath: (String) -> Unit, onOpenFile: (String) -> Unit) {
    if (entry.type == SrcEntryType.DIRECTORY) {
        JengaListItem(
            headline = entry.name,
            leadingContent = {
                JengaIcon(PlatypusIcons.Folder, contentDescription = null, tint = JengaTheme.colors.brand)
            },
            trailingContent = { JengaIcon(JengaIcons.ChevronRight, contentDescription = null) },
            onClick = { onNavigateToPath(entry.path) },
        )
    } else {
        JengaListItem(
            headline = entry.name,
            supporting = formatByteSize(entry.size),
            leadingContent = {
                JengaIcon(PlatypusIcons.File, contentDescription = null, tint = JengaTheme.colors.textMuted)
            },
            onClick = { onOpenFile(entry.path) },
        )
    }
}
