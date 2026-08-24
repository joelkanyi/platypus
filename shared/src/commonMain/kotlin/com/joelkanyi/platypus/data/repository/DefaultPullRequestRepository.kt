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
import com.joelkanyi.platypus.data.remote.mapper.parsePrDiff
import com.joelkanyi.platypus.data.remote.mapper.toDetail
import com.joelkanyi.platypus.data.remote.mapper.toDomain
import com.joelkanyi.platypus.data.remote.network.collectPaged
import com.joelkanyi.platypus.data.remote.network.ktorErrorMapper
import com.joelkanyi.platypus.domain.model.ActivityItem
import com.joelkanyi.platypus.domain.model.Commit
import com.joelkanyi.platypus.domain.model.MergePair
import com.joelkanyi.platypus.domain.model.MergeStrategy
import com.joelkanyi.platypus.domain.model.PrComment
import com.joelkanyi.platypus.domain.model.PrDiff
import com.joelkanyi.platypus.domain.model.PrRef
import com.joelkanyi.platypus.domain.model.PullRequest
import com.joelkanyi.platypus.domain.model.PullRequestDetail
import com.joelkanyi.platypus.domain.model.RepoRef
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

    override fun clearCache() {
        diffCache.clear()
    }

    override suspend fun pullRequests(repo: RepoRef, repoName: String): NetworkResult<List<PullRequest>> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        return withClient(accountId) { client ->
            val api = client.api()
            val me = me(accountId)
            val label = accountLabel(accountId)
            collectPaged(
                firstPage = { api.open(workspaceSlug, repoSlug) },
                nextPage = { api.page(it) },
            ).map { it.toDomain(me, accountId, workspaceSlug, repoSlug, repoName, label) }
        }
    }

    override suspend fun detail(pr: PrRef): NetworkResult<PullRequestDetail> {
        val accountId = pr.repo.accountId.value
        val workspaceSlug = pr.repo.workspace.value
        val repoSlug = pr.repo.repoSlug.value
        val id = pr.id.value
        return withClient(accountId) { client ->
            client.api().detail(workspaceSlug, repoSlug, id).toDetail(me(accountId), accountId, workspaceSlug, repoSlug)
        }
    }

    override suspend fun comments(pr: PrRef): NetworkResult<List<PrComment>> {
        val workspaceSlug = pr.repo.workspace.value
        val repoSlug = pr.repo.repoSlug.value
        val id = pr.id.value
        return withClient(pr.repo.accountId.value) { client ->
            val api = client.api()
            collectPaged(
                firstPage = { api.comments(workspaceSlug, repoSlug, id) },
                nextPage = { api.commentsPage(it) },
            ).map { it.toDomain() }.filterNot { it.deleted }
        }
    }

    override suspend fun addComment(
        pr: PrRef,
        raw: String,
        parentId: Long?,
        inlinePath: String?,
        inlineTo: Int?,
    ): NetworkResult<PrComment> {
        val workspaceSlug = pr.repo.workspace.value
        val repoSlug = pr.repo.repoSlug.value
        val id = pr.id.value
        return withClient(pr.repo.accountId.value) { client ->
            client.api().addComment(workspaceSlug, repoSlug, id, raw, parentId, inlinePath, inlineTo).toDomain()
        }
    }

    override suspend fun activity(pr: PrRef): NetworkResult<List<ActivityItem>> {
        val workspaceSlug = pr.repo.workspace.value
        val repoSlug = pr.repo.repoSlug.value
        val id = pr.id.value
        return withClient(pr.repo.accountId.value) { client ->
            val api = client.api()
            collectPaged(
                firstPage = { api.activity(workspaceSlug, repoSlug, id) },
                nextPage = { api.activityPage(it) },
            ).mapNotNull { it.toDomain() }
        }
    }

    override suspend fun commits(pr: PrRef): NetworkResult<List<Commit>> {
        val workspaceSlug = pr.repo.workspace.value
        val repoSlug = pr.repo.repoSlug.value
        val id = pr.id.value
        return withClient(pr.repo.accountId.value) { client ->
            val api = client.api()
            collectPaged(
                firstPage = { api.commits(workspaceSlug, repoSlug, id) },
                nextPage = { api.commitsPage(it) },
            ).map { it.toDomain() }
        }
    }

    override suspend fun resolveComment(pr: PrRef, commentId: Long, resolve: Boolean): NetworkResult<Unit> {
        val workspaceSlug = pr.repo.workspace.value
        val repoSlug = pr.repo.repoSlug.value
        val id = pr.id.value
        return withClient(pr.repo.accountId.value) { client ->
            if (resolve) {
                client.api().resolveComment(workspaceSlug, repoSlug, id, commentId)
            } else {
                client.api().unresolveComment(workspaceSlug, repoSlug, id, commentId)
            }
        }
    }

    override suspend fun approve(pr: PrRef): NetworkResult<Unit> {
        val workspaceSlug = pr.repo.workspace.value
        val repoSlug = pr.repo.repoSlug.value
        val id = pr.id.value
        return withClient(pr.repo.accountId.value) { it.api().approve(workspaceSlug, repoSlug, id) }
    }

    override suspend fun unapprove(pr: PrRef): NetworkResult<Unit> {
        val workspaceSlug = pr.repo.workspace.value
        val repoSlug = pr.repo.repoSlug.value
        val id = pr.id.value
        return withClient(pr.repo.accountId.value) { it.api().unapprove(workspaceSlug, repoSlug, id) }
    }

    override suspend fun requestChanges(pr: PrRef): NetworkResult<Unit> {
        val workspaceSlug = pr.repo.workspace.value
        val repoSlug = pr.repo.repoSlug.value
        val id = pr.id.value
        return withClient(pr.repo.accountId.value) { it.api().requestChanges(workspaceSlug, repoSlug, id) }
    }

    override suspend fun unrequestChanges(pr: PrRef): NetworkResult<Unit> {
        val workspaceSlug = pr.repo.workspace.value
        val repoSlug = pr.repo.repoSlug.value
        val id = pr.id.value
        return withClient(pr.repo.accountId.value) { it.api().unrequestChanges(workspaceSlug, repoSlug, id) }
    }

    override suspend fun merge(
        pr: PrRef,
        strategy: MergeStrategy,
        message: String?,
        closeSourceBranch: Boolean,
    ): NetworkResult<PullRequestDetail> {
        val accountId = pr.repo.accountId.value
        val workspaceSlug = pr.repo.workspace.value
        val repoSlug = pr.repo.repoSlug.value
        val id = pr.id.value
        return withClient(accountId) { client ->
            client.api()
                .merge(workspaceSlug, repoSlug, id, strategy.wire, message, closeSourceBranch)
                .toDetail(me(accountId), accountId, workspaceSlug, repoSlug)
        }
    }

    override suspend fun decline(pr: PrRef): NetworkResult<PullRequestDetail> {
        val accountId = pr.repo.accountId.value
        val workspaceSlug = pr.repo.workspace.value
        val repoSlug = pr.repo.repoSlug.value
        val id = pr.id.value
        return withClient(accountId) { client ->
            client.api().decline(workspaceSlug, repoSlug, id)
                .toDetail(me(accountId), accountId, workspaceSlug, repoSlug)
        }
    }

    override suspend fun diff(pr: PrRef): NetworkResult<PrDiff> {
        val accountId = pr.repo.accountId.value
        val workspaceSlug = pr.repo.workspace.value
        val repoSlug = pr.repo.repoSlug.value
        val id = pr.id.value
        val key = "$accountId/$workspaceSlug/$repoSlug/$id"
        diffCache[key]?.let { return NetworkResult.Success(it) }
        return withClient(accountId) { client ->
            parsePrDiff(client.api().diff(workspaceSlug, repoSlug, id)).also { diffCache[key] = it }
        }
    }

    override suspend fun hasConflicts(repo: RepoRef, pair: MergePair): NetworkResult<Boolean> {
        val sourceCommit = pair.source.value
        val destinationCommit = pair.destination.value
        if (sourceCommit.isBlank() || destinationCommit.isBlank()) return NetworkResult.Success(false)
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        return withClient(repo.accountId.value) { client ->
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

    private fun accountLabel(accountId: String): String =
        authRepository.accounts.value.firstOrNull { it.id == accountId }?.user?.displayName.orEmpty()

    private companion object {
        const val SIGNED_OUT = "This account is signed out."
        const val MAX_PAGES = 10
        const val MERGE_CONFLICT_STATUS = "merge conflict"
    }
}
