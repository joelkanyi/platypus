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
package com.joelkanyi.platypus.feature.auth.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelkanyi.platypus.app.OAuthDeepLinks
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

class SignInViewModel(private val authRepository: AuthRepository, private val oauthDeepLinks: OAuthDeepLinks) :
    ViewModel() {

    private val _uiState = MutableStateFlow(
        SignInUiState(oauthConfigured = authRepository.authorizeUrl() != null),
    )
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SignInUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            oauthDeepLinks.codes.collect { code -> completeOAuth(code) }
        }
    }

    fun onEvent(event: SignInUiEvent) {
        when (event) {
            is SignInUiEvent.EmailChanged -> _uiState.update { it.copy(email = event.value) }
            is SignInUiEvent.ApiTokenChanged -> _uiState.update { it.copy(apiToken = event.value) }
            SignInUiEvent.SubmitApiToken -> submitApiToken()
            SignInUiEvent.StartOAuth -> startOAuth()
            SignInUiEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun submitApiToken() {
        val state = _uiState.value
        if (!state.canSubmitApiToken) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            when (val result = authRepository.signInWithApiToken(state.email.trim(), state.apiToken.trim())) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.trySend(SignInUiEffect.SignedIn)
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isSubmitting = false, error = result.userMessage())
                }
            }
        }
    }

    private fun startOAuth() {
        val url = authRepository.authorizeUrl() ?: return
        _effects.trySend(SignInUiEffect.OpenUrl(url))
    }

    private fun completeOAuth(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = null) }
            when (val result = authRepository.completeOAuth(code)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effects.trySend(SignInUiEffect.SignedIn)
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(isSubmitting = false, error = result.userMessage())
                }
            }
        }
    }
}
