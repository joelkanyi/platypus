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

import com.joelkanyi.platypus.data.remote.network.PlatypusJson
import com.joelkanyi.platypus.data.repository.DefaultPullRequestRepository
import com.joelkanyi.platypus.data.repository.FakeAuthRepository
import com.joelkanyi.platypus.data.repository.FakeWatchlistRepository
import com.joelkanyi.platypus.domain.model.Account
import com.joelkanyi.platypus.domain.model.AuthMode
import com.joelkanyi.platypus.domain.model.BitbucketUser
import com.joelkanyi.platypus.domain.model.PrRelationship
import com.joelkanyi.platypus.domain.model.WatchedRepo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val ME = "{me}"
private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private val account = Account(
    id = "1",
    user = BitbucketUser(uuid = ME, accountId = "a-1", nickname = "joel", displayName = "Joel", avatarUrl = null),
    mode = AuthMode.API_TOKEN,
)

private val watched = WatchedRepo(
    accountId = "1",
    workspaceSlug = "acme",
    repoSlug = "api-gateway",
    repoUuid = "{r}",
    name = "API Gateway",
    fullName = "acme/api-gateway",
    avatarUrl = null,
)

private const val PR_PAGE =
    """{"values":[""" +
        """{"id":101,"title":"Review me","updated_on":"2026-08-21T08:00:00+00:00",""" +
        """"author":{"uuid":"{other}"},""" +
        """"participants":[{"user":{"uuid":"{me}"},"role":"REVIEWER","approved":false}]},""" +
        """{"id":102,"title":"Mine","updated_on":"2026-08-21T10:00:00+00:00","author":{"uuid":"{me}"}}""" +
        """]}"""

class GetReviewInboxTest {

    private fun client(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine) {
        expectSuccess = true
        install(ContentNegotiation) { json(PlatypusJson) }
        engine { addHandler(handler) }
    }

    @Test
    fun aggregatesTagsAndSortsByUpdatedDescending() = runTest {
        val useCase = GetReviewInbox(
            watchlistRepository = FakeWatchlistRepository(listOf(watched)),
            pullRequestRepository = DefaultPullRequestRepository(
                FakeAuthRepository(
                    client = client { respond(PR_PAGE, HttpStatusCode.OK, jsonHeaders) },
                    accounts = listOf(account),
                ),
            ),
        )

        val inbox = useCase()

        assertEquals(listOf(102L, 101L), inbox.pullRequests.map { it.id })
        assertEquals(PrRelationship.MINE, inbox.pullRequests[0].relationship)
        assertEquals(PrRelationship.TO_REVIEW, inbox.pullRequests[1].relationship)
        assertEquals("API Gateway", inbox.pullRequests[0].repoName)
        assertTrue(inbox.failures.isEmpty())
    }

    @Test
    fun reportsPerSourceFailureWhenAccountSignedOut() = runTest {
        val useCase = GetReviewInbox(
            watchlistRepository = FakeWatchlistRepository(listOf(watched)),
            pullRequestRepository = DefaultPullRequestRepository(
                FakeAuthRepository(client = null, accounts = listOf(account)),
            ),
        )

        val inbox = useCase()

        assertTrue(inbox.pullRequests.isEmpty())
        assertEquals(listOf("API Gateway"), inbox.failures.map { it.repoName })
    }
}
