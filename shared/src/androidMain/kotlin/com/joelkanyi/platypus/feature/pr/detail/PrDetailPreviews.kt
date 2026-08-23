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
package com.joelkanyi.platypus.feature.pr.detail

import androidx.compose.runtime.Composable
import com.joelkanyi.platypus.domain.model.ActivityItem
import com.joelkanyi.platypus.domain.model.PrApproval
import com.joelkanyi.platypus.domain.model.PrComment
import com.joelkanyi.platypus.domain.model.PrReviewer
import com.joelkanyi.platypus.domain.model.PrState
import com.joelkanyi.platypus.domain.model.PullRequestDetail
import com.joelkanyi.platypus.preview.PlatypusPreview
import com.joelkanyi.platypus.preview.PlatypusThemePreviews

private fun detail(state: PrState = PrState.OPEN, myApproval: PrApproval = PrApproval.NONE) = PullRequestDetail(
    id = 142,
    title = "Add retry to token refresh",
    description = "Retries the refresh once on a transient 5xx before surfacing an error.",
    state = state,
    authorName = "Grace Njeri",
    authorAvatarUrl = null,
    authorUuid = "{grace}",
    sourceBranch = "feature/token-retry",
    destinationBranch = "main",
    sourceCommit = "a1b2c3d",
    destinationCommit = "d4e5f6a",
    commentCount = 2,
    updatedOn = "2026-08-21T09:00:00+00:00",
    webUrl = "https://bitbucket.org/acme/api/pull-requests/142",
    closeSourceBranch = true,
    reviewers = listOf(
        PrReviewer("{joel}", "Joel Kanyi", null, PrApproval.APPROVED),
        PrReviewer("{peter}", "Peter Otieno", null, PrApproval.CHANGES_REQUESTED),
        PrReviewer("{asha}", "Asha Mwangi", null, PrApproval.NONE),
    ),
    myApproval = myApproval,
    isAuthoredByMe = false,
    accountId = "1",
    workspaceSlug = "acme",
    repoSlug = "api",
)

private fun sampleComment(id: Long, author: String, body: String, date: String, parentId: Long? = null) = PrComment(
    id = id,
    authorName = author,
    authorAvatarUrl = null,
    content = body,
    createdOn = date,
    parentId = parentId,
    inlinePath = null,
    inlineTo = null,
    deleted = false,
    resolved = false,
)

private val sampleActivity = listOf(
    ActivityItem.Approved("Joel Kanyi", null, "2026-08-21T08:00:00+00:00"),
    ActivityItem.Commented(sampleComment(1, "Peter Otieno", "Can we cap the backoff?", "2026-08-21T09:10:00+00:00")),
    ActivityItem.Commented(sampleComment(2, "Grace Njeri", "Done, capped at 2s.", "2026-08-21T09:20:00+00:00", 1)),
)

@PlatypusThemePreviews
@Composable
private fun PrDetailOpenPreview() {
    PlatypusPreview {
        PrDetailContent(
            repoName = "API Gateway",
            prId = 142,
            state = PrDetailUiState(isLoading = false, detail = detail(), activity = sampleActivity),
            onEvent = {},
            onBack = {},
            onOpenFiles = {},
            onOpenCommits = {},
            onOpenUrl = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun PrDetailApprovedPreview() {
    PlatypusPreview {
        PrDetailContent(
            repoName = "API Gateway",
            prId = 142,
            state = PrDetailUiState(
                isLoading = false,
                detail = detail(myApproval = PrApproval.APPROVED),
                activity = sampleActivity,
            ),
            onEvent = {},
            onBack = {},
            onOpenFiles = {},
            onOpenCommits = {},
            onOpenUrl = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun PrDetailMergedPreview() {
    PlatypusPreview {
        PrDetailContent(
            repoName = "API Gateway",
            prId = 142,
            state = PrDetailUiState(
                isLoading = false,
                detail = detail(state = PrState.MERGED),
                activity = sampleActivity,
            ),
            onEvent = {},
            onBack = {},
            onOpenFiles = {},
            onOpenCommits = {},
            onOpenUrl = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun PrDetailErrorPreview() {
    PlatypusPreview {
        PrDetailContent(
            repoName = "API Gateway",
            prId = 142,
            state = PrDetailUiState(isLoading = false, error = "Couldn't reach Bitbucket."),
            onEvent = {},
            onBack = {},
            onOpenFiles = {},
            onOpenCommits = {},
            onOpenUrl = {},
        )
    }
}
