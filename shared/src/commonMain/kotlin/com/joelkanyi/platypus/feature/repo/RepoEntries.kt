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
package com.joelkanyi.platypus.feature.repo

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.joelkanyi.platypus.feature.repo.browse.RepositoryBrowseScreen
import com.joelkanyi.platypus.feature.repo.commits.CommitDetailScreen
import com.joelkanyi.platypus.feature.repo.commits.CommitsScreen
import com.joelkanyi.platypus.feature.repo.file.FileViewerScreen
import com.joelkanyi.platypus.feature.repo.overview.RepositoryOverviewScreen
import com.joelkanyi.platypus.navigation.CommitDetailKey
import com.joelkanyi.platypus.navigation.CommitsKey
import com.joelkanyi.platypus.navigation.DeploymentsKey
import com.joelkanyi.platypus.navigation.FileViewerKey
import com.joelkanyi.platypus.navigation.Navigator
import com.joelkanyi.platypus.navigation.PipelinesKey
import com.joelkanyi.platypus.navigation.RepoPullRequestsKey
import com.joelkanyi.platypus.navigation.RepositoryBrowseKey
import com.joelkanyi.platypus.navigation.RepositoryOverviewKey
import com.joelkanyi.platypus.navigation.SchedulesKey
import com.joelkanyi.platypus.navigation.SearchKey

fun EntryProviderScope<NavKey>.repoEntries(navigator: Navigator, onOpenUrl: (String) -> Unit) {
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
                navigator.navigate(RepoPullRequestsKey(key.accountId, key.workspace, key.repoSlug, key.repoName))
            },
            onOpenPipelines = {
                navigator.navigate(PipelinesKey(key.accountId, key.workspace, key.repoSlug, key.repoName))
            },
            onOpenDeployments = {
                navigator.navigate(DeploymentsKey(key.accountId, key.workspace, key.repoSlug, key.repoName))
            },
            onOpenSchedules = {
                navigator.navigate(SchedulesKey(key.accountId, key.workspace, key.repoSlug, key.repoName))
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
                navigator.navigate(RepositoryBrowseKey(key.accountId, key.workspace, key.repoSlug, key.ref, targetPath))
            },
            onOpenUrl = onOpenUrl,
            onBack = navigator::goBack,
            fromSearch = key.fromSearch,
            onViewLatest = { defaultRef ->
                navigator.navigate(FileViewerKey(key.accountId, key.workspace, key.repoSlug, defaultRef, key.path))
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
