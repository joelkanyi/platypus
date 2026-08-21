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
package com.joelkanyi.platypus.domain.repository

import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.domain.model.Branch
import com.joelkanyi.platypus.domain.model.CommitDetail
import com.joelkanyi.platypus.domain.model.CommitPage
import com.joelkanyi.platypus.domain.model.DirectoryListing
import com.joelkanyi.platypus.domain.model.RepoFile
import com.joelkanyi.platypus.domain.model.RepositoryDetail

interface RepoContentRepository {

    suspend fun repository(accountId: String, workspaceSlug: String, repoSlug: String): NetworkResult<RepositoryDetail>

    suspend fun directory(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        ref: String,
        path: String,
    ): NetworkResult<DirectoryListing>

    suspend fun file(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        ref: String,
        path: String,
    ): NetworkResult<RepoFile>

    suspend fun paths(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        ref: String,
    ): NetworkResult<List<String>>

    suspend fun branches(accountId: String, workspaceSlug: String, repoSlug: String): NetworkResult<List<Branch>>

    suspend fun commits(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        ref: String,
        cursor: String?,
    ): NetworkResult<CommitPage>

    suspend fun commitDetail(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        hash: String,
    ): NetworkResult<CommitDetail>
}
