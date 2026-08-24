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
import com.joelkanyi.platypus.domain.model.MergePair
import com.joelkanyi.platypus.domain.model.MergeStrategy
import com.joelkanyi.platypus.domain.model.PrComment
import com.joelkanyi.platypus.domain.model.PrDiff
import com.joelkanyi.platypus.domain.model.PrRef
import com.joelkanyi.platypus.domain.model.PullRequest
import com.joelkanyi.platypus.domain.model.PullRequestDetail
import com.joelkanyi.platypus.domain.model.RepoRef

interface PullRequestRepository {

    /** Drops the in-memory diff cache. Call on sign-out so no session's diffs linger. */
    fun clearCache()

    suspend fun pullRequests(repo: RepoRef, repoName: String): NetworkResult<List<PullRequest>>

    suspend fun detail(pr: PrRef): NetworkResult<PullRequestDetail>

    suspend fun comments(pr: PrRef): NetworkResult<List<PrComment>>

    suspend fun addComment(
        pr: PrRef,
        raw: String,
        parentId: Long?,
        inlinePath: String? = null,
        inlineTo: Int? = null,
    ): NetworkResult<PrComment>

    suspend fun activity(pr: PrRef): NetworkResult<List<ActivityItem>>

    suspend fun commits(pr: PrRef): NetworkResult<List<Commit>>

    suspend fun resolveComment(pr: PrRef, commentId: Long, resolve: Boolean): NetworkResult<Unit>

    suspend fun approve(pr: PrRef): NetworkResult<Unit>

    suspend fun unapprove(pr: PrRef): NetworkResult<Unit>

    suspend fun requestChanges(pr: PrRef): NetworkResult<Unit>

    suspend fun unrequestChanges(pr: PrRef): NetworkResult<Unit>

    suspend fun merge(
        pr: PrRef,
        strategy: MergeStrategy,
        message: String?,
        closeSourceBranch: Boolean,
    ): NetworkResult<PullRequestDetail>

    suspend fun decline(pr: PrRef): NetworkResult<PullRequestDetail>

    suspend fun diff(pr: PrRef): NetworkResult<PrDiff>

    suspend fun hasConflicts(repo: RepoRef, pair: MergePair): NetworkResult<Boolean>
}
