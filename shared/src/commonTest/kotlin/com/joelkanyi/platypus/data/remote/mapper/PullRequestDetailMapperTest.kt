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

import com.joelkanyi.platypus.data.remote.dto.CommentContentDto
import com.joelkanyi.platypus.data.remote.dto.CommentDto
import com.joelkanyi.platypus.data.remote.dto.CommentInlineDto
import com.joelkanyi.platypus.data.remote.dto.CommentParentDto
import com.joelkanyi.platypus.data.remote.dto.PrBranchDto
import com.joelkanyi.platypus.data.remote.dto.PrEndpointDto
import com.joelkanyi.platypus.data.remote.dto.PrParticipantDto
import com.joelkanyi.platypus.data.remote.dto.PrUserDto
import com.joelkanyi.platypus.data.remote.dto.PullRequestDto
import com.joelkanyi.platypus.domain.model.PrApproval
import com.joelkanyi.platypus.domain.model.PrState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val ME = "{me}"

class PullRequestDetailMapperTest {

    @Test
    fun mapsReviewersAndMyApprovalFromParticipants() {
        val dto = PullRequestDto(
            id = 7,
            title = "Add feature",
            description = "Body",
            state = "OPEN",
            closeSourceBranch = true,
            author = PrUserDto(uuid = "{other}", displayName = "Ada"),
            participants = listOf(
                PrParticipantDto(user = PrUserDto(uuid = ME, displayName = "Joel"), role = "REVIEWER", approved = true),
                PrParticipantDto(
                    user = PrUserDto(uuid = "{r2}", displayName = "Grace"),
                    role = "REVIEWER",
                    approved = false,
                    state = "changes_requested",
                ),
                PrParticipantDto(user = PrUserDto(uuid = "{p}", displayName = "Alan"), role = "PARTICIPANT"),
            ),
            source = PrEndpointDto(PrBranchDto("feature")),
            destination = PrEndpointDto(PrBranchDto("main")),
        )

        val detail = dto.toDetail(me = ME, accountId = "1", workspaceSlug = "acme", repoSlug = "api")

        assertEquals(PrState.OPEN, detail.state)
        assertEquals("feature", detail.sourceBranch)
        assertEquals("main", detail.destinationBranch)
        assertEquals(2, detail.reviewers.size)
        assertEquals(PrApproval.APPROVED, detail.myApproval)
        assertFalse(detail.isAuthoredByMe)
        assertTrue(detail.closeSourceBranch)
        assertEquals(PrApproval.CHANGES_REQUESTED, detail.reviewers.first { it.name == "Grace" }.approval)
    }

    @Test
    fun mapsStateAndAuthorship() {
        val dto = PullRequestDto(
            id = 8,
            state = "MERGED",
            author = PrUserDto(uuid = ME, displayName = "Joel"),
        )

        val detail = dto.toDetail(me = ME, accountId = "1", workspaceSlug = "acme", repoSlug = "api")

        assertEquals(PrState.MERGED, detail.state)
        assertTrue(detail.isAuthoredByMe)
        assertEquals(PrApproval.NONE, detail.myApproval)
    }

    @Test
    fun mapsActivityItems() {
        val approved = com.joelkanyi.platypus.data.remote.dto.ActivityDto(
            approval = com.joelkanyi.platypus.data.remote.dto.ActivityApprovalDto(
                date = "2026-08-21T08:00:00+00:00",
                user = PrUserDto(uuid = "{joel}", displayName = "Joel"),
            ),
        ).toDomain()
        assertIs<com.joelkanyi.platypus.domain.model.ActivityItem.Approved>(approved)

        val commented = com.joelkanyi.platypus.data.remote.dto.ActivityDto(
            comment = CommentDto(id = 5, content = CommentContentDto(raw = "hi"), user = PrUserDto(uuid = "{u}")),
        ).toDomain()
        assertIs<com.joelkanyi.platypus.domain.model.ActivityItem.Commented>(commented)

        val empty = com.joelkanyi.platypus.data.remote.dto.ActivityDto().toDomain()
        assertNull(empty)
    }

    @Test
    fun mapsCommentFields() {
        val general = CommentDto(
            id = 1,
            content = CommentContentDto(raw = "Looks good"),
            user = PrUserDto(uuid = "{u}", displayName = "Ada"),
        ).toDomain()
        assertEquals("Looks good", general.content)
        assertNull(general.parentId)
        assertNull(general.inlinePath)

        val reply = CommentDto(
            id = 2,
            content = CommentContentDto(raw = "Thanks"),
            parent = CommentParentDto(id = 1),
            inline = CommentInlineDto(path = "src/Main.kt", to = 12),
        ).toDomain()
        assertEquals(1L, reply.parentId)
        assertEquals("src/Main.kt", reply.inlinePath)
    }
}
