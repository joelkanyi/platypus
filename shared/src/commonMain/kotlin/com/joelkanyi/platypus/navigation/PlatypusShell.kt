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
package com.joelkanyi.platypus.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.navigation.JengaNavIndicator
import io.github.joelkanyi.jenga.component.navigation.JengaNavigationBar
import io.github.joelkanyi.jenga.component.navigation.JengaNavigationBarItem
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.state.JengaEmptyState

@Composable
fun PlatypusShell() {
    val navigationState = rememberNavigationState(
        startRoute = InboxKey,
        topLevelRoutes = TopLevelDestination.entries.map { it.root }.toSet(),
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    val entryProvider = entryProvider<NavKey> {
        entry<InboxKey> { TabPlaceholder("Inbox") }
        entry<PullRequestsKey> { TabPlaceholder("Pull Requests") }
        entry<RepositoriesKey> { TabPlaceholder("Repositories") }
        entry<PipelinesKey> { TabPlaceholder("Pipelines") }
        entry<ProfileKey> { TabPlaceholder("Profile") }
    }

    JengaScaffold(
        bottomBar = {
            if (navigationState.atTabRoot) {
                JengaNavigationBar {
                    TopLevelDestination.entries.forEach { tab ->
                        JengaNavigationBarItem(
                            selected = navigationState.topLevelRoute == tab.root,
                            onClick = { navigator.navigate(tab.root) },
                            icon = { JengaIcon(tab.icon, contentDescription = tab.label) },
                            label = tab.label,
                            indicator = JengaNavIndicator.Pill,
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavDisplay(
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() },
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

@Composable
private fun TabPlaceholder(title: String) {
    JengaEmptyState(
        title = title,
        description = "Coming soon",
        modifier = Modifier.fillMaxSize(),
    )
}

private val TopLevelDestination.label: String
    get() = when (this) {
        TopLevelDestination.INBOX -> "Inbox"
        TopLevelDestination.PULL_REQUESTS -> "Pull Requests"
        TopLevelDestination.REPOSITORIES -> "Repositories"
        TopLevelDestination.PIPELINES -> "Pipelines"
        TopLevelDestination.PROFILE -> "Profile"
    }

private val TopLevelDestination.icon
    @Composable get() = when (this) {
        TopLevelDestination.INBOX -> JengaIcons.Bell
        TopLevelDestination.PULL_REQUESTS -> JengaIcons.MessageCircle
        TopLevelDestination.REPOSITORIES -> JengaIcons.Database
        TopLevelDestination.PIPELINES -> JengaIcons.Flash
        TopLevelDestination.PROFILE -> JengaIcons.User
    }
