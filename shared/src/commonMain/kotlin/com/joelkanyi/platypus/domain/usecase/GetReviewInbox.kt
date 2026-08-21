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
import com.joelkanyi.platypus.core.result.safeApiCall
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.data.remote.api.PullRequestsApi
import com.joelkanyi.platypus.data.remote.ktorErrorMapper
import com.joelkanyi.platypus.data.remote.mapper.toDomain
import com.joelkanyi.platypus.domain.model.Account
import com.joelkanyi.platypus.domain.model.InboxSourceFailure
import com.joelkanyi.platypus.domain.model.PullRequest
import com.joelkanyi.platypus.domain.model.ReviewInbox
import com.joelkanyi.platypus.domain.model.WatchedRepo
import com.joelkanyi.platypus.domain.repository.AuthRepository
import com.joelkanyi.platypus.domain.repository.WatchlistRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.first

class GetReviewInbox(
    private val authRepository: AuthRepository,
    private val watchlistRepository: WatchlistRepository,
) {

    suspend operator fun invoke(): ReviewInbox {
        val watched = watchlistRepository.watchedAll().first()
        val accountsById = authRepository.accounts.value.associateBy { it.id }

        val pullRequests = mutableListOf<PullRequest>()
        val failures = mutableListOf<InboxSourceFailure>()

        for (repo in watched) {
            val account = accountsById[repo.accountId]
            val client = account?.let { authRepository.authenticatedClient(it.id) }
            if (account == null || client == null) {
                failures += InboxSourceFailure(repo.name, "This account is signed out.")
                continue
            }
            when (val result = fetchOpen(client, repo, account)) {
                is NetworkResult.Success -> pullRequests += result.data
                is NetworkResult.Failure -> failures += InboxSourceFailure(repo.name, result.userMessage())
            }
        }

        return ReviewInbox(
            pullRequests = pullRequests.sortedByDescending { it.updatedOn },
            failures = failures,
        )
    }

    private suspend fun fetchOpen(
        client: HttpClient,
        repo: WatchedRepo,
        account: Account,
    ): NetworkResult<List<PullRequest>> = safeApiCall(::ktorErrorMapper) {
        val api = PullRequestsApi(client)
        val me = account.user.uuid
        val label = account.user.displayName
        val out = mutableListOf<PullRequest>()
        var page = api.open(repo.workspaceSlug, repo.repoSlug)
        var guard = 0
        while (true) {
            out += page.values.map { it.toDomain(me, repo, label) }
            val next = page.next
            if (next == null || ++guard >= MAX_PAGES) break
            page = api.page(next)
        }
        out
    }

    private companion object {
        const val MAX_PAGES = 10
    }
}
