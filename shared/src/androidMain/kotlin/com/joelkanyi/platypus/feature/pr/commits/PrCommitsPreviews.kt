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
package com.joelkanyi.platypus.feature.pr.commits

import androidx.compose.runtime.Composable
import com.joelkanyi.platypus.domain.model.Commit
import com.joelkanyi.platypus.preview.PlatypusPreview
import com.joelkanyi.platypus.preview.PlatypusThemePreviews

private val samplePrCommits = listOf(
    Commit("a1b2c3d4e5", "Add retry to token refresh", "Grace Njeri", "2026-08-20T09:00:00+00:00"),
    Commit("f6g7h8i9j0", "Fix crash on empty workspace", "Peter Otieno", "2026-08-19T14:00:00+00:00"),
)

@PlatypusThemePreviews
@Composable
private fun PrCommitsContentPreview() {
    PlatypusPreview {
        PrCommitsContent(
            state = PrCommitsUiState(isLoading = false, commits = samplePrCommits),
            onOpenCommit = {},
            onBack = {},
            onRetry = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun PrCommitsLoadingPreview() {
    PlatypusPreview {
        PrCommitsContent(
            state = PrCommitsUiState(isLoading = true),
            onOpenCommit = {},
            onBack = {},
            onRetry = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun PrCommitsEmptyPreview() {
    PlatypusPreview {
        PrCommitsContent(
            state = PrCommitsUiState(isLoading = false, commits = emptyList()),
            onOpenCommit = {},
            onBack = {},
            onRetry = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun PrCommitsErrorPreview() {
    PlatypusPreview {
        PrCommitsContent(
            state = PrCommitsUiState(isLoading = false, error = "Couldn't load commits"),
            onOpenCommit = {},
            onBack = {},
            onRetry = {},
        )
    }
}
