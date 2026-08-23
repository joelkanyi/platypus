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
import com.joelkanyi.platypus.feature.pipelines.DeploymentsScreen
import com.joelkanyi.platypus.feature.pipelines.PipelineDetailScreen
import com.joelkanyi.platypus.feature.pipelines.PipelineListScreen
import com.joelkanyi.platypus.feature.pipelines.PipelineStepLogScreen
import com.joelkanyi.platypus.feature.pipelines.SchedulesScreen
import com.joelkanyi.platypus.feature.pr.commits.PrCommitsScreen
import com.joelkanyi.platypus.feature.pr.detail.PrDetailScreen
import com.joelkanyi.platypus.feature.pr.files.FilesChangedScreen
import com.joelkanyi.platypus.feature.pr.files.PrFileDiffScreen
import com.joelkanyi.platypus.feature.pr.list.RepoPullRequestsScreen
import com.joelkanyi.platypus.feature.profile.ProfileScreen
import com.joelkanyi.platypus.feature.repo.browse.RepositoryBrowseScreen
import com.joelkanyi.platypus.feature.repo.commits.CommitDetailScreen
import com.joelkanyi.platypus.feature.repo.commits.CommitsScreen
import com.joelkanyi.platypus.feature.repo.file.FileViewerScreen
import com.joelkanyi.platypus.feature.repo.overview.RepositoryOverviewScreen
import com.joelkanyi.platypus.feature.repositories.RepositoriesScreen
import com.joelkanyi.platypus.feature.search.SearchScreen
import com.joelkanyi.platypus.feature.settings.SettingsScreen
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
        entry<InboxKey> {
            InboxScreen(
                onBrowseWatchlist = { navigator.navigate(RepositoriesKey) },
                onOpenPullRequest = { pr ->
                    navigator.navigate(
                        PullRequestKey(pr.accountId, pr.workspaceSlug, pr.repoSlug, pr.id, pr.repoName),
                    )
                },
            )
        }
        entry<RepositoriesKey> {
            RepositoriesScreen(
                onOpenRepo = { repo ->
                    navigator.navigate(
                        RepositoryOverviewKey(repo.accountId, repo.workspaceSlug, repo.repoSlug, repo.name),
                    )
                },
            )
        }
        entry<ProfileKey> {
            ProfileScreen(onOpenSettings = { navigator.navigate(SettingsKey) })
        }
        entry<SettingsKey> { SettingsScreen(onBack = navigator::goBack) }
        entry<SearchKey> { key ->
            SearchScreen(
                accountId = key.accountId,
                workspaceSlug = key.workspaceSlug,
                repoSlug = key.repoSlug,
                repoName = key.repoName,
                onOpenCode = { accountId, result ->
                    navigator.navigate(
                        FileViewerKey(
                            accountId = accountId,
                            workspace = result.workspaceSlug,
                            repoSlug = result.repoSlug,
                            ref = result.commitHash,
                            path = result.path,
                            fromSearch = true,
                        ),
                    )
                },
                onBack = if (key.repoSlug != null) navigator::goBack else null,
            )
        }
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
                onOpenPullRequests = {
                    navigator.navigate(
                        RepoPullRequestsKey(key.accountId, key.workspace, key.repoSlug, key.repoName),
                    )
                },
                onOpenPipelines = {
                    navigator.navigate(
                        PipelinesKey(key.accountId, key.workspace, key.repoSlug, key.repoName),
                    )
                },
                onOpenDeployments = {
                    navigator.navigate(
                        DeploymentsKey(key.accountId, key.workspace, key.repoSlug, key.repoName),
                    )
                },
                onOpenSchedules = {
                    navigator.navigate(
                        SchedulesKey(key.accountId, key.workspace, key.repoSlug, key.repoName),
                    )
                },
                onSearchRepo = {
                    navigator.navigate(
                        SearchKey(
                            accountId = key.accountId,
                            workspaceSlug = key.workspace,
                            repoSlug = key.repoSlug,
                            repoName = key.repoName,
                        ),
                    )
                },
                onOpenUrl = onOpenUrl,
                onBack = navigator::goBack,
            )
        }
        entry<DeploymentsKey> { key ->
            DeploymentsScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                repoName = key.repoName,
                onBack = navigator::goBack,
            )
        }
        entry<SchedulesKey> { key ->
            SchedulesScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                repoName = key.repoName,
                onBack = navigator::goBack,
            )
        }
        entry<PipelinesKey> { key ->
            PipelineListScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                repoName = key.repoName,
                onOpenPipeline = { pipeline ->
                    navigator.navigate(
                        PipelineDetailKey(
                            key.accountId,
                            key.workspace,
                            key.repoSlug,
                            pipeline.uuid,
                            pipeline.buildNumber,
                        ),
                    )
                },
                onBack = navigator::goBack,
            )
        }
        entry<PipelineDetailKey> { key ->
            PipelineDetailScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                pipelineUuid = key.pipelineUuid,
                buildNumber = key.buildNumber,
                onOpenStepLog = { step ->
                    navigator.navigate(
                        PipelineStepLogKey(
                            key.accountId,
                            key.workspace,
                            key.repoSlug,
                            key.pipelineUuid,
                            step.uuid,
                            step.name,
                        ),
                    )
                },
                onOpenPipeline = { pipeline ->
                    navigator.navigate(
                        PipelineDetailKey(
                            key.accountId,
                            key.workspace,
                            key.repoSlug,
                            pipeline.uuid,
                            pipeline.buildNumber,
                        ),
                    )
                },
                onOpenCommit = { hash ->
                    navigator.navigate(CommitDetailKey(key.accountId, key.workspace, key.repoSlug, hash))
                },
                onOpenPullRequest = { prId ->
                    navigator.navigate(PullRequestKey(key.accountId, key.workspace, key.repoSlug, prId, key.repoSlug))
                },
                onBack = navigator::goBack,
            )
        }
        entry<PipelineStepLogKey> { key ->
            PipelineStepLogScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                pipelineUuid = key.pipelineUuid,
                stepUuid = key.stepUuid,
                stepName = key.stepName,
                onBack = navigator::goBack,
            )
        }
        entry<RepoPullRequestsKey> { key ->
            RepoPullRequestsScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                repoName = key.repoName,
                onOpenPullRequest = { pr ->
                    navigator.navigate(
                        PullRequestKey(pr.accountId, pr.workspaceSlug, pr.repoSlug, pr.id, pr.repoName),
                    )
                },
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
                fromSearch = key.fromSearch,
                onViewLatest = { defaultRef ->
                    navigator.navigate(
                        FileViewerKey(key.accountId, key.workspace, key.repoSlug, defaultRef, key.path),
                    )
                },
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
        entry<PullRequestKey> { key ->
            PrDetailScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                prId = key.prId,
                repoName = key.repoName,
                onBack = navigator::goBack,
                onOpenFiles = {
                    navigator.navigate(
                        FilesChangedKey(key.accountId, key.workspace, key.repoSlug, key.prId, key.repoName),
                    )
                },
                onOpenCommits = {
                    navigator.navigate(
                        PrCommitsKey(key.accountId, key.workspace, key.repoSlug, key.prId, key.repoName),
                    )
                },
                onOpenUrl = onOpenUrl,
            )
        }
        entry<PrCommitsKey> { key ->
            PrCommitsScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                prId = key.prId,
                onOpenCommit = { hash ->
                    navigator.navigate(CommitDetailKey(key.accountId, key.workspace, key.repoSlug, hash))
                },
                onBack = navigator::goBack,
            )
        }
        entry<FilesChangedKey> { key ->
            FilesChangedScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                prId = key.prId,
                onOpenFile = { path ->
                    navigator.navigate(
                        PrFileDiffKey(key.accountId, key.workspace, key.repoSlug, key.prId, path),
                    )
                },
                onBack = navigator::goBack,
            )
        }
        entry<PrFileDiffKey> { key ->
            PrFileDiffScreen(
                accountId = key.accountId,
                workspace = key.workspace,
                repoSlug = key.repoSlug,
                prId = key.prId,
                path = key.path,
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

private val TopLevelDestination.label: String
    get() = when (this) {
        TopLevelDestination.REPOSITORIES -> "Repositories"
        TopLevelDestination.INBOX -> "Inbox"
        TopLevelDestination.SEARCH -> "Search"
        TopLevelDestination.PROFILE -> "Profile"
    }

private val TopLevelDestination.icon
    @Composable get() = when (this) {
        TopLevelDestination.REPOSITORIES -> JengaIcons.Database
        TopLevelDestination.INBOX -> JengaIcons.Bell
        TopLevelDestination.SEARCH -> JengaIcons.Search
        TopLevelDestination.PROFILE -> JengaIcons.User
    }
