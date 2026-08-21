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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.data.auth.OAuthDeepLinks
import com.joelkanyi.platypus.domain.repository.AuthRepository
import io.github.joelkanyi.jenga.component.button.JengaButton
import io.github.joelkanyi.jenga.component.button.JengaButtonVariant
import io.github.joelkanyi.jenga.component.feedback.JengaBottomSheet
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.component.textfield.JengaTextField
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun AddAccountSheet(
    authRepository: AuthRepository,
    oauthDeepLinks: OAuthDeepLinks,
    openUrl: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel = viewModel(key = "add-account") { SignInViewModel(authRepository, oauthDeepLinks) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SignInUiEffect.OpenUrl -> openUrl(effect.url)
                SignInUiEffect.SignedIn -> onDismiss()
            }
        }
    }

    val spacing = JengaTheme.spacing
    JengaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = spacing.lg)
                .padding(bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            JengaText(text = "Add account", style = JengaTheme.typography.headingMedium)
            JengaText(
                text = "Sign in another Bitbucket account to keep them side by side.",
                style = JengaTheme.typography.bodyMedium,
                color = JengaTheme.colors.textSecondary,
            )

            JengaTextField(
                value = state.email,
                onValueChange = { viewModel.onEvent(SignInUiEvent.EmailChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = "Email",
                placeholder = "you@example.com",
                enabled = !state.isSubmitting,
                leadingIcon = {
                    JengaIcon(JengaIcons.Mail, contentDescription = null, tint = JengaTheme.colors.textMuted)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )

            ApiTokenField(
                value = state.apiToken,
                onValueChange = { viewModel.onEvent(SignInUiEvent.ApiTokenChanged(it)) },
                enabled = !state.isSubmitting,
                onSubmit = { viewModel.onEvent(SignInUiEvent.SubmitApiToken) },
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let { message ->
                JengaText(
                    text = message,
                    style = JengaTheme.typography.bodySmall,
                    color = JengaTheme.colors.error,
                )
            }

            JengaButton(
                text = "Add account",
                onClick = { viewModel.onEvent(SignInUiEvent.SubmitApiToken) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSubmitApiToken,
                loading = state.isSubmitting,
            )

            if (state.oauthConfigured) {
                JengaButton(
                    text = "Add with Bitbucket",
                    onClick = { viewModel.onEvent(SignInUiEvent.StartOAuth) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = JengaButtonVariant.Outline,
                    enabled = !state.isSubmitting,
                )
            }
        }
    }
}
