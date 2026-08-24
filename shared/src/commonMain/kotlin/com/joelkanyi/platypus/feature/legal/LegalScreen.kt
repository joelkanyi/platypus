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
package com.joelkanyi.platypus.feature.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.joelkanyi.platypus.designsystem.expand
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme

data class LegalSection(val heading: String, val body: String)

@Composable
fun PrivacyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) =
    LegalScreen("Privacy Policy", LAST_UPDATED, privacySections, onBack, modifier)

@Composable
fun TermsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) =
    LegalScreen("Terms of Use", LAST_UPDATED, termsSections, onBack, modifier)

@Composable
internal fun LegalScreen(
    title: String,
    lastUpdated: String,
    sections: List<LegalSection>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = title,
                navigationIcon = {
                    JengaIconButton(onClick = onBack) {
                        JengaIcon(JengaIcons.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding.expand(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            item {
                JengaText(
                    text = lastUpdated,
                    style = JengaTheme.typography.caption,
                    color = JengaTheme.colors.textMuted,
                )
            }
            items(sections) { section ->
                Column(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    JengaText(
                        text = section.heading,
                        style = JengaTheme.typography.titleSmall,
                        color = JengaTheme.colors.textPrimary,
                    )
                    JengaText(
                        text = section.body,
                        style = JengaTheme.typography.bodyMedium,
                        color = JengaTheme.colors.textSecondary,
                    )
                }
            }
        }
    }
}

private const val LAST_UPDATED = "Last updated: August 2026"

private val privacySections = listOf(
    LegalSection(
        "The short version",
        "Platypus is an unofficial Bitbucket Cloud client. It has no servers of its own for your content. " +
            "Your Bitbucket credentials and data stay on your device and are sent only to Atlassian's Bitbucket " +
            "API to do what you ask.",
    ),
    LegalSection(
        "What is stored on your device",
        "Your sign-in credentials (Atlassian API token or OAuth refresh token) are kept in the platform secure " +
            "store (Android Keystore-backed encrypted storage; iOS Keychain). The app also stores, locally, the " +
            "repositories you choose to watch, a cache of pull requests for your inbox, and your app settings.",
    ),
    LegalSection(
        "What is sent, and to whom",
        "Requests go directly from your device to Atlassian's Bitbucket API (api.bitbucket.org). If you use OAuth " +
            "sign-in, the authorization code and token refresh are relayed through a stateless backend that only " +
            "attaches the OAuth secret and forwards the request to Atlassian; it stores nothing.",
    ),
    LegalSection(
        "No analytics or tracking",
        "Platypus does not include analytics, advertising, or third-party tracking SDKs, and does not collect a " +
            "device identifier or usage profile.",
    ),
    LegalSection(
        "Deleting your data",
        "Signing out removes an account's session. Delete account (in Profile) removes that account together with " +
            "its watched repositories and cached pull requests from this device. To delete your Atlassian account " +
            "itself, manage it at id.atlassian.com.",
    ),
    LegalSection(
        "Contact",
        "Questions about privacy can be raised on the project's issue tracker.",
    ),
)

private val termsSections = listOf(
    LegalSection(
        "Unofficial client",
        "Platypus is an independent, unofficial client for Bitbucket Cloud. It is not affiliated with, endorsed " +
            "by, or sponsored by Atlassian. Bitbucket is a trademark of Atlassian.",
    ),
    LegalSection(
        "Your Bitbucket account",
        "You are responsible for your own Bitbucket credentials and for complying with Atlassian's terms of " +
            "service when you use Platypus to access Bitbucket.",
    ),
    LegalSection(
        "No warranty",
        "The app is provided \"as is\", without warranty of any kind. To the extent permitted by law, the author " +
            "is not liable for any loss or damage arising from its use.",
    ),
    LegalSection(
        "Licensing",
        "Platypus is open-source software released under the Apache License 2.0.",
    ),
)
