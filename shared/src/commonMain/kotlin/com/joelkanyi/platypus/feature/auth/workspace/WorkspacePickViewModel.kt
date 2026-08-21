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
package com.joelkanyi.platypus.feature.auth.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.domain.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkspacePickViewModel(private val authRepository: AuthRepository, private val accountId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkspacePickUiState())
    val uiState: StateFlow<WorkspacePickUiState> = _uiState.asStateFlow()

    private val _effects = Channel<WorkspacePickUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        load()
    }

    fun onEvent(event: WorkspacePickUiEvent) {
        when (event) {
            WorkspacePickUiEvent.Retry -> load()
            WorkspacePickUiEvent.SignOut -> viewModelScope.launch { authRepository.signOut(accountId) }
            is WorkspacePickUiEvent.Select -> _effects.trySend(WorkspacePickUiEffect.Selected(event.workspace))
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepository.workspaces(accountId)) {
                is NetworkResult.Success ->
                    _uiState.update { it.copy(isLoading = false, workspaces = result.data) }
                is NetworkResult.Failure ->
                    _uiState.update { it.copy(isLoading = false, error = result.userMessage()) }
            }
        }
    }
}
