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

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OAuthClientTest {

    /**
     * The whole point of the fix: Bitbucket answers an expired/missing token with
     * `WWW-Authenticate: OAuth`, which Ktor's bearer provider ignores. The client must still
     * refresh and retry off the 401 alone.
     */
    private val bitbucketAuthHandler: (freshToken: String) -> MockRequestHandler = { fresh ->
        { request ->
            if (request.headers[HttpHeaders.Authorization] == "Bearer $fresh") {
                respond("""{"ok":true}""", HttpStatusCode.OK)
            } else {
                respond(
                    content = "unauthorized",
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.WWWAuthenticate, """OAuth realm="Bitbucket.org HTTP""""),
                )
            }
        }
    }

    private fun base(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine) {
        expectSuccess = true
        engine { addHandler(handler) }
    }

    @Test
    fun refreshesAndRetriesWhenBitbucketReturnsOAuthChallenge401() = runTest {
        var refreshCount = 0
        val client = oauthClient(
            base = base(bitbucketAuthHandler("fresh")),
            initialAccessToken = "expired",
            initialRefreshToken = "rt-old",
        ) { previous ->
            refreshCount++
            assertEquals("rt-old", previous)
            BearerTokens("fresh", "rt-new")
        }

        val response = client.get("https://api.bitbucket.org/2.0/user")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1, refreshCount)
    }

    @Test
    fun refreshesWhenRestoredWithoutAnAccessToken() = runTest {
        var refreshCount = 0
        val client = oauthClient(
            base = base(bitbucketAuthHandler("fresh")),
            initialAccessToken = "",
            initialRefreshToken = "rt-old",
        ) {
            refreshCount++
            BearerTokens("fresh", "rt-new")
        }

        val response = client.get("https://api.bitbucket.org/2.0/user")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(1, refreshCount)
    }

    @Test
    fun surfacesUnauthorizedWhenRefreshFails() = runTest {
        val client = oauthClient(
            base = base(bitbucketAuthHandler("fresh")),
            initialAccessToken = "expired",
            initialRefreshToken = "rt-dead",
        ) { null }

        val failure = assertFailsWith<ClientRequestException> {
            client.get("https://api.bitbucket.org/2.0/user")
        }
        assertEquals(HttpStatusCode.Unauthorized, failure.response.status)
    }

    @Test
    fun concurrentUnauthorizedRequestsTriggerASingleRefresh() = runTest {
        var refreshCount = 0
        val client = oauthClient(
            base = base(bitbucketAuthHandler("fresh")),
            initialAccessToken = "expired",
            initialRefreshToken = "rt-old",
        ) {
            refreshCount++
            BearerTokens("fresh", "rt-new")
        }

        val responses = (1..5).map {
            async { client.get("https://api.bitbucket.org/2.0/user") }
        }.awaitAll()

        assertTrue(responses.all { it.status == HttpStatusCode.OK })
        assertEquals(1, refreshCount)
    }
}
