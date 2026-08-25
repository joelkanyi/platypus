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

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.feature.inbox.inboxEntries
import com.joelkanyi.platypus.feature.legal.legalEntries
import com.joelkanyi.platypus.feature.pipelines.pipelineEntries
import com.joelkanyi.platypus.feature.pr.prEntries
import com.joelkanyi.platypus.feature.profile.profileEntries
import com.joelkanyi.platypus.feature.repo.repoEntries
import com.joelkanyi.platypus.feature.repositories.repositoriesEntries
import com.joelkanyi.platypus.feature.search.searchEntries
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.navigation.JengaNavIndicator
import io.github.joelkanyi.jenga.component.navigation.JengaNavigationBar
import io.github.joelkanyi.jenga.component.navigation.JengaNavigationBarItem
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold

@Composable
fun PlatypusShell() {
    val navigationState = rememberNavigationState(
        startRoute = RepositoriesKey,
        topLevelRoutes = TopLevelDestination.entries.map { it.root }.toSet(),
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val onOpenUrl: (String) -> Unit = LocalPlatypusDependencies.current::openUrl

    val entryProvider = entryProvider<NavKey> {
        inboxEntries(navigator)
        repositoriesEntries(navigator)
        profileEntries(navigator)
        legalEntries(navigator)
        searchEntries(navigator)
        repoEntries(navigator, onOpenUrl)
        pipelineEntries(navigator)
        prEntries(navigator, onOpenUrl)
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        )
    }
}

private val TopLevelDestination.label: String
    get() = when (this) {
        TopLevelDestination.REPOSITORIES -> "Repositories"
        TopLevelDestination.INBOX -> "Inbox"
        TopLevelDestination.SEARCH -> "Search"
        TopLevelDestination.PROFILE -> "Account"
    }

private val TopLevelDestination.icon
    @Composable get() = when (this) {
        TopLevelDestination.REPOSITORIES -> JengaIcons.Database
        TopLevelDestination.INBOX -> JengaIcons.Bell
        TopLevelDestination.SEARCH -> JengaIcons.Search
        TopLevelDestination.PROFILE -> JengaIcons.User
    }
