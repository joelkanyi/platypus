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
package com.joelkanyi.platypus.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.domain.model.Branch
import com.joelkanyi.platypus.domain.repository.RepoContentRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class BranchesUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val branches: ImmutableList<Branch> = persistentListOf(),
)

class BranchesViewModel(
    private val repoContentRepository: RepoContentRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BranchesUiState())
    val uiState: StateFlow<BranchesUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repoContentRepository.branches(accountId, workspace, repoSlug)) {
                is NetworkResult.Success ->
                    _uiState.update { it.copy(isLoading = false, branches = result.data.toImmutableList()) }
                is NetworkResult.Failure ->
                    _uiState.update { it.copy(isLoading = false, error = result.userMessage()) }
            }
        }
    }
}
