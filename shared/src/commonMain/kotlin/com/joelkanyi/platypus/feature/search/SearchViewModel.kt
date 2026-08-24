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
package com.joelkanyi.platypus.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.domain.repository.AuthRepository
import com.joelkanyi.platypus.domain.repository.SearchRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val authRepository: AuthRepository,
    private val searchRepository: SearchRepository,
    scopeAccountId: String?,
    scopeWorkspaceSlug: String?,
    scopeRepoSlug: String?,
    scopeRepoName: String?,
) : ViewModel() {

    private val repoScope: RepoScope? =
        if (scopeAccountId != null && scopeWorkspaceSlug != null && scopeRepoSlug != null) {
            RepoScope(scopeAccountId, scopeWorkspaceSlug, scopeRepoSlug, scopeRepoName ?: scopeRepoSlug)
        } else {
            null
        }

    private val _uiState = MutableStateFlow(
        SearchUiState(repoScope = repoScope, isLoadingWorkspaces = repoScope == null),
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        if (repoScope == null) loadWorkspaces()
    }

    fun onEvent(event: SearchUiEvent) {
        when (event) {
            is SearchUiEvent.QueryChanged -> onQueryChanged(event.query)
            SearchUiEvent.ClearQuery -> onQueryChanged("")
            SearchUiEvent.Submit -> runSearch(force = true)
            SearchUiEvent.OpenWorkspacePicker -> _uiState.update { it.copy(showWorkspacePicker = true) }
            SearchUiEvent.DismissWorkspacePicker -> _uiState.update { it.copy(showWorkspacePicker = false) }
            is SearchUiEvent.SelectWorkspace -> onSelectWorkspace(event.workspace)
            SearchUiEvent.Retry -> runSearch(force = true)
            SearchUiEvent.LoadMore -> loadMore()
            SearchUiEvent.RetryWorkspaces -> loadWorkspaces()
        }
    }

    private fun loadWorkspaces() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWorkspaces = true, workspacesError = null) }
            val accounts = authRepository.accounts.value
            val options = mutableListOf<SearchWorkspace>()
            var lastError: String? = null
            for (account in accounts) {
                when (val result = authRepository.workspaces(account.id)) {
                    is NetworkResult.Success -> options += result.data.map {
                        SearchWorkspace(account.id, account.user.displayName, it.slug, it.name, it.avatarUrl)
                    }
                    is NetworkResult.Failure -> lastError = result.userMessage()
                }
            }
            if (options.isEmpty()) {
                _uiState.update { it.copy(isLoadingWorkspaces = false, workspacesError = lastError) }
                return@launch
            }
            _uiState.update {
                it.copy(isLoadingWorkspaces = false, workspaces = options.toImmutableList(), selected = options.first())
            }
        }
    }

    private fun onSelectWorkspace(workspace: SearchWorkspace) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                selected = workspace,
                showWorkspacePicker = false,
                status = SearchStatus.Idle,
                results = persistentListOf(),
                next = null,
            )
        }
        if (_uiState.value.query.trim().length >= MIN_QUERY) runSearch(force = true)
    }

    private fun onQueryChanged(query: String) {
        searchJob?.cancel()
        _uiState.update { it.copy(query = query) }
        if (query.trim().length < MIN_QUERY) {
            _uiState.update { it.copy(status = SearchStatus.Idle, results = persistentListOf(), next = null) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            runSearch(force = false)
        }
    }

    private fun runSearch(force: Boolean) {
        val account = accountId() ?: return
        val workspace = workspaceSlug() ?: return
        val userQuery = _uiState.value.query.trim()
        if (userQuery.length < MIN_QUERY) {
            _uiState.update { it.copy(status = SearchStatus.Idle, results = persistentListOf(), next = null) }
            return
        }
        if (!force && _uiState.value.status == SearchStatus.RateLimited) return
        val scopeKey = scopeKey()
        searchJob?.cancel()
        _uiState.update { it.copy(status = SearchStatus.Loading, results = persistentListOf(), next = null) }
        searchJob = viewModelScope.launch {
            val result = searchRepository.code(account, workspace, effectiveQuery(userQuery))
            if (!isCurrent(userQuery, scopeKey)) return@launch
            when (result) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        status = if (result.data.items.isEmpty()) SearchStatus.NoResults else SearchStatus.Loaded,
                        results = result.data.items.toImmutableList(),
                        totalFiles = result.data.totalFiles,
                        next = result.data.next,
                    )
                }
                is NetworkResult.Failure -> _uiState.update { it.copy(status = failureStatus(result)) }
            }
        }
    }

    private fun failureStatus(failure: NetworkResult.Failure): SearchStatus =
        when ((failure as? NetworkResult.Failure.Http)?.code) {
            429 -> SearchStatus.RateLimited
            402, 403 -> SearchStatus.Gated
            400 -> SearchStatus.BadQuery
            else -> SearchStatus.Error(failure.userMessage())
        }

    private fun loadMore() {
        val account = accountId() ?: return
        val workspace = workspaceSlug() ?: return
        val userQuery = _uiState.value.query.trim()
        val next = _uiState.value.next
        if (next == null || _uiState.value.loadingMore) return
        val scopeKey = scopeKey()
        _uiState.update { it.copy(loadingMore = true, loadMoreError = false) }
        viewModelScope.launch {
            val result = searchRepository.codePage(account, next, workspace)
            if (!isCurrent(userQuery, scopeKey)) return@launch
            when (result) {
                is NetworkResult.Success -> _uiState.update {
                    it.copy(
                        results = (it.results + result.data.items).toImmutableList(),
                        next = result.data.next,
                        loadingMore = false,
                    )
                }
                is NetworkResult.Failure -> _uiState.update { it.copy(loadingMore = false, loadMoreError = true) }
            }
        }
    }

    private fun accountId(): String? = repoScope?.accountId ?: _uiState.value.selected?.accountId

    private fun workspaceSlug(): String? = repoScope?.workspaceSlug ?: _uiState.value.selected?.slug

    private fun effectiveQuery(userQuery: String): String =
        repoScope?.let { "repo:${it.repoSlug} $userQuery" } ?: userQuery

    private fun scopeKey(): String = repoScope?.repoSlug ?: _uiState.value.selected?.id.orEmpty()

    private fun isCurrent(userQuery: String, scopeKey: String): Boolean {
        val state = _uiState.value
        return scopeKey() == scopeKey && state.query.trim() == userQuery
    }

    private companion object {
        const val MIN_QUERY = 2
        const val DEBOUNCE_MS = 500L
    }
}
