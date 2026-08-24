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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.OAuthDeepLinks
import com.joelkanyi.platypus.designsystem.expand
import com.joelkanyi.platypus.domain.repository.AuthRepository
import io.github.joelkanyi.jenga.component.button.JengaButton
import io.github.joelkanyi.jenga.component.button.JengaButtonVariant
import io.github.joelkanyi.jenga.component.divider.JengaDivider
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.component.textfield.JengaTextField
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun SignInScreen(
    authRepository: AuthRepository,
    oauthDeepLinks: OAuthDeepLinks,
    openUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSignedIn: () -> Unit = {},
) {
    val viewModel = viewModel { SignInViewModel(authRepository, oauthDeepLinks) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SignInUiEffect.OpenUrl -> openUrl(effect.url)
                SignInUiEffect.SignedIn -> onSignedIn()
            }
        }
    }

    SignInContent(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

@Composable
internal fun SignInContent(state: SignInUiState, onEvent: (SignInUiEvent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = JengaTheme.spacing
    val itemModifier = Modifier.fillMaxWidth().widthIn(max = 440.dp)

    JengaScaffold(modifier = modifier) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = innerPadding.expand(horizontal = spacing.lg, vertical = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { Header(itemModifier) }

            item {
                JengaTextField(
                    value = state.email,
                    onValueChange = { onEvent(SignInUiEvent.EmailChanged(it)) },
                    modifier = itemModifier,
                    label = "Email",
                    placeholder = "you@example.com",
                    enabled = !state.isSubmitting,
                    leadingIcon = {
                        JengaIcon(JengaIcons.Mail, contentDescription = null, tint = JengaTheme.colors.textMuted)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                )
            }

            item {
                ApiTokenField(
                    value = state.apiToken,
                    onValueChange = { onEvent(SignInUiEvent.ApiTokenChanged(it)) },
                    enabled = !state.isSubmitting,
                    onSubmit = { onEvent(SignInUiEvent.SubmitApiToken) },
                    modifier = itemModifier,
                )
            }

            state.error?.let { message ->
                item {
                    JengaText(
                        text = message,
                        style = JengaTheme.typography.bodySmall,
                        color = JengaTheme.colors.error,
                        modifier = itemModifier,
                    )
                }
            }

            item {
                JengaButton(
                    text = "Sign in",
                    onClick = { onEvent(SignInUiEvent.SubmitApiToken) },
                    modifier = itemModifier,
                    enabled = state.canSubmitApiToken,
                    loading = state.isSubmitting,
                )
            }

            item { LabeledDivider(label = "Or", modifier = itemModifier) }

            item {
                JengaButton(
                    text = "Sign in with Bitbucket",
                    onClick = { onEvent(SignInUiEvent.StartOAuth) },
                    modifier = itemModifier,
                    variant = JengaButtonVariant.Outline,
                    enabled = state.oauthConfigured && !state.isSubmitting,
                )
            }

            if (!state.oauthConfigured) {
                item {
                    JengaText(
                        text = "Bitbucket sign-in is unavailable until the app is configured.",
                        style = JengaTheme.typography.caption,
                        color = JengaTheme.colors.textMuted,
                        modifier = itemModifier,
                    )
                }
            }

            item { TrustFootnote(itemModifier) }
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    val spacing = JengaTheme.spacing
    Column(
        modifier = modifier.padding(bottom = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        JengaText(
            text = "Sign in to Platypus",
            style = JengaTheme.typography.headingMedium,
        )
        JengaText(
            text = "Review Bitbucket pull requests, pipelines, and diffs from your phone.",
            style = JengaTheme.typography.bodyMedium,
            color = JengaTheme.colors.textSecondary,
        )
    }
}

@Composable
internal fun ApiTokenField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }
    JengaTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = "API token",
        placeholder = "Paste your scoped API token",
        supportingText = "Create one at id.atlassian.com under API tokens.",
        enabled = enabled,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        leadingIcon = {
            JengaIcon(JengaIcons.Lock, contentDescription = null, tint = JengaTheme.colors.textMuted)
        },
        trailingIcon = {
            JengaIcon(
                imageVector = if (visible) JengaIcons.EyeOff else JengaIcons.Eye,
                contentDescription = if (visible) "Hide token" else "Show token",
                tint = JengaTheme.colors.textMuted,
                modifier = Modifier.clickable(enabled = enabled) { visible = !visible },
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
    )
}

@Composable
private fun LabeledDivider(label: String, modifier: Modifier = Modifier) {
    val spacing = JengaTheme.spacing
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        JengaDivider(modifier = Modifier.weight(1f))
        JengaText(
            text = label,
            style = JengaTheme.typography.caption,
            color = JengaTheme.colors.textMuted,
        )
        JengaDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TrustFootnote(modifier: Modifier = Modifier) {
    JengaText(
        text = "Platypus never requests, proxies, or stores your repository content. " +
            "Code goes device to Bitbucket only. The optional push relay sees event metadata, not diffs.",
        style = JengaTheme.typography.caption,
        color = JengaTheme.colors.textMuted,
        modifier = modifier,
    )
}
