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
package com.joelkanyi.platypus.feature.pipelines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.domain.model.AccountId
import com.joelkanyi.platypus.domain.model.Pipeline
import com.joelkanyi.platypus.domain.model.RepoRef
import com.joelkanyi.platypus.domain.model.RepoSlug
import com.joelkanyi.platypus.domain.model.WorkspaceSlug
import com.joelkanyi.platypus.domain.model.rerunRequest
import com.joelkanyi.platypus.domain.repository.PipelineRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PipelineDetailViewModel(
    private val repository: PipelineRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
    private val pipelineUuid: String,
) : ViewModel() {

    private val repoRef = RepoRef(AccountId(accountId), WorkspaceSlug(workspace), RepoSlug(repoSlug))

    private val _uiState = MutableStateFlow(PipelineDetailUiState())
    val uiState: StateFlow<PipelineDetailUiState> = _uiState.asStateFlow()

    init {
        load()
        poll()
    }

    fun retry() = load()

    fun clearActionError() = _uiState.update { it.copy(actionError = null) }

    fun stop() {
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionError = null) }
            when (val result = repository.stop(repoRef, pipelineUuid)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(actionInProgress = false) }
                    load()
                }
                is NetworkResult.Failure ->
                    _uiState.update { it.copy(actionInProgress = false, actionError = result.userMessage()) }
            }
        }
    }

    fun rerun(onTriggered: (Pipeline) -> Unit) {
        val request = _uiState.value.pipeline?.rerunRequest() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(actionInProgress = true, actionError = null) }
            when (val result = repository.trigger(repoRef, request)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(actionInProgress = false) }
                    onTriggered(result.data)
                }
                is NetworkResult.Failure ->
                    _uiState.update { it.copy(actionInProgress = false, actionError = result.userMessage()) }
            }
        }
    }

    private fun poll() {
        viewModelScope.launch {
            while (true) {
                delay(POLL_MS)
                val state = _uiState.value
                if (!state.isLoading && !state.actionInProgress && state.pipeline?.status?.isRunning == true) {
                    silentReload()
                }
            }
        }
    }

    private suspend fun silentReload() {
        val pipeline = repository.pipeline(repoRef, pipelineUuid)
        if (pipeline is NetworkResult.Success) {
            _uiState.update { if (it.pipeline == pipeline.data) it else it.copy(pipeline = pipeline.data) }
        }
        val steps = repository.steps(repoRef, pipelineUuid)
        if (steps is NetworkResult.Success) {
            _uiState.update { if (it.steps == steps.data) it else it.copy(steps = steps.data.toImmutableList()) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.pipeline(repoRef, pipelineUuid)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, pipeline = result.data) }
                    loadSteps()
                }
                is NetworkResult.Failure ->
                    _uiState.update { it.copy(isLoading = false, error = result.userMessage()) }
            }
        }
    }

    private fun loadSteps() {
        viewModelScope.launch {
            val result = repository.steps(repoRef, pipelineUuid)
            if (result is NetworkResult.Success) _uiState.update { it.copy(steps = result.data.toImmutableList()) }
        }
    }

    private companion object {
        const val POLL_MS = 10_000L
    }
}
