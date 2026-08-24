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
import com.joelkanyi.platypus.domain.model.ActivityItem
import com.joelkanyi.platypus.domain.model.Commit
import com.joelkanyi.platypus.domain.model.MergeStrategy
import com.joelkanyi.platypus.domain.model.PrComment
import com.joelkanyi.platypus.domain.model.PrDiff
import com.joelkanyi.platypus.domain.model.PullRequest
import com.joelkanyi.platypus.domain.model.PullRequestDetail

interface PullRequestRepository {

    /** Drops the in-memory diff cache. Call on sign-out so no session's diffs linger. */
    fun clearCache()

    suspend fun pullRequests(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        repoName: String,
    ): NetworkResult<List<PullRequest>>

    suspend fun detail(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<PullRequestDetail>

    suspend fun comments(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<List<PrComment>>

    suspend fun addComment(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
        raw: String,
        parentId: Long?,
        inlinePath: String? = null,
        inlineTo: Int? = null,
    ): NetworkResult<PrComment>

    suspend fun activity(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<List<ActivityItem>>

    suspend fun commits(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<List<Commit>>

    suspend fun resolveComment(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
        commentId: Long,
        resolve: Boolean,
    ): NetworkResult<Unit>

    suspend fun approve(accountId: String, workspaceSlug: String, repoSlug: String, id: Long): NetworkResult<Unit>

    suspend fun unapprove(accountId: String, workspaceSlug: String, repoSlug: String, id: Long): NetworkResult<Unit>

    suspend fun requestChanges(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<Unit>

    suspend fun unrequestChanges(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<Unit>

    suspend fun merge(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
        strategy: MergeStrategy,
        message: String?,
        closeSourceBranch: Boolean,
    ): NetworkResult<PullRequestDetail>

    suspend fun decline(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        id: Long,
    ): NetworkResult<PullRequestDetail>

    suspend fun diff(accountId: String, workspaceSlug: String, repoSlug: String, id: Long): NetworkResult<PrDiff>

    suspend fun hasConflicts(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        sourceCommit: String,
        destinationCommit: String,
    ): NetworkResult<Boolean>
}
