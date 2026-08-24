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
package com.joelkanyi.platypus.feature.repositories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.domain.model.RepoTab
import com.joelkanyi.platypus.domain.model.Repository
import com.joelkanyi.platypus.domain.model.WatchedRepo
import com.joelkanyi.platypus.domain.repository.AuthRepository
import com.joelkanyi.platypus.domain.repository.WatchlistRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RepositoriesViewModel(
    private val authRepository: AuthRepository,
    private val watchlistRepository: WatchlistRepository,
    initialTab: RepoTab,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepositoriesUiState(tab = initialTab))
    val uiState: StateFlow<RepositoriesUiState> = _uiState.asStateFlow()

    private var allWatched: List<WatchedRepo> = emptyList()
    private var searchJob: Job? = null

    init {
        observeWatched()
        loadWorkspaces()
    }

    fun onEvent(event: RepositoriesUiEvent) {
        when (event) {
            is RepositoriesUiEvent.SelectTab -> _uiState.update { it.copy(tab = event.tab) }
            is RepositoriesUiEvent.SelectWorkspace -> select(event.option)
            is RepositoriesUiEvent.QueryChanged -> onQueryChanged(event.query)
            is RepositoriesUiEvent.ToggleWatch -> toggleWatch(event.repo, event.watch)
            is RepositoriesUiEvent.Unwatch -> unwatch(event.repo)
            RepositoriesUiEvent.LoadMore -> loadMore()
            RepositoriesUiEvent.RetryWorkspaces -> loadWorkspaces()
            RepositoriesUiEvent.RetryRepos -> _uiState.value.selected?.let { browse(it, reset = true) }
        }
    }

    private fun observeWatched() {
        viewModelScope.launch {
            watchlistRepository.watchedAll().collect { watched ->
                allWatched = watched
                _uiState.update { state ->
                    state.copy(
                        watched = watched.toImmutableList(),
                        watchedCount = watched.size,
                        repos = state.repos.map { it.copy(watched = isWatched(it.repo)) }.toImmutableList(),
                    )
                }
            }
        }
    }

    private fun isWatched(repo: Repository): Boolean {
        val accountId = _uiState.value.selected?.accountId ?: return false
        return allWatched.any {
            it.accountId == accountId && it.workspaceSlug == repo.workspaceSlug && it.repoSlug == repo.slug
        }
    }

    private fun loadWorkspaces() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWorkspaces = true, workspacesError = null) }
            val accounts = authRepository.accounts.value
            val options = mutableListOf<WorkspaceOption>()
            var lastError: String? = null
            for (account in accounts) {
                when (val result = authRepository.workspaces(account.id)) {
                    is NetworkResult.Success ->
                        options += result.data.map { WorkspaceOption(account.id, account.user.displayName, it) }
                    is NetworkResult.Failure -> lastError = result.userMessage()
                }
            }
            if (options.isEmpty()) {
                _uiState.update { it.copy(isLoadingWorkspaces = false, workspacesError = lastError) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isLoadingWorkspaces = false,
                    workspaces = options.toImmutableList(),
                    multiAccount =
                    accounts.size > 1,
                )
            }
            select(options.first())
        }
    }

    private fun select(option: WorkspaceOption) {
        if (_uiState.value.selected?.id == option.id) return
        _uiState.update {
            it.copy(selected = option, query = "", repos = persistentListOf(), nextCursor = null, reposError = null)
        }
        browse(option, reset = true)
    }

    private fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        val option = _uiState.value.selected ?: return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            browse(option, reset = true)
        }
    }

    private fun loadMore() {
        val state = _uiState.value
        val option = state.selected ?: return
        if (!state.canLoadMore) return
        browse(option, reset = false)
    }

    private fun browse(option: WorkspaceOption, reset: Boolean) {
        viewModelScope.launch {
            val query = _uiState.value.query.takeIf { it.isNotBlank() }
            val cursor = if (reset) null else _uiState.value.nextCursor
            _uiState.update {
                if (reset) it.copy(isLoadingRepos = true, reposError = null) else it.copy(isPaginating = true)
            }
            when (val result = watchlistRepository.browse(option.accountId, option.workspace.slug, query, cursor)) {
                is NetworkResult.Success -> {
                    val rows = result.data.repositories.map { RepoRow(it, isWatched(it)) }
                    _uiState.update { state ->
                        state.copy(
                            isLoadingRepos = false,
                            isPaginating = false,
                            repos = (if (reset) rows else state.repos + rows).toImmutableList(),
                            nextCursor = result.data.next,
                        )
                    }
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isLoadingRepos = false, isPaginating = false, reposError = result.userMessage())
                }
            }
        }
    }

    private fun toggleWatch(repo: Repository, watch: Boolean) {
        val option = _uiState.value.selected ?: return
        viewModelScope.launch {
            if (watch) {
                watchlistRepository.watch(option.accountId, repo)
            } else {
                watchlistRepository.unwatch(option.accountId, repo.workspaceSlug, repo.slug)
            }
        }
    }

    private fun unwatch(repo: WatchedRepo) {
        viewModelScope.launch {
            watchlistRepository.unwatch(repo.accountId, repo.workspaceSlug, repo.repoSlug)
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
    }
}
