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

import com.joelkanyi.platypus.data.remote.dto.BranchDto
import com.joelkanyi.platypus.data.remote.dto.CommitDto
import com.joelkanyi.platypus.data.remote.dto.PageDto
import com.joelkanyi.platypus.data.remote.dto.RepositoryDetailDto
import com.joelkanyi.platypus.data.remote.dto.SrcEntryDto
import com.joelkanyi.platypus.data.remote.network.BITBUCKET_API_BASE
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText

class RepoContentApi(private val client: HttpClient) {

    suspend fun repository(workspaceSlug: String, repoSlug: String): RepositoryDetailDto =
        client.get("$BITBUCKET_API_BASE/repositories/$workspaceSlug/$repoSlug").body()

    suspend fun directory(workspaceSlug: String, repoSlug: String, ref: String, path: String): PageDto<SrcEntryDto> =
        client.get(directoryUrl(workspaceSlug, repoSlug, ref, path)) {
            parameter("pagelen", PAGE_LEN)
        }.body()

    suspend fun directoryPage(url: String): PageDto<SrcEntryDto> = client.get(url).body()

    suspend fun paths(workspaceSlug: String, repoSlug: String, ref: String): PageDto<SrcEntryDto> =
        client.get(directoryUrl(workspaceSlug, repoSlug, ref, "")) {
            parameter("max_depth", MAX_DEPTH)
            parameter("pagelen", PAGE_LEN)
            parameter("fields", "next,values.path,values.type")
        }.body()

    suspend fun pathsPage(url: String): PageDto<SrcEntryDto> = client.get(url).body()

    suspend fun branches(workspaceSlug: String, repoSlug: String): PageDto<BranchDto> =
        client.get("$BITBUCKET_API_BASE/repositories/$workspaceSlug/$repoSlug/refs/branches") {
            parameter("pagelen", PAGE_LEN)
            parameter("sort", "name")
        }.body()

    suspend fun branchesPage(url: String): PageDto<BranchDto> = client.get(url).body()

    suspend fun commits(workspaceSlug: String, repoSlug: String, ref: String): PageDto<CommitDto> =
        client.get("$BITBUCKET_API_BASE/repositories/$workspaceSlug/$repoSlug/commits/$ref") {
            parameter("pagelen", COMMITS_PAGE_LEN)
        }.body()

    suspend fun commitsPage(url: String): PageDto<CommitDto> = client.get(url).body()

    suspend fun commit(workspaceSlug: String, repoSlug: String, hash: String): CommitDto =
        client.get("$BITBUCKET_API_BASE/repositories/$workspaceSlug/$repoSlug/commit/$hash").body()

    suspend fun diff(workspaceSlug: String, repoSlug: String, hash: String): String =
        client.get("$BITBUCKET_API_BASE/repositories/$workspaceSlug/$repoSlug/diff/$hash").bodyAsText()

    suspend fun fileMeta(workspaceSlug: String, repoSlug: String, ref: String, path: String): SrcEntryDto =
        client.get(fileUrl(workspaceSlug, repoSlug, ref, path)) {
            parameter("format", "meta")
        }.body()

    suspend fun fileText(workspaceSlug: String, repoSlug: String, ref: String, path: String): String =
        client.get(fileUrl(workspaceSlug, repoSlug, ref, path)).bodyAsText()

    // Bitbucket's src endpoint returns 404 for a directory without a trailing slash (`/src/master` fails,
    // `/src/master/` lists the root). Files must NOT have the trailing slash.
    private fun directoryUrl(workspaceSlug: String, repoSlug: String, ref: String, path: String): String {
        val trimmed = path.trim('/')
        val prefix = "$BITBUCKET_API_BASE/repositories/$workspaceSlug/$repoSlug/src/$ref"
        return if (trimmed.isEmpty()) "$prefix/" else "$prefix/$trimmed/"
    }

    private fun fileUrl(workspaceSlug: String, repoSlug: String, ref: String, path: String): String {
        val trimmed = path.trim('/')
        return "$BITBUCKET_API_BASE/repositories/$workspaceSlug/$repoSlug/src/$ref/$trimmed"
    }

    private companion object {
        const val PAGE_LEN = 100
        const val COMMITS_PAGE_LEN = 30
        const val MAX_DEPTH = 100
    }
}
