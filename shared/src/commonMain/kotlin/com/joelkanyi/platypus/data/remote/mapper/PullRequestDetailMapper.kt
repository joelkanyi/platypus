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
package com.joelkanyi.platypus.data.remote.mapper

import com.joelkanyi.platypus.data.remote.dto.ActivityDto
import com.joelkanyi.platypus.data.remote.dto.CommentDto
import com.joelkanyi.platypus.data.remote.dto.PrParticipantDto
import com.joelkanyi.platypus.data.remote.dto.PrUserDto
import com.joelkanyi.platypus.data.remote.dto.PullRequestDto
import com.joelkanyi.platypus.domain.model.ActivityItem
import com.joelkanyi.platypus.domain.model.PrApproval
import com.joelkanyi.platypus.domain.model.PrComment
import com.joelkanyi.platypus.domain.model.PrReviewer
import com.joelkanyi.platypus.domain.model.PrState
import com.joelkanyi.platypus.domain.model.PullRequestDetail

fun PullRequestDto.toDetail(
    me: String,
    accountId: String,
    workspaceSlug: String,
    repoSlug: String,
): PullRequestDetail {
    val defaultUuids = reviewers.map { it.uuid }.filter { it.isNotBlank() }.toSet()
    val reviewerParticipants = participants.filter { it.role == "REVIEWER" && it.user != null }
    val reviewerModels = reviewerParticipants.map { it.toReviewer(defaultUuids) }
    val myApproval = participants.firstOrNull { it.user?.uuid == me }?.approval() ?: PrApproval.NONE
    return PullRequestDetail(
        id = id,
        title = title,
        description = description,
        state = state.toPrState(),
        authorName = author.name(),
        authorAvatarUrl = author?.links?.avatar?.href,
        authorUuid = author?.uuid.orEmpty(),
        sourceBranch = source?.branch?.name.orEmpty(),
        destinationBranch = destination?.branch?.name.orEmpty(),
        sourceCommit = source?.commit?.hash.orEmpty(),
        destinationCommit = destination?.commit?.hash.orEmpty(),
        commentCount = commentCount,
        updatedOn = updatedOn,
        webUrl = links?.html?.href,
        closeSourceBranch = closeSourceBranch,
        reviewers = reviewerModels,
        myApproval = myApproval,
        isAuthoredByMe = author?.uuid == me,
        accountId = accountId,
        workspaceSlug = workspaceSlug,
        repoSlug = repoSlug,
    )
}

fun CommentDto.toDomain(): PrComment = PrComment(
    id = id,
    authorName = user.name(),
    authorAvatarUrl = user?.links?.avatar?.href,
    content = content?.raw.orEmpty(),
    createdOn = createdOn,
    parentId = parent?.id,
    inlinePath = inline?.path?.takeIf { it.isNotBlank() },
    inlineTo = inline?.to,
    deleted = deleted,
    resolved = resolution != null,
)

fun ActivityDto.toDomain(): ActivityItem? = when {
    approval != null -> ActivityItem.Approved(approval.user.name(), approval.user?.links?.avatar?.href, approval.date)
    changesRequested != null -> ActivityItem.ChangesRequested(
        changesRequested.user.name(),
        changesRequested.user?.links?.avatar?.href,
        changesRequested.date,
    )
    comment != null && !comment.deleted -> ActivityItem.Commented(comment.toDomain())
    update != null -> ActivityItem.Updated(update.author.name(), update.date, update.title)
    else -> null
}

private fun PrParticipantDto.toReviewer(defaultUuids: Set<String>): PrReviewer = PrReviewer(
    uuid = user?.uuid.orEmpty(),
    name = user.name(),
    avatarUrl = user?.links?.avatar?.href,
    approval = approval(),
    isDefault = user?.uuid in defaultUuids,
)

private fun PrParticipantDto.approval(): PrApproval = when {
    approved -> PrApproval.APPROVED
    state == "changes_requested" -> PrApproval.CHANGES_REQUESTED
    else -> PrApproval.NONE
}

private fun PrUserDto?.name(): String = this?.displayName?.takeIf { it.isNotBlank() } ?: this?.nickname.orEmpty()

private fun String.toPrState(): PrState = when (uppercase()) {
    "OPEN" -> PrState.OPEN
    "MERGED" -> PrState.MERGED
    "DECLINED" -> PrState.DECLINED
    "SUPERSEDED" -> PrState.SUPERSEDED
    else -> PrState.OTHER
}
