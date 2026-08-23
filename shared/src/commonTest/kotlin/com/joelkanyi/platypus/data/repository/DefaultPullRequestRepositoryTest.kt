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
import com.joelkanyi.platypus.data.remote.PlatypusJson
import com.joelkanyi.platypus.domain.model.Account
import com.joelkanyi.platypus.domain.model.AuthMode
import com.joelkanyi.platypus.domain.model.BitbucketUser
import com.joelkanyi.platypus.domain.model.MergeStrategy
import com.joelkanyi.platypus.domain.model.PrApproval
import com.joelkanyi.platypus.domain.model.PrState
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val ME = "{me}"
private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private val account = Account(
    id = "1",
    user = BitbucketUser(uuid = ME, accountId = "a-1", nickname = "joel", displayName = "Joel", avatarUrl = null),
    mode = AuthMode.API_TOKEN,
)

private const val DETAIL =
    """{"id":7,"title":"Add feature","state":"OPEN",""" +
        """"author":{"uuid":"{other}","display_name":"Ada"},""" +
        """"participants":[{"user":{"uuid":"{me}"},"role":"REVIEWER","approved":false}],""" +
        """"source":{"branch":{"name":"feature"}},"destination":{"branch":{"name":"main"}}}"""

private const val COMMENTS =
    """{"values":[""" +
        """{"id":1,"content":{"raw":"hi"},"user":{"uuid":"{u}","display_name":"Ada"}},""" +
        """{"id":2,"deleted":true}""" +
        """]}"""

private const val MERGED = """{"id":7,"title":"Add feature","state":"MERGED"}"""

private const val DIFFSTAT_CONFLICT =
    """{"values":[{"status":"modified"},{"status":"merge conflict"}]}"""

private const val DIFFSTAT_CLEAN =
    """{"values":[{"status":"modified"},{"status":"added"}]}"""

class DefaultPullRequestRepositoryTest {

    private fun repository(handler: MockRequestHandler): DefaultPullRequestRepository {
        val client = HttpClient(MockEngine) {
            expectSuccess = true
            install(ContentNegotiation) { json(PlatypusJson) }
            engine { addHandler(handler) }
        }
        return DefaultPullRequestRepository(FakeAuthRepository(client = client, accounts = listOf(account)))
    }

    private val router: MockRequestHandler = { request ->
        val path = request.url.encodedPath
        when {
            path.endsWith("/pullrequests/7") && request.method == HttpMethod.Get ->
                respond(DETAIL, HttpStatusCode.OK, jsonHeaders)
            path.endsWith("/comments") && request.method == HttpMethod.Get ->
                respond(COMMENTS, HttpStatusCode.OK, jsonHeaders)
            path.endsWith("/approve") -> respond("", HttpStatusCode.OK)
            path.endsWith("/merge") -> respond(MERGED, HttpStatusCode.OK, jsonHeaders)
            path.contains("/diffstat/") && path.contains("conflict") ->
                respond(DIFFSTAT_CONFLICT, HttpStatusCode.OK, jsonHeaders)
            path.contains("/diffstat/") -> respond(DIFFSTAT_CLEAN, HttpStatusCode.OK, jsonHeaders)
            else -> respond("nope", HttpStatusCode.NotFound)
        }
    }

    @Test
    fun detailMapsFromApi() = runTest {
        val result = repository(router).detail("1", "acme", "api", 7)
        val detail = assertIs<NetworkResult.Success<*>>(
            result,
        ).data as com.joelkanyi.platypus.domain.model.PullRequestDetail
        assertEquals(PrState.OPEN, detail.state)
        assertEquals(PrApproval.NONE, detail.myApproval)
        assertEquals("feature", detail.sourceBranch)
    }

    @Test
    fun commentsFilterDeleted() = runTest {
        val result = repository(router).comments("1", "acme", "api", 7)
        val comments = assertIs<NetworkResult.Success<*>>(result).data as List<*>
        assertEquals(1, comments.size)
    }

    @Test
    fun approveSucceeds() = runTest {
        val result = repository(router).approve("1", "acme", "api", 7)
        assertIs<NetworkResult.Success<Unit>>(result)
    }

    @Test
    fun mergeReturnsUpdatedDetail() = runTest {
        val result = repository(router).merge("1", "acme", "api", 7, MergeStrategy.SQUASH, null, true)
        val detail = assertIs<NetworkResult.Success<*>>(
            result,
        ).data as com.joelkanyi.platypus.domain.model.PullRequestDetail
        assertEquals(PrState.MERGED, detail.state)
    }

    @Test
    fun hasConflictsTrueWhenDiffstatReportsConflict() = runTest {
        val result = repository(router).hasConflicts("1", "acme", "api", "conflictA", "b")
        assertEquals(true, assertIs<NetworkResult.Success<Boolean>>(result).data)
    }

    @Test
    fun hasConflictsFalseWhenDiffstatClean() = runTest {
        val result = repository(router).hasConflicts("1", "acme", "api", "cleanA", "b")
        assertEquals(false, assertIs<NetworkResult.Success<Boolean>>(result).data)
    }

    @Test
    fun hasConflictsFalseWhenCommitsMissing() = runTest {
        val result = repository(router).hasConflicts("1", "acme", "api", "", "b")
        assertEquals(false, assertIs<NetworkResult.Success<Boolean>>(result).data)
    }

    @Test
    fun failsWhenSignedOut() = runTest {
        val repo = DefaultPullRequestRepository(FakeAuthRepository(client = null, accounts = listOf(account)))
        val result = repo.detail("1", "acme", "api", 7)
        val failure = assertIs<NetworkResult.Failure.Http>(result)
        assertTrue(failure.code == 401)
    }
}
