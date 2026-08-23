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
package com.joelkanyi.platypus.feature.repositories

import androidx.compose.runtime.Composable
import com.joelkanyi.platypus.domain.model.RepoTab
import com.joelkanyi.platypus.domain.model.Repository
import com.joelkanyi.platypus.domain.model.WatchedRepo
import com.joelkanyi.platypus.domain.model.Workspace
import com.joelkanyi.platypus.preview.PlatypusPreview
import com.joelkanyi.platypus.preview.PlatypusThemePreviews

private val sampleWorkspace = Workspace("{w}", "acme", "Acme Corp", null)
private val sampleOption = WorkspaceOption("1", "Joel Kanyi", sampleWorkspace)

private fun watched(slug: String, name: String, workspace: String = "acme") = WatchedRepo(
    accountId = "1",
    workspaceSlug = workspace,
    repoSlug = slug,
    repoUuid = "{$workspace/$slug}",
    name = name,
    fullName = "$workspace/$slug",
    avatarUrl = null,
)

private val sampleWatched = listOf(
    watched("api-gateway", "API Gateway"),
    watched("mobile", "Mobile"),
    watched("billing", "Billing", workspace = "beta"),
)

private fun repo(slug: String, name: String, private: Boolean) = Repository(
    uuid = "{$slug}",
    workspaceSlug = "acme",
    slug = slug,
    name = name,
    fullName = "acme/$slug",
    description = "",
    isPrivate = private,
    avatarUrl = null,
)

private val sampleRepos = listOf(
    RepoRow(repo("api-gateway", "API Gateway", true), watched = true),
    RepoRow(repo("web-app", "Web App", false), watched = false),
    RepoRow(repo("infra", "Infrastructure", true), watched = false),
)

@PlatypusThemePreviews
@Composable
private fun RepositoriesWatchingPreview() {
    PlatypusPreview {
        RepositoriesContent(
            state = RepositoriesUiState(
                tab = RepoTab.WATCHING,
                watched = sampleWatched,
                watchedCount = sampleWatched.size,
                isLoadingWorkspaces = false,
                workspaces = listOf(sampleOption),
                selected = sampleOption,
            ),
            onEvent = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun RepositoriesWatchingEmptyPreview() {
    PlatypusPreview {
        RepositoriesContent(
            state = RepositoriesUiState(tab = RepoTab.WATCHING, isLoadingWorkspaces = false),
            onEvent = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun RepositoriesBrowsePreview() {
    PlatypusPreview {
        RepositoriesContent(
            state = RepositoriesUiState(
                tab = RepoTab.BROWSE,
                watchedCount = 3,
                isLoadingWorkspaces = false,
                workspaces = listOf(sampleOption),
                selected = sampleOption,
                repos = sampleRepos,
            ),
            onEvent = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun RepositoriesBrowseErrorPreview() {
    PlatypusPreview {
        RepositoriesContent(
            state = RepositoriesUiState(
                tab = RepoTab.BROWSE,
                isLoadingWorkspaces = false,
                workspacesError = "You are offline.",
            ),
            onEvent = {},
        )
    }
}
