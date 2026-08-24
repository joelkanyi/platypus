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
package com.joelkanyi.platypus.feature.repo.commits

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.joelkanyi.platypus.domain.model.Commit
import com.joelkanyi.platypus.domain.model.CommitDetail
import com.joelkanyi.platypus.preview.PlatypusPreview
import com.joelkanyi.platypus.preview.PlatypusThemePreviews

private val sampleCommits = listOf(
    Commit("a1b2c3d4e5", "Add retry to token refresh", "Grace Njeri", "2026-08-20T09:00:00+00:00"),
    Commit(
        "f6g7h8i9j0",
        "Fix crash on empty workspace\n\nGuard the null case.",
        "Peter Otieno",
        "2026-08-19T14:00:00+00:00",
    ),
)

@PlatypusThemePreviews
@Composable
private fun CommitsContentPreview() {
    PlatypusPreview {
        CommitsContent(
            ref = "main",
            state = CommitsUiState(isLoading = false, commits = sampleCommits, nextCursor = "next"),
            onBack = {},
            onRetry = {},
            onLoadMore = {},
            onOpenCommit = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun CommitsLoadingPreview() {
    PlatypusPreview {
        CommitsContent(
            ref = "main",
            state = CommitsUiState(isLoading = true),
            onBack = {},
            onRetry = {},
            onLoadMore = {},
            onOpenCommit = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun CommitsEmptyPreview() {
    PlatypusPreview {
        CommitsContent(
            ref = "main",
            state = CommitsUiState(isLoading = false, commits = emptyList()),
            onBack = {},
            onRetry = {},
            onLoadMore = {},
            onOpenCommit = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun CommitsErrorPreview() {
    PlatypusPreview {
        CommitsContent(
            ref = "main",
            state = CommitsUiState(isLoading = false, error = "Network unavailable", commits = emptyList()),
            onBack = {},
            onRetry = {},
            onLoadMore = {},
            onOpenCommit = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun CommitDetailLoadingPreview() {
    PlatypusPreview {
        CommitDetailContent(
            shortHash = "a1b2c3d",
            state = CommitDetailUiState(isLoading = true, detail = null),
            wrap = false,
            fontSize = 13.sp,
            onBack = {},
            onRetry = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun CommitDetailErrorPreview() {
    PlatypusPreview {
        CommitDetailContent(
            shortHash = "a1b2c3d",
            state = CommitDetailUiState(isLoading = false, error = "Couldn't reach Bitbucket", detail = null),
            wrap = false,
            fontSize = 13.sp,
            onBack = {},
            onRetry = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun CommitDetailContentPreview() {
    PlatypusPreview {
        CommitDetailContent(
            shortHash = "a1b2c3d",
            state = CommitDetailUiState(
                isLoading = false,
                detail = CommitDetail(
                    commit = sampleCommits.first(),
                    diffLines = listOf(
                        "diff --git a/App.kt b/App.kt",
                        "@@ -1,4 +1,5 @@",
                        " fun main() {",
                        "-    println(\"old\")",
                        "+    println(\"new\")",
                        "+    retry()",
                        " }",
                    ),
                ),
            ),
            wrap = false,
            fontSize = 13.sp,
            onBack = {},
            onRetry = {},
        )
    }
}
