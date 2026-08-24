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
package com.joelkanyi.platypus.feature.pr

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.joelkanyi.platypus.feature.pr.commits.PrCommitsScreen
import com.joelkanyi.platypus.feature.pr.detail.PrDetailScreen
import com.joelkanyi.platypus.feature.pr.files.FilesChangedScreen
import com.joelkanyi.platypus.feature.pr.files.PrFileDiffScreen
import com.joelkanyi.platypus.feature.pr.list.RepoPullRequestsScreen
import com.joelkanyi.platypus.navigation.CommitDetailKey
import com.joelkanyi.platypus.navigation.FilesChangedKey
import com.joelkanyi.platypus.navigation.Navigator
import com.joelkanyi.platypus.navigation.PrCommitsKey
import com.joelkanyi.platypus.navigation.PrFileDiffKey
import com.joelkanyi.platypus.navigation.PullRequestKey
import com.joelkanyi.platypus.navigation.RepoPullRequestsKey

fun EntryProviderScope<NavKey>.prEntries(navigator: Navigator, onOpenUrl: (String) -> Unit) {
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
    entry<PullRequestKey> { key ->
        PrDetailScreen(
            accountId = key.accountId,
            workspace = key.workspace,
            repoSlug = key.repoSlug,
            prId = key.prId,
            repoName = key.repoName,
            onBack = navigator::goBack,
            onOpenFiles = {
                navigator.navigate(FilesChangedKey(key.accountId, key.workspace, key.repoSlug, key.prId, key.repoName))
            },
            onOpenCommits = {
                navigator.navigate(PrCommitsKey(key.accountId, key.workspace, key.repoSlug, key.prId, key.repoName))
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
                navigator.navigate(PrFileDiffKey(key.accountId, key.workspace, key.repoSlug, key.prId, path))
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
}
