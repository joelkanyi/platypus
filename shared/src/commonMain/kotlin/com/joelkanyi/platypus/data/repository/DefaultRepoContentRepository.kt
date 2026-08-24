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
import com.joelkanyi.platypus.data.remote.api.RepoContentApi
import com.joelkanyi.platypus.data.remote.mapper.toDomain
import com.joelkanyi.platypus.data.remote.network.collectPaged
import com.joelkanyi.platypus.data.remote.network.ktorErrorMapper
import com.joelkanyi.platypus.domain.model.Branch
import com.joelkanyi.platypus.domain.model.CommitDetail
import com.joelkanyi.platypus.domain.model.CommitPage
import com.joelkanyi.platypus.domain.model.DirectoryListing
import com.joelkanyi.platypus.domain.model.RepoFile
import com.joelkanyi.platypus.domain.model.RepoRef
import com.joelkanyi.platypus.domain.model.RepositoryDetail
import com.joelkanyi.platypus.domain.model.SrcEntryType
import com.joelkanyi.platypus.domain.repository.AuthRepository
import com.joelkanyi.platypus.domain.repository.RepoContentRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultRepoContentRepository(private val authRepository: AuthRepository) : RepoContentRepository {

    private val directoryCache = mutableMapOf<String, DirectoryListing>()
    private val fileCache = mutableMapOf<String, RepoFile>()
    private val pathsCache = mutableMapOf<String, List<String>>()

    override fun clearCache() {
        directoryCache.clear()
        fileCache.clear()
        pathsCache.clear()
    }

    override suspend fun repository(repo: RepoRef): NetworkResult<RepositoryDetail> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        val client = authRepository.authenticatedClient(accountId)
            ?: return NetworkResult.Failure.Http(401, SIGNED_OUT)
        return safeApiCall(::ktorErrorMapper) {
            RepoContentApi(client).repository(workspaceSlug, repoSlug).toDomain()
        }
    }

    override suspend fun directory(repo: RepoRef, ref: String, path: String): NetworkResult<DirectoryListing> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        val key = cacheKey(accountId, workspaceSlug, repoSlug, ref, path)
        directoryCache[key]?.let { return NetworkResult.Success(it) }
        val client = authRepository.authenticatedClient(accountId)
            ?: return NetworkResult.Failure.Http(401, SIGNED_OUT)
        return safeApiCall(::ktorErrorMapper) {
            val api = RepoContentApi(client)
            val entries = collectPaged(
                firstPage = { api.directory(workspaceSlug, repoSlug, ref, path) },
                nextPage = { api.directoryPage(it) },
            ).map { it.toDomain() }
            val sorted = entries.sortedWith(
                compareBy({ it.type != SrcEntryType.DIRECTORY }, { it.name.lowercase() }),
            )
            DirectoryListing(sorted, null).also { directoryCache[key] = it }
        }
    }

    override suspend fun file(repo: RepoRef, ref: String, path: String): NetworkResult<RepoFile> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        val key = cacheKey(accountId, workspaceSlug, repoSlug, ref, path)
        fileCache[key]?.let { return NetworkResult.Success(it) }
        val client = authRepository.authenticatedClient(accountId)
            ?: return NetworkResult.Failure.Http(401, SIGNED_OUT)
        val webUrl = "https://bitbucket.org/$workspaceSlug/$repoSlug/src/$ref/$path"
        return safeApiCall(::ktorErrorMapper) {
            val api = RepoContentApi(client)
            val meta = api.fileMeta(workspaceSlug, repoSlug, ref, path)
            val file = if (!isRenderable(meta.mimetype, meta.size)) {
                RepoFile(path, emptyList(), truncatedAtLine = null, renderable = false, webUrl = webUrl)
            } else {
                val allLines = api.fileText(workspaceSlug, repoSlug, ref, path).split("\n")
                val over = allLines.size > MAX_LINES
                RepoFile(
                    path = path,
                    lines = if (over) allLines.take(MAX_LINES) else allLines,
                    truncatedAtLine = if (over) MAX_LINES else null,
                    renderable = true,
                    webUrl = webUrl,
                )
            }
            file.also { fileCache[key] = it }
        }
    }

    override suspend fun paths(repo: RepoRef, ref: String): NetworkResult<List<String>> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        val key = cacheKey(accountId, workspaceSlug, repoSlug, ref, "")
        pathsCache[key]?.let { return NetworkResult.Success(it) }
        val client = authRepository.authenticatedClient(accountId)
            ?: return NetworkResult.Failure.Http(401, SIGNED_OUT)
        return safeApiCall(::ktorErrorMapper) {
            val api = RepoContentApi(client)
            val out = collectPaged(
                maxPages = MAX_PATH_PAGES,
                firstPage = { api.paths(workspaceSlug, repoSlug, ref) },
                nextPage = { api.pathsPage(it) },
            ).filter { it.type == "commit_file" }.map { it.path }
            out.also { pathsCache[key] = it }
        }
    }

    override suspend fun branches(repo: RepoRef): NetworkResult<List<Branch>> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        val client = authRepository.authenticatedClient(accountId)
            ?: return NetworkResult.Failure.Http(401, SIGNED_OUT)
        return safeApiCall(::ktorErrorMapper) {
            val api = RepoContentApi(client)
            collectPaged(
                firstPage = { api.branches(workspaceSlug, repoSlug) },
                nextPage = { api.branchesPage(it) },
            ).map { it.toDomain() }
        }
    }

    override suspend fun commits(repo: RepoRef, ref: String, cursor: String?): NetworkResult<CommitPage> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        val client = authRepository.authenticatedClient(accountId)
            ?: return NetworkResult.Failure.Http(401, SIGNED_OUT)
        return safeApiCall(::ktorErrorMapper) {
            val api = RepoContentApi(client)
            val page = if (cursor.isNullOrBlank()) {
                api.commits(
                    workspaceSlug,
                    repoSlug,
                    ref,
                )
            } else {
                api.commitsPage(cursor)
            }
            CommitPage(commits = page.values.map { it.toDomain() }, next = page.next)
        }
    }

    override suspend fun commitDetail(repo: RepoRef, hash: String): NetworkResult<CommitDetail> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        val client = authRepository.authenticatedClient(accountId)
            ?: return NetworkResult.Failure.Http(401, SIGNED_OUT)
        return safeApiCall(::ktorErrorMapper) {
            val api = RepoContentApi(client)
            val commit = api.commit(workspaceSlug, repoSlug, hash).toDomain()
            val diff = api.diff(workspaceSlug, repoSlug, hash).split("\n").take(MAX_DIFF_LINES)
            CommitDetail(commit = commit, diffLines = diff)
        }
    }

    private fun isRenderable(mimetype: String?, size: Long): Boolean {
        if (size > MAX_BYTES) return false
        val type = mimetype?.lowercase() ?: return true
        return NON_RENDERABLE_PREFIXES.none { type.startsWith(it) }
    }

    private fun cacheKey(accountId: String, workspaceSlug: String, repoSlug: String, ref: String, path: String) =
        "$accountId|$workspaceSlug|$repoSlug|$ref|$path"

    private companion object {
        const val SIGNED_OUT = "This account is signed out."
        const val MAX_PATH_PAGES = 50
        const val MAX_DIFF_LINES = 3_000
        const val MAX_LINES = 1_000
        const val MAX_BYTES = 1_000_000L
        val NON_RENDERABLE_PREFIXES = listOf(
            "image/",
            "video/",
            "audio/",
            "application/octet-stream",
            "application/pdf",
            "application/zip",
            "application/x-",
        )
    }
}
