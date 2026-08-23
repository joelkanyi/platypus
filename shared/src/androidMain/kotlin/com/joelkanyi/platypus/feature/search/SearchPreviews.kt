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

import androidx.compose.runtime.Composable
import com.joelkanyi.platypus.domain.model.CodeLine
import com.joelkanyi.platypus.domain.model.CodeSearchResult
import com.joelkanyi.platypus.domain.model.CodeSegment
import com.joelkanyi.platypus.preview.PlatypusPreview
import com.joelkanyi.platypus.preview.PlatypusThemePreviews

private val sampleWorkspace = SearchWorkspace(
    accountId = "acc-1",
    accountLabel = "Joel Kanyi",
    slug = "acme",
    name = "acme-tools",
    avatarUrl = null,
)

private fun line(number: Int, vararg segments: Pair<String, Boolean>): CodeLine =
    CodeLine(segments.map { CodeSegment(it.first, it.second) }, lineNumber = number)

private val sampleCode = listOf(
    CodeSearchResult(
        workspaceSlug = "acme",
        repoSlug = "api-gateway",
        repoName = "api-gateway",
        path = "src/main/kotlin/auth/TokenStore.kt",
        commitHash = "a1b2c3d4e5f6",
        matchCount = 4,
        snippet = listOf(
            line(12, "    fun refresh(" to false, "token" to true, ": String) {" to false),
            line(18, "        val next = " to false, "token" to true, ".rotate()" to false),
        ),
        pathSegments = emptyList(),
    ),
    CodeSearchResult(
        workspaceSlug = "acme",
        repoSlug = "billing",
        repoName = "billing",
        path = "internal/token.go",
        commitHash = "0f9e8d7c",
        matchCount = 1,
        snippet = listOf(line(7, "var " to false, "token" to true, " = load()" to false)),
        pathSegments = emptyList(),
    ),
)

private fun baseState() = SearchUiState(
    isLoadingWorkspaces = false,
    workspaces = listOf(sampleWorkspace),
    selected = sampleWorkspace,
    query = "token",
)

@PlatypusThemePreviews
@Composable
private fun SearchLoadedPreview() {
    PlatypusPreview {
        SearchContent(
            state = baseState().copy(status = SearchStatus.Loaded, results = sampleCode),
            onEvent = {},
            onOpenCode = { _, _ -> },
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun SearchIdlePreview() {
    PlatypusPreview {
        SearchContent(
            state = baseState().copy(query = "", status = SearchStatus.Idle),
            onEvent = {},
            onOpenCode = { _, _ -> },
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun SearchRepoScopedPreview() {
    PlatypusPreview {
        SearchContent(
            state = SearchUiState(
                repoScope = RepoScope("acc-1", "acme", "api-gateway", "api-gateway"),
                isLoadingWorkspaces = false,
                query = "token",
                status = SearchStatus.Loaded,
                results = sampleCode,
            ),
            onEvent = {},
            onOpenCode = { _, _ -> },
            onBack = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun SearchGatedPreview() {
    PlatypusPreview {
        SearchContent(
            state = baseState().copy(status = SearchStatus.Gated),
            onEvent = {},
            onOpenCode = { _, _ -> },
        )
    }
}
