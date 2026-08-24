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
package com.joelkanyi.platypus.feature.search

import androidx.compose.runtime.Immutable
import com.joelkanyi.platypus.domain.model.CodeSearchResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class SearchWorkspace(
    val accountId: String,
    val accountLabel: String,
    val slug: String,
    val name: String,
    val avatarUrl: String?,
) {
    val id: String get() = "$accountId/$slug"
}

@Immutable
data class RepoScope(val accountId: String, val workspaceSlug: String, val repoSlug: String, val repoName: String)

sealed interface SearchStatus {
    data object Idle : SearchStatus
    data object Loading : SearchStatus
    data object Loaded : SearchStatus
    data object NoResults : SearchStatus
    data object RateLimited : SearchStatus
    data object BadQuery : SearchStatus
    data object Gated : SearchStatus
    data class Error(val message: String) : SearchStatus
}

@Immutable
data class SearchUiState(
    val repoScope: RepoScope? = null,
    val isLoadingWorkspaces: Boolean = true,
    val workspacesError: String? = null,
    val workspaces: ImmutableList<SearchWorkspace> = persistentListOf(),
    val selected: SearchWorkspace? = null,
    val showWorkspacePicker: Boolean = false,
    val query: String = "",
    val status: SearchStatus = SearchStatus.Idle,
    val results: ImmutableList<CodeSearchResult> = persistentListOf(),
    val totalFiles: Int? = null,
    val next: String? = null,
    val loadingMore: Boolean = false,
    val loadMoreError: Boolean = false,
)

sealed interface SearchUiEvent {
    data class QueryChanged(val query: String) : SearchUiEvent
    data object ClearQuery : SearchUiEvent
    data object Submit : SearchUiEvent
    data object OpenWorkspacePicker : SearchUiEvent
    data object DismissWorkspacePicker : SearchUiEvent
    data class SelectWorkspace(val workspace: SearchWorkspace) : SearchUiEvent
    data object Retry : SearchUiEvent
    data object LoadMore : SearchUiEvent
    data object RetryWorkspaces : SearchUiEvent
}
