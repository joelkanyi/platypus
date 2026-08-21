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

import com.joelkanyi.platypus.data.remote.dto.LinkDto
import com.joelkanyi.platypus.data.remote.dto.LinksDto
import com.joelkanyi.platypus.data.remote.dto.PrBranchDto
import com.joelkanyi.platypus.data.remote.dto.PrEndpointDto
import com.joelkanyi.platypus.data.remote.dto.PrParticipantDto
import com.joelkanyi.platypus.data.remote.dto.PrUserDto
import com.joelkanyi.platypus.data.remote.dto.PullRequestDto
import com.joelkanyi.platypus.domain.model.PrRelationship
import com.joelkanyi.platypus.domain.model.WatchedRepo
import kotlin.test.Test
import kotlin.test.assertEquals

private const val ME = "{me}"

private val watched = WatchedRepo(
    accountId = "1",
    workspaceSlug = "acme",
    repoSlug = "api-gateway",
    repoUuid = "{r}",
    name = "API Gateway",
    fullName = "acme/api-gateway",
    avatarUrl = null,
)

private fun dto(id: Long = 1, author: String = "{other}", participants: List<PrParticipantDto> = emptyList()) =
    PullRequestDto(
        id = id,
        title = "Title",
        author = PrUserDto(uuid = author, displayName = "Author Name"),
        participants = participants,
        source = PrEndpointDto(PrBranchDto("feature/x")),
        destination = PrEndpointDto(PrBranchDto("main")),
        commentCount = 4,
        updatedOn = "2026-08-21T09:00:00+00:00",
        links = LinksDto(html = LinkDto("https://bitbucket.org/acme/api-gateway/pull-requests/1")),
    )

class PullRequestMapperTest {

    @Test
    fun tagsToReviewWhenMeIsUnapprovedReviewer() {
        val pr = dto(
            participants = listOf(PrParticipantDto(PrUserDto(uuid = ME), role = "REVIEWER", approved = false)),
        ).toDomain(ME, watched, "Joel")

        assertEquals(PrRelationship.TO_REVIEW, pr.relationship)
    }

    @Test
    fun tagsMineWhenMeIsAuthor() {
        val pr = dto(author = ME).toDomain(ME, watched, "Joel")

        assertEquals(PrRelationship.MINE, pr.relationship)
    }

    @Test
    fun tagsOtherWhenReviewerAlreadyApproved() {
        val pr = dto(
            participants = listOf(PrParticipantDto(PrUserDto(uuid = ME), role = "REVIEWER", approved = true)),
        ).toDomain(ME, watched, "Joel")

        assertEquals(PrRelationship.OTHER, pr.relationship)
    }

    @Test
    fun mapsFieldsAndOrigin() {
        val pr = dto(id = 7).toDomain(ME, watched, "Joel Kanyi")

        assertEquals(7, pr.id)
        assertEquals("feature/x", pr.sourceBranch)
        assertEquals("main", pr.destinationBranch)
        assertEquals(4, pr.commentCount)
        assertEquals("https://bitbucket.org/acme/api-gateway/pull-requests/1", pr.webUrl)
        assertEquals("API Gateway", pr.repoName)
        assertEquals("Joel Kanyi", pr.accountLabel)
    }
}
