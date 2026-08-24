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
package com.joelkanyi.platypus.domain.usecase

import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.domain.model.InboxSourceFailure
import com.joelkanyi.platypus.domain.model.PullRequest
import com.joelkanyi.platypus.domain.model.ReviewInbox
import com.joelkanyi.platypus.domain.repository.PullRequestRepository
import com.joelkanyi.platypus.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.first

class GetReviewInbox(
    private val watchlistRepository: WatchlistRepository,
    private val pullRequestRepository: PullRequestRepository,
) {

    suspend operator fun invoke(): ReviewInbox {
        val watched = watchlistRepository.watchedAll().first()

        val pullRequests = mutableListOf<PullRequest>()
        val failures = mutableListOf<InboxSourceFailure>()

        for (repo in watched) {
            val result = pullRequestRepository.pullRequests(
                accountId = repo.accountId,
                workspaceSlug = repo.workspaceSlug,
                repoSlug = repo.repoSlug,
                repoName = repo.name,
            )
            when (result) {
                is NetworkResult.Success -> pullRequests += result.data
                is NetworkResult.Failure -> failures += InboxSourceFailure(repo.name, result.userMessage())
            }
        }

        return ReviewInbox(
            pullRequests = pullRequests.sortedByDescending { it.updatedOn },
            failures = failures,
        )
    }
}
