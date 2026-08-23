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
package com.joelkanyi.platypus.domain.model

import kotlinx.serialization.Serializable

enum class PrState { OPEN, MERGED, DECLINED, SUPERSEDED, OTHER }

enum class PrApproval { APPROVED, CHANGES_REQUESTED, NONE }

@Serializable
enum class MergeStrategy(val wire: String) {
    MERGE_COMMIT("merge_commit"),
    SQUASH("squash"),
    FAST_FORWARD("fast_forward"),
}

data class PrReviewer(
    val uuid: String,
    val name: String,
    val avatarUrl: String?,
    val approval: PrApproval,
    val isDefault: Boolean = false,
)

data class PullRequestDetail(
    val id: Long,
    val title: String,
    val description: String,
    val state: PrState,
    val authorName: String,
    val authorAvatarUrl: String?,
    val authorUuid: String,
    val sourceBranch: String,
    val destinationBranch: String,
    val sourceCommit: String,
    val destinationCommit: String,
    val commentCount: Int,
    val updatedOn: String,
    val webUrl: String?,
    val closeSourceBranch: Boolean,
    val reviewers: List<PrReviewer>,
    val myApproval: PrApproval,
    val isAuthoredByMe: Boolean,
    val accountId: String,
    val workspaceSlug: String,
    val repoSlug: String,
) {
    val isOpen: Boolean get() = state == PrState.OPEN

    val hasApproval: Boolean get() = reviewers.any { it.approval == PrApproval.APPROVED }

    val hasChangesRequested: Boolean get() = reviewers.any { it.approval == PrApproval.CHANGES_REQUESTED }

    val isReadyToMerge: Boolean get() = isOpen && hasApproval && !hasChangesRequested

    val jiraKey: String? get() = JIRA_KEY.find(title)?.value
}

private val JIRA_KEY = Regex("[A-Z][A-Z0-9]+-\\d+")

data class PrComment(
    val id: Long,
    val authorName: String,
    val authorAvatarUrl: String?,
    val content: String,
    val createdOn: String,
    val parentId: Long?,
    val inlinePath: String?,
    val inlineTo: Int?,
    val deleted: Boolean,
    val resolved: Boolean,
)

sealed interface ActivityItem {
    val date: String

    data class Approved(val actorName: String, val actorAvatarUrl: String?, override val date: String) : ActivityItem

    data class ChangesRequested(val actorName: String, val actorAvatarUrl: String?, override val date: String) :
        ActivityItem

    data class Updated(val actorName: String, override val date: String, val title: String) : ActivityItem

    data class Commented(val comment: PrComment) : ActivityItem {
        override val date: String get() = comment.createdOn
    }
}
