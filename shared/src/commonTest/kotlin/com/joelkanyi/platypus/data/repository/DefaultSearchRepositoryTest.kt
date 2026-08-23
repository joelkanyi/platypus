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
import kotlin.test.assertIs

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private val account = Account(
    id = "1",
    user = BitbucketUser(uuid = "{me}", accountId = "a-1", nickname = "joel", displayName = "Joel", avatarUrl = null),
    mode = AuthMode.API_TOKEN,
)

private const val CODE =
    """{"values":[{"type":"code_search_result","content_match_count":2,""" +
        """"content_matches":[{"lines":[{"segments":[{"text":"val "},{"text":"token","match":true}]}]}],""" +
        """"file":{"path":"src/A.kt","commit":{"hash":"abc123",""" +
        """"repository":{"name":"api","full_name":"acme/api"}}}}],"next":null}"""

class DefaultSearchRepositoryTest {

    private fun repository(handler: MockRequestHandler): DefaultSearchRepository {
        val client = HttpClient(MockEngine) {
            expectSuccess = true
            install(ContentNegotiation) { json(PlatypusJson) }
            engine { addHandler(handler) }
        }
        return DefaultSearchRepository(FakeAuthRepository(client = client, accounts = listOf(account)))
    }

    @Test
    fun code_parses_results() = runTest {
        val repo = repository { respond(CODE, HttpStatusCode.OK, jsonHeaders) }
        val result = repo.code("1", "acme", "token")
        assertIs<NetworkResult.Success<*>>(result)
        val page = (result as NetworkResult.Success).data
        assertEquals(1, page.items.size)
        val hit = page.items.first()
        assertEquals("api", hit.repoSlug)
        assertEquals("abc123", hit.commitHash)
        assertEquals("token", hit.snippet.single().segments[1].text)
    }

    @Test
    fun code_gate_surfaces_http_403() = runTest {
        val repo = repository { respond("no", HttpStatusCode.Forbidden, jsonHeaders) }
        val result = repo.code("1", "acme", "token")
        assertIs<NetworkResult.Failure.Http>(result)
        assertEquals(403, (result as NetworkResult.Failure.Http).code)
    }
}
