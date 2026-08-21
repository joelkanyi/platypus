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

import com.joelkanyi.platypus.data.remote.dto.PullRequestDto
import com.joelkanyi.platypus.domain.model.PrRelationship
import com.joelkanyi.platypus.domain.model.PullRequest
import com.joelkanyi.platypus.domain.model.WatchedRepo

fun PullRequestDto.toDomain(me: String, source: WatchedRepo, accountLabel: String): PullRequest {
    val relationship = when {
        participants.any { it.role == "REVIEWER" && it.user?.uuid == me && !it.approved } -> PrRelationship.TO_REVIEW
        author?.uuid == me -> PrRelationship.MINE
        else -> PrRelationship.OTHER
    }
    return PullRequest(
        id = id,
        title = title,
        authorName = author?.displayName?.takeIf { it.isNotBlank() } ?: author?.nickname.orEmpty(),
        authorAvatarUrl = author?.links?.avatar?.href,
        sourceBranch = this.source?.branch?.name.orEmpty(),
        destinationBranch = destination?.branch?.name.orEmpty(),
        commentCount = commentCount,
        updatedOn = updatedOn,
        webUrl = links?.html?.href,
        relationship = relationship,
        accountId = source.accountId,
        accountLabel = accountLabel,
        workspaceSlug = source.workspaceSlug,
        repoSlug = source.repoSlug,
        repoName = source.name,
    )
}
