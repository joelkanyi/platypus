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
package com.joelkanyi.platypus.feature.repo.overview

import androidx.compose.runtime.Composable
import com.joelkanyi.platypus.domain.model.RepositoryDetail
import com.joelkanyi.platypus.preview.PlatypusPreview
import com.joelkanyi.platypus.preview.PlatypusThemePreviews

private val sampleDetail = RepositoryDetail(
    name = "API Gateway",
    fullName = "acme/api-gateway",
    description = "Edge routing and auth for the platform.",
    language = "Kotlin",
    size = 4_812_345,
    updatedOn = "2026-08-19T10:00:00+00:00",
    isPrivate = true,
    defaultBranch = "main",
    avatarUrl = null,
    webUrl = "https://bitbucket.org/acme/api-gateway",
)

@PlatypusThemePreviews
@Composable
private fun OverviewContentPreview() {
    PlatypusPreview {
        OverviewContent(
            repoName = "API Gateway",
            onBack = {},
            state = OverviewUiState(
                isLoading = false,
                detail = sampleDetail,
                readme = "# API Gateway\n\nEdge service.",
            ),
            onRetry = {},
            onOpenFiles = {},
            onOpenCommits = {},
            onOpenPullRequests = {},
            onBranchClick = {},
            onOpenUrl = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun OverviewErrorPreview() {
    PlatypusPreview {
        OverviewContent(
            repoName = "API Gateway",
            onBack = {},
            state = OverviewUiState(isLoading = false, error = "You are offline."),
            onRetry = {},
            onOpenFiles = {},
            onOpenCommits = {},
            onOpenPullRequests = {},
            onBranchClick = {},
            onOpenUrl = {},
        )
    }
}
