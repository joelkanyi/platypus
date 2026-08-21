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
import com.joelkanyi.platypus.feature.inbox.InboxScreen
import com.joelkanyi.platypus.feature.profile.ProfileScreen
import com.joelkanyi.platypus.feature.repo.browse.RepositoryBrowseScreen
import com.joelkanyi.platypus.feature.repo.commits.CommitDetailScreen
import com.joelkanyi.platypus.feature.repo.commits.CommitsScreen
import com.joelkanyi.platypus.feature.repo.file.FileViewerScreen
import com.joelkanyi.platypus.feature.repo.overview.RepositoryOverviewScreen
import com.joelkanyi.platypus.feature.repositories.RepositoriesScreen
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
    val onOpenUrl: (String) -> Unit = LocalPlatypusDependencies.current::openUrl

    val entryProvider = entryProvider<NavKey> {
        entry<InboxKey> { InboxScreen(onBrowseWatchlist = { navigator.navigate(RepositoriesKey) }) }
        entry<PullRequestsKey> { TabPlaceholder("Pull Requests") }
        entry<RepositoriesKey> {
            RepositoriesScreen(
                onOpenRepo = { repo ->
                    navigator.navigate(
                        RepositoryOverviewKey(repo.accountId, repo.workspaceSlug, repo.repoSlug, repo.name),
                    )
                },
            )
        }
        entry<PipelinesKey> { TabPlaceholder("Pipelines") }
        entry<ProfileKey> { ProfileScreen() }
        entry<RepositoryOverviewKey> { key ->
            RepositoryOverviewScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                repoName = key.repoName,
                onOpenFiles = { ref ->
                    navigator.navigate(RepositoryBrowseKey(key.accountId, key.workspace, key.repoSlug, ref, ""))
                },
                onOpenCommits = { ref ->
                    navigator.navigate(CommitsKey(key.accountId, key.workspace, key.repoSlug, ref))
                },
                onOpenBranch = { ref ->
                    navigator.navigate(RepositoryBrowseKey(key.accountId, key.workspace, key.repoSlug, ref, ""))
                },
                onOpenUrl = onOpenUrl,
                onBack = navigator::goBack,
            )
        }
        entry<RepositoryBrowseKey> { key ->
            RepositoryBrowseScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                ref = key.ref,
                path = key.path,
                onOpenFile = { ref, childPath ->
                    navigator.navigate(FileViewerKey(key.accountId, key.workspace, key.repoSlug, ref, childPath))
                },
                onBack = navigator::goBack,
            )
        }
        entry<FileViewerKey> { key ->
            FileViewerScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                ref = key.ref,
                path = key.path,
                onNavigateToPath = { targetPath ->
                    navigator.navigate(
                        RepositoryBrowseKey(key.accountId, key.workspace, key.repoSlug, key.ref, targetPath),
                    )
                },
                onOpenUrl = onOpenUrl,
                onBack = navigator::goBack,
            )
        }
        entry<CommitsKey> { key ->
            CommitsScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                ref = key.ref,
                onOpenCommit = { hash ->
                    navigator.navigate(CommitDetailKey(key.accountId, key.workspace, key.repoSlug, hash))
                },
                onBack = navigator::goBack,
            )
        }
        entry<CommitDetailKey> { key ->
            CommitDetailScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                hash = key.hash,
                onBack = navigator::goBack,
            )
        }
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
