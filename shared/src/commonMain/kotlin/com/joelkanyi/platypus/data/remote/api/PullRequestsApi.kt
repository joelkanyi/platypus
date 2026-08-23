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
package com.joelkanyi.platypus.data.remote.api

import com.joelkanyi.platypus.data.remote.BITBUCKET_API_BASE
import com.joelkanyi.platypus.data.remote.dto.ActivityDto
import com.joelkanyi.platypus.data.remote.dto.CommentContentDto
import com.joelkanyi.platypus.data.remote.dto.CommentDto
import com.joelkanyi.platypus.data.remote.dto.CommentInlineDto
import com.joelkanyi.platypus.data.remote.dto.CommentParentDto
import com.joelkanyi.platypus.data.remote.dto.CommentRequestDto
import com.joelkanyi.platypus.data.remote.dto.CommitDto
import com.joelkanyi.platypus.data.remote.dto.DiffStatEntryDto
import com.joelkanyi.platypus.data.remote.dto.MergeRequestDto
import com.joelkanyi.platypus.data.remote.dto.PageDto
import com.joelkanyi.platypus.data.remote.dto.PullRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType

class PullRequestsApi(private val client: HttpClient) {

    suspend fun open(workspaceSlug: String, repoSlug: String): PageDto<PullRequestDto> =
        client.get("$BITBUCKET_API_BASE/repositories/$workspaceSlug/$repoSlug/pullrequests") {
            parameter("state", "OPEN")
            parameter("pagelen", PAGE_LEN)
            parameter("sort", "-updated_on")
        }.body()

    suspend fun page(url: String): PageDto<PullRequestDto> = client.get(url).body()

    suspend fun detail(workspaceSlug: String, repoSlug: String, id: Long): PullRequestDto =
        client.get(prUrl(workspaceSlug, repoSlug, id)).body()

    suspend fun comments(workspaceSlug: String, repoSlug: String, id: Long): PageDto<CommentDto> =
        client.get("${prUrl(workspaceSlug, repoSlug, id)}/comments") {
            parameter("pagelen", PAGE_LEN)
            parameter("sort", "created_on")
        }.body()

    suspend fun commentsPage(url: String): PageDto<CommentDto> = client.get(url).body()

    suspend fun addComment(
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
        raw: String,
        parentId: Long?,
        inlinePath: String? = null,
        inlineTo: Int? = null,
    ): CommentDto = client.post("${prUrl(workspaceSlug, repoSlug, id)}/comments") {
        contentType(ContentType.Application.Json)
        setBody(
            CommentRequestDto(
                content = CommentContentDto(raw = raw),
                parent = parentId?.let { CommentParentDto(it) },
                inline = inlinePath?.let { CommentInlineDto(path = it, to = inlineTo) },
            ),
        )
    }.body()

    suspend fun activity(workspaceSlug: String, repoSlug: String, id: Long): PageDto<ActivityDto> =
        client.get("${prUrl(workspaceSlug, repoSlug, id)}/activity") { parameter("pagelen", PAGE_LEN) }.body()

    suspend fun activityPage(url: String): PageDto<ActivityDto> = client.get(url).body()

    suspend fun commits(workspaceSlug: String, repoSlug: String, id: Long): PageDto<CommitDto> =
        client.get("${prUrl(workspaceSlug, repoSlug, id)}/commits") { parameter("pagelen", PAGE_LEN) }.body()

    suspend fun commitsPage(url: String): PageDto<CommitDto> = client.get(url).body()

    suspend fun resolveComment(workspaceSlug: String, repoSlug: String, id: Long, commentId: Long) {
        client.post("${prUrl(workspaceSlug, repoSlug, id)}/comments/$commentId/resolve")
    }

    suspend fun unresolveComment(workspaceSlug: String, repoSlug: String, id: Long, commentId: Long) {
        client.delete("${prUrl(workspaceSlug, repoSlug, id)}/comments/$commentId/resolve")
    }

    suspend fun approve(workspaceSlug: String, repoSlug: String, id: Long) {
        client.post("${prUrl(workspaceSlug, repoSlug, id)}/approve")
    }

    suspend fun unapprove(workspaceSlug: String, repoSlug: String, id: Long) {
        client.delete("${prUrl(workspaceSlug, repoSlug, id)}/approve")
    }

    suspend fun requestChanges(workspaceSlug: String, repoSlug: String, id: Long) {
        client.post("${prUrl(workspaceSlug, repoSlug, id)}/request-changes")
    }

    suspend fun unrequestChanges(workspaceSlug: String, repoSlug: String, id: Long) {
        client.delete("${prUrl(workspaceSlug, repoSlug, id)}/request-changes")
    }

    suspend fun merge(
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
        strategy: String,
        message: String?,
        closeSourceBranch: Boolean,
    ): PullRequestDto = client.post("${prUrl(workspaceSlug, repoSlug, id)}/merge") {
        contentType(ContentType.Application.Json)
        setBody(MergeRequestDto(mergeStrategy = strategy, message = message, closeSourceBranch = closeSourceBranch))
    }.body()

    suspend fun decline(workspaceSlug: String, repoSlug: String, id: Long): PullRequestDto =
        client.post("${prUrl(workspaceSlug, repoSlug, id)}/decline").body()

    suspend fun diff(workspaceSlug: String, repoSlug: String, id: Long): String =
        client.get("${prUrl(workspaceSlug, repoSlug, id)}/diff").bodyAsText()

    suspend fun diffstat(workspaceSlug: String, repoSlug: String, spec: String): PageDto<DiffStatEntryDto> =
        client.get("$BITBUCKET_API_BASE/repositories/$workspaceSlug/$repoSlug/diffstat/$spec") {
            parameter("pagelen", PAGE_LEN)
        }.body()

    suspend fun diffstatPage(url: String): PageDto<DiffStatEntryDto> = client.get(url).body()

    private fun prUrl(workspaceSlug: String, repoSlug: String, id: Long): String =
        "$BITBUCKET_API_BASE/repositories/$workspaceSlug/$repoSlug/pullrequests/$id"

    private companion object {
        const val PAGE_LEN = 50
    }
}
