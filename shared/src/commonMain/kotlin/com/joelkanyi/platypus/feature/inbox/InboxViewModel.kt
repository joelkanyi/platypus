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
package com.joelkanyi.platypus.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelkanyi.platypus.domain.model.InboxFilter
import com.joelkanyi.platypus.domain.model.WatchedRepo
import com.joelkanyi.platypus.domain.repository.InboxCache
import com.joelkanyi.platypus.domain.repository.WatchlistRepository
import com.joelkanyi.platypus.domain.usecase.GetReviewInbox
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class InboxViewModel(
    private val getReviewInbox: GetReviewInbox,
    private val watchlistRepository: WatchlistRepository,
    private val inboxCache: InboxCache,
    initialFilter: InboxFilter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState(filter = initialFilter))
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            inboxCache.load()?.let { cached ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        pullRequests = cached.pullRequests,
                        lastUpdatedEpochMs = cached.updatedAtEpochMs,
                    )
                }
            }
            var firstEmission = true
            watchlistRepository.watchedAll()
                .map { watched -> watched.map(WatchedRepo::identity).toSet() }
                .distinctUntilChanged()
                .collectLatest { keys ->
                    val isFirst = firstEmission
                    firstEmission = false
                    when {
                        keys.isEmpty() -> _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                hasWatchlist = false,
                                pullRequests = emptyList(),
                                failures = emptyList(),
                                lastUpdatedEpochMs = null,
                            )
                        }
                        isFirst && _uiState.value.lastUpdatedEpochMs != null -> {
                            _uiState.update { it.copy(hasWatchlist = true, isLoading = false) }
                        }
                        else -> {
                            _uiState.update { it.copy(hasWatchlist = true) }
                            fetch(refresh = false)
                        }
                    }
                }
        }
    }

    fun onEvent(event: InboxUiEvent) {
        when (event) {
            is InboxUiEvent.SelectFilter -> _uiState.update { it.copy(filter = event.filter) }
            InboxUiEvent.Refresh -> viewModelScope.launch { fetch(refresh = true) }
            InboxUiEvent.Retry -> viewModelScope.launch { fetch(refresh = false) }
        }
    }

    private suspend fun fetch(refresh: Boolean) {
        _uiState.update {
            if (refresh) it.copy(isRefreshing = true) else it.copy(isLoading = it.lastUpdatedEpochMs == null)
        }
        val inbox = getReviewInbox()
        val now = Clock.System.now().toEpochMilliseconds()
        inboxCache.save(inbox.pullRequests, now)
        _uiState.update {
            it.copy(
                isLoading = false,
                isRefreshing = false,
                hasWatchlist = true,
                pullRequests = inbox.pullRequests,
                failures = inbox.failures,
                lastUpdatedEpochMs = now,
            )
        }
    }
}

private fun WatchedRepo.identity(): String = "$accountId/$workspaceSlug/$repoSlug"
