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
package com.joelkanyi.platypus.data.repository

import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.safeApiCall
import com.joelkanyi.platypus.data.remote.api.PullRequestsApi
import com.joelkanyi.platypus.data.remote.ktorErrorMapper
import com.joelkanyi.platypus.data.remote.mapper.parsePrDiff
import com.joelkanyi.platypus.data.remote.mapper.toDetail
import com.joelkanyi.platypus.data.remote.mapper.toDomain
import com.joelkanyi.platypus.domain.model.ActivityItem
import com.joelkanyi.platypus.domain.model.Commit
import com.joelkanyi.platypus.domain.model.MergeStrategy
import com.joelkanyi.platypus.domain.model.PrComment
import com.joelkanyi.platypus.domain.model.PrDiff
import com.joelkanyi.platypus.domain.model.PullRequestDetail
import com.joelkanyi.platypus.domain.repository.AuthRepository
import com.joelkanyi.platypus.domain.repository.PullRequestRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultPullRequestRepository(private val authRepository: AuthRepository) : PullRequestRepository {

    private val diffCache = mutableMapOf<String, PrDiff>()

    override suspend fun detail(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<PullRequestDetail> = withClient(accountId) { client ->
        client.api().detail(workspaceSlug, repoSlug, id).toDetail(me(accountId), accountId, workspaceSlug, repoSlug)
    }

    override suspend fun comments(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<List<PrComment>> = withClient(accountId) { client ->
        val api = client.api()
        val out = mutableListOf<PrComment>()
        var page = api.comments(workspaceSlug, repoSlug, id)
        var guard = 0
        while (true) {
            out += page.values.map { it.toDomain() }
            val next = page.next
            if (next == null || ++guard >= MAX_PAGES) break
            page = api.commentsPage(next)
        }
        out.filterNot { it.deleted }
    }

    override suspend fun addComment(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
        raw: String,
        parentId: Long?,
        inlinePath: String?,
        inlineTo: Int?,
    ): NetworkResult<PrComment> = withClient(accountId) { client ->
        client.api().addComment(workspaceSlug, repoSlug, id, raw, parentId, inlinePath, inlineTo).toDomain()
    }

    override suspend fun activity(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<List<ActivityItem>> = withClient(accountId) { client ->
        val api = client.api()
        val out = mutableListOf<ActivityItem>()
        var page = api.activity(workspaceSlug, repoSlug, id)
        var guard = 0
        while (true) {
            out += page.values.mapNotNull { it.toDomain() }
            val next = page.next
            if (next == null || ++guard >= MAX_PAGES) break
            page = api.activityPage(next)
        }
        out
    }

    override suspend fun commits(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<List<Commit>> = withClient(accountId) { client ->
        val api = client.api()
        val out = mutableListOf<Commit>()
        var page = api.commits(workspaceSlug, repoSlug, id)
        var guard = 0
        while (true) {
            out += page.values.map { it.toDomain() }
            val next = page.next
            if (next == null || ++guard >= MAX_PAGES) break
            page = api.commitsPage(next)
        }
        out
    }

    override suspend fun resolveComment(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
        commentId: Long,
        resolve: Boolean,
    ): NetworkResult<Unit> = withClient(accountId) { client ->
        if (resolve) {
            client.api().resolveComment(workspaceSlug, repoSlug, id, commentId)
        } else {
            client.api().unresolveComment(workspaceSlug, repoSlug, id, commentId)
        }
    }

    override suspend fun approve(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<Unit> = withClient(accountId) { it.api().approve(workspaceSlug, repoSlug, id) }

    override suspend fun unapprove(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<Unit> = withClient(accountId) { it.api().unapprove(workspaceSlug, repoSlug, id) }

    override suspend fun requestChanges(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<Unit> = withClient(accountId) { it.api().requestChanges(workspaceSlug, repoSlug, id) }

    override suspend fun unrequestChanges(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<Unit> = withClient(accountId) { it.api().unrequestChanges(workspaceSlug, repoSlug, id) }

    override suspend fun merge(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
        strategy: MergeStrategy,
        message: String?,
        closeSourceBranch: Boolean,
    ): NetworkResult<PullRequestDetail> = withClient(accountId) { client ->
        client.api()
            .merge(workspaceSlug, repoSlug, id, strategy.wire, message, closeSourceBranch)
            .toDetail(me(accountId), accountId, workspaceSlug, repoSlug)
    }

    override suspend fun decline(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<PullRequestDetail> = withClient(accountId) { client ->
        client.api().decline(workspaceSlug, repoSlug, id)
            .toDetail(me(accountId), accountId, workspaceSlug, repoSlug)
    }

    override suspend fun diff(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<PrDiff> {
        val key = "$accountId/$workspaceSlug/$repoSlug/$id"
        diffCache[key]?.let { return NetworkResult.Success(it) }
        return withClient(accountId) { client ->
            parsePrDiff(client.api().diff(workspaceSlug, repoSlug, id)).also { diffCache[key] = it }
        }
    }

    override suspend fun hasConflicts(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        sourceCommit: String,
        destinationCommit: String,
    ): NetworkResult<Boolean> {
        if (sourceCommit.isBlank() || destinationCommit.isBlank()) return NetworkResult.Success(false)
        return withClient(accountId) { client ->
            val api = client.api()
            val spec = "$sourceCommit..$destinationCommit"
            var page = api.diffstat(workspaceSlug, repoSlug, spec)
            var guard = 0
            var conflict = page.values.any { it.status == MERGE_CONFLICT_STATUS }
            while (!conflict) {
                val next = page.next
                if (next == null || ++guard >= MAX_PAGES) break
                page = api.diffstatPage(next)
                conflict = page.values.any { it.status == MERGE_CONFLICT_STATUS }
            }
            conflict
        }
    }

    private suspend inline fun <T> withClient(
        accountId: String,
        crossinline block: suspend (HttpClient) -> T,
    ): NetworkResult<T> {
        val client = authRepository.authenticatedClient(accountId)
            ?: return NetworkResult.Failure.Http(401, SIGNED_OUT)
        return safeApiCall(::ktorErrorMapper) { block(client) }
    }

    private fun HttpClient.api(): PullRequestsApi = PullRequestsApi(this)

    private fun me(accountId: String): String =
        authRepository.accounts.value.firstOrNull { it.id == accountId }?.user?.uuid.orEmpty()

    private companion object {
        const val SIGNED_OUT = "This account is signed out."
        const val MAX_PAGES = 10
        const val MERGE_CONFLICT_STATUS = "merge conflict"
    }
}
