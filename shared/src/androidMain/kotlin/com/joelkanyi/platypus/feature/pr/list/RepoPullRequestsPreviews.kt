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
package com.joelkanyi.platypus.feature.pr.list

import androidx.compose.runtime.Composable
import com.joelkanyi.platypus.domain.model.PrRelationship
import com.joelkanyi.platypus.domain.model.PullRequest
import com.joelkanyi.platypus.preview.PlatypusPreview
import com.joelkanyi.platypus.preview.PlatypusThemePreviews
import kotlinx.collections.immutable.persistentListOf

private fun pr(id: Long, title: String, author: String, relationship: PrRelationship, comments: Int) = PullRequest(
    id = id,
    title = title,
    authorName = author,
    authorAvatarUrl = null,
    sourceBranch = "feature/x",
    destinationBranch = "main",
    commentCount = comments,
    updatedOn = "2026-08-21T09:00:00+00:00",
    webUrl = null,
    relationship = relationship,
    accountId = "1",
    accountLabel = "Joel Kanyi",
    workspaceSlug = "acme",
    repoSlug = "api-gateway",
    repoName = "API Gateway",
)

private val samplePullRequests = persistentListOf(
    pr(101, "Add retry to token refresh", "Grace Njeri", PrRelationship.TO_REVIEW, 3),
    pr(102, "Fix null crash on empty workspace", "Peter Otieno", PrRelationship.OTHER, 0),
    pr(103, "Bump Ktor to 3.5.1", "Joel Kanyi", PrRelationship.MINE, 5),
)

@PlatypusThemePreviews
@Composable
private fun RepoPullRequestsContentPreview() {
    PlatypusPreview {
        RepoPullRequestsContent(
            repoName = "API Gateway",
            state = RepoPullRequestsUiState(isLoading = false, pullRequests = samplePullRequests),
            onBack = {},
            onRetry = {},
            onRefresh = {},
            onOpenPullRequest = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun RepoPullRequestsEmptyPreview() {
    PlatypusPreview {
        RepoPullRequestsContent(
            repoName = "API Gateway",
            state = RepoPullRequestsUiState(isLoading = false, pullRequests = persistentListOf()),
            onBack = {},
            onRetry = {},
            onRefresh = {},
            onOpenPullRequest = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun RepoPullRequestsLoadingPreview() {
    PlatypusPreview {
        RepoPullRequestsContent(
            repoName = "API Gateway",
            state = RepoPullRequestsUiState(isLoading = true),
            onBack = {},
            onRetry = {},
            onRefresh = {},
            onOpenPullRequest = {},
        )
    }
}
