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
import com.joelkanyi.platypus.domain.model.Repository
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private const val PAGE_ONE =
    """{"values":[""" +
        """{"uuid":"{r1}","name":"API Gateway","slug":"api-gateway",""" +
        """"full_name":"acme/api-gateway","is_private":true},""" +
        """{"uuid":"{r2}","name":"Web App","slug":"web-app",""" +
        """"full_name":"acme/web-app","is_private":false}""" +
        """],"next":"https://api.bitbucket.org/2.0/repositories/acme?page=2"}"""

class DefaultWatchlistRepositoryTest {

    private fun client(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine) {
        expectSuccess = true
        install(ContentNegotiation) { json(PlatypusJson) }
        engine { addHandler(handler) }
    }

    private fun repository(dao: FakeWatchedRepoDao, client: HttpClient?): DefaultWatchlistRepository =
        DefaultWatchlistRepository(dao, FakeAuthRepository(client))

    private fun sampleRepo(slug: String = "api-gateway") = Repository(
        uuid = "{$slug}",
        workspaceSlug = "acme",
        slug = slug,
        name = slug,
        fullName = "acme/$slug",
        description = "",
        isPrivate = true,
        avatarUrl = null,
    )

    @Test
    fun browseMapsRepositoriesAndCursor() = runTest {
        val repo = repository(
            dao = FakeWatchedRepoDao(),
            client = client { respond(PAGE_ONE, HttpStatusCode.OK, jsonHeaders) },
        )

        val result = repo.browse(accountId = "1", workspaceSlug = "acme", query = null, cursor = null)

        assertTrue(result is NetworkResult.Success)
        assertEquals(listOf("api-gateway", "web-app"), result.data.repositories.map { it.slug })
        assertEquals("acme", result.data.repositories.first().workspaceSlug)
        assertEquals("https://api.bitbucket.org/2.0/repositories/acme?page=2", result.data.next)
    }

    @Test
    fun browseFollowsCursorUrlWhenProvided() = runTest {
        var requestedPath: String? = null
        val repo = repository(
            dao = FakeWatchedRepoDao(),
            client = client { request ->
                requestedPath = request.url.encodedPath + "?" + request.url.encodedQuery
                respond("""{"values":[]}""", HttpStatusCode.OK, jsonHeaders)
            },
        )

        repo.browse("1", "acme", query = null, cursor = "https://api.bitbucket.org/2.0/repositories/acme?page=2")

        assertEquals("/2.0/repositories/acme?page=2", requestedPath)
    }

    @Test
    fun browseFailsWhenAccountSignedOut() = runTest {
        val repo = repository(dao = FakeWatchedRepoDao(), client = null)

        val result = repo.browse("1", "acme", query = null, cursor = null)

        assertTrue(result is NetworkResult.Failure.Http)
        assertEquals(401, result.code)
    }

    @Test
    fun watchThenUnwatchPersistsPerAccount() = runTest {
        val dao = FakeWatchedRepoDao()
        val repo = repository(dao, client = null)

        repo.watch("1", sampleRepo())
        assertEquals(listOf("api-gateway"), repo.watched("1").first().map { it.repoSlug })
        assertEquals(listOf("api-gateway"), repo.watchedAll().first().map { it.repoSlug })

        repo.unwatch("1", "acme", "api-gateway")
        assertTrue(repo.watched("1").first().isEmpty())
        assertTrue(repo.watchedAll().first().isEmpty())
    }
}
