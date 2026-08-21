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

import androidx.compose.runtime.Immutable
import com.joelkanyi.platypus.domain.model.Repository
import com.joelkanyi.platypus.domain.model.WatchedRepo
import com.joelkanyi.platypus.domain.model.Workspace

enum class RepoTab { WATCHING, BROWSE }

@Immutable
data class WorkspaceOption(val accountId: String, val accountLabel: String, val workspace: Workspace) {
    val id: String get() = "$accountId/${workspace.slug}"
}

@Immutable
data class RepoRow(val repo: Repository, val watched: Boolean)

@Immutable
data class RepositoriesUiState(
    val tab: RepoTab = RepoTab.WATCHING,
    val watched: List<WatchedRepo> = emptyList(),
    val watchedCount: Int = 0,
    val isLoadingWorkspaces: Boolean = true,
    val workspacesError: String? = null,
    val workspaces: List<WorkspaceOption> = emptyList(),
    val selected: WorkspaceOption? = null,
    val multiAccount: Boolean = false,
    val query: String = "",
    val isLoadingRepos: Boolean = false,
    val isPaginating: Boolean = false,
    val reposError: String? = null,
    val repos: List<RepoRow> = emptyList(),
    val nextCursor: String? = null,
) {
    val canLoadMore: Boolean get() = nextCursor != null && !isPaginating && !isLoadingRepos
}

sealed interface RepositoriesUiEvent {
    data class SelectTab(val tab: RepoTab) : RepositoriesUiEvent

    data class SelectWorkspace(val option: WorkspaceOption) : RepositoriesUiEvent

    data class QueryChanged(val query: String) : RepositoriesUiEvent

    data class ToggleWatch(val repo: Repository, val watch: Boolean) : RepositoriesUiEvent

    data class Unwatch(val repo: WatchedRepo) : RepositoriesUiEvent

    data object LoadMore : RepositoriesUiEvent

    data object RetryWorkspaces : RepositoriesUiEvent

    data object RetryRepos : RepositoriesUiEvent
}
