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
package com.joelkanyi.platypus.feature.repo.file

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.domain.model.AccountId
import com.joelkanyi.platypus.domain.model.RepoRef
import com.joelkanyi.platypus.domain.model.RepoSlug
import com.joelkanyi.platypus.domain.model.WorkspaceSlug
import com.joelkanyi.platypus.domain.repository.RepoContentRepository
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val MARKDOWN_EXTENSIONS = setOf("md", "markdown", "mdown", "mkd")

class FileViewerViewModel(
    private val repoContentRepository: RepoContentRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
    private val ref: String,
    private val path: String,
    renderMarkdownDefault: Boolean,
    private val fromSearch: Boolean = false,
) : ViewModel() {

    private val isMarkdown = path.substringAfterLast('.', "").lowercase() in MARKDOWN_EXTENSIONS

    private val repoRef = RepoRef(AccountId(accountId), WorkspaceSlug(workspace), RepoSlug(repoSlug))

    private val _uiState =
        MutableStateFlow(FileUiState(isMarkdown = isMarkdown, preview = isMarkdown && renderMarkdownDefault))
    val uiState: StateFlow<FileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun togglePreview() = _uiState.update { it.copy(preview = !it.preview) }

    fun toggleFind() = _uiState.update {
        if (it.findActive) {
            it.copy(findActive = false, findQuery = "", matches = persistentListOf(), matchIndex = 0)
        } else {
            it.copy(findActive = true)
        }
    }

    fun onFindQuery(query: String) = _uiState.update { state ->
        val matches = FileFind.matchingLines(query, state.file?.lines ?: emptyList()).toImmutableList()
        state.copy(findQuery = query, matches = matches, matchIndex = 0)
    }

    fun toggleOutline() = _uiState.update { it.copy(outlineOpen = !it.outlineOpen) }

    fun jumpTo(line: Int) = _uiState.update {
        it.copy(outlineOpen = false, matches = persistentListOf(line), matchIndex = 0)
    }

    fun nextMatch() = _uiState.update { it.copy(matchIndex = FileFind.nextIndex(it.matchIndex, it.matches.size)) }

    fun previousMatch() =
        _uiState.update { it.copy(matchIndex = FileFind.previousIndex(it.matchIndex, it.matches.size)) }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repoContentRepository.file(repoRef, ref, path)) {
                is NetworkResult.Success -> _uiState.update { it.copy(isLoading = false, file = result.data) }
                is NetworkResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.userMessage()) }
            }
        }
        if (fromSearch) loadDefaultBranch()
    }

    private fun loadDefaultBranch() {
        viewModelScope.launch {
            val result = repoContentRepository.repository(repoRef)
            if (result is NetworkResult.Success) {
                _uiState.update { it.copy(defaultBranch = result.data.defaultBranch) }
            }
        }
    }
}
