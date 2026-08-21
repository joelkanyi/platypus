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
package com.joelkanyi.platypus.data.auth

import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.data.remote.PlatypusJson
import com.joelkanyi.platypus.domain.model.AuthMode
import com.joelkanyi.platypus.domain.model.AuthStatus
import com.joelkanyi.platypus.domain.model.BitbucketUser
import com.joelkanyi.platypus.domain.model.Credential
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

private val configuredConfig = AuthConfig(
    backendBaseUrl = "https://backend.test",
    oauthClientId = "client-abc",
    redirectUri = "platypus://oauth/callback",
)

private const val USER_JSON =
    """{"uuid":"{u}","account_id":"acc-1","nickname":"joel","display_name":"Joel Kanyi",""" +
        """"links":{"avatar":{"href":"https://img/a.png"}}}"""

private const val WORKSPACES_JSON =
    """{"values":[{"workspace":{"uuid":"{w1}","slug":"acme","name":"Acme"}},""" +
        """{"workspace":{"uuid":"{w2}","slug":"beta","name":"Beta"}}]}"""

private const val TOKEN_JSON =
    """{"accessToken":"at-1","refreshToken":"rt-1","expiresIn":7200,"scopes":"account"}"""

class DefaultAuthRepositoryTest {

    private fun repository(
        config: AuthConfig = configuredConfig,
        store: FakeAccountStore = FakeAccountStore(),
        handler: MockRequestHandler,
    ): DefaultAuthRepository {
        val factory = {
            HttpClient(MockEngine) {
                expectSuccess = true
                install(ContentNegotiation) { json(PlatypusJson) }
                engine { addHandler(handler) }
            }
        }
        return DefaultAuthRepository(config, store, factory)
    }

    private val happyPath: MockRequestHandler = { request ->
        when (request.url.encodedPath) {
            "/2.0/user" -> respond(USER_JSON, HttpStatusCode.OK, jsonHeaders)
            "/2.0/user/workspaces" -> respond(WORKSPACES_JSON, HttpStatusCode.OK, jsonHeaders)
            "/auth/exchange", "/auth/refresh" -> respond(TOKEN_JSON, HttpStatusCode.OK, jsonHeaders)
            else -> respond("not found", HttpStatusCode.NotFound)
        }
    }

    @Test
    fun apiTokenSignInAddsAccountAndSignsIn() = runTest {
        val store = FakeAccountStore()
        val repo = repository(store = store, handler = happyPath)

        val result = repo.signInWithApiToken("joel@example.com", "atk")

        assertTrue(result is NetworkResult.Success)
        assertEquals("{u}", result.data.id)
        assertEquals("Joel Kanyi", result.data.user.displayName)
        assertEquals(AuthMode.API_TOKEN, result.data.mode)
        assertEquals(listOf("{u}"), store.stored.map { it.id })
        assertEquals(AuthStatus.SignedIn, repo.status.value)
        assertEquals(1, repo.accounts.value.size)
    }

    @Test
    fun apiTokenSignInFailureAddsNothing() = runTest {
        val store = FakeAccountStore()
        val repo = repository(store = store, handler = { respond("no", HttpStatusCode.Unauthorized) })
        repo.restore()

        val result = repo.signInWithApiToken("joel@example.com", "wrong")

        assertTrue(result is NetworkResult.Failure.Http)
        assertEquals(401, result.code)
        assertTrue(store.stored.isEmpty())
        assertEquals(AuthStatus.SignedOut, repo.status.value)
    }

    @Test
    fun workspacesReturnsMappedListForAccount() = runTest {
        val repo = repository(handler = happyPath)
        val account = (repo.signInWithApiToken("joel@example.com", "atk") as NetworkResult.Success).data

        val result = repo.workspaces(account.id)

        assertTrue(result is NetworkResult.Success)
        assertEquals(listOf("Acme", "Beta"), result.data.map { it.name })
    }

    @Test
    fun workspacesFailsForUnknownAccount() = runTest {
        val repo = repository(handler = happyPath)
        assertTrue(repo.workspaces("nope") is NetworkResult.Failure)
    }

    @Test
    fun signOutRemovesTheAccount() = runTest {
        val store = FakeAccountStore()
        val repo = repository(store = store, handler = happyPath)
        val account = (repo.signInWithApiToken("joel@example.com", "atk") as NetworkResult.Success).data

        repo.signOut(account.id)

        assertTrue(store.stored.isEmpty())
        assertTrue(repo.accounts.value.isEmpty())
        assertEquals(AuthStatus.SignedOut, repo.status.value)
    }

    @Test
    fun restoreRebuildsAccountsFromStoreWithoutNetwork() = runTest {
        val store = FakeAccountStore(
            listOf(
                StoredAccount(
                    id = "{u}",
                    user = BitbucketUser("{u}", "acc-1", "joel", "Joel Kanyi", null),
                    credential = Credential.ApiToken("joel@example.com", "atk"),
                ),
            ),
        )
        val repo = repository(store = store, handler = { respond("boom", HttpStatusCode.InternalServerError) })

        repo.restore()

        assertEquals(AuthStatus.SignedIn, repo.status.value)
        assertEquals(listOf("{u}"), repo.accounts.value.map { it.id })
    }

    @Test
    fun completeOAuthAddsAnOAuthAccount() = runTest {
        val store = FakeAccountStore()
        val repo = repository(store = store, handler = happyPath)

        val result = repo.completeOAuth("auth-code")

        assertTrue(result is NetworkResult.Success)
        assertEquals(AuthMode.OAUTH, result.data.mode)
        assertEquals(Credential.OAuth("rt-1"), store.stored.single().credential)
        assertEquals(AuthStatus.SignedIn, repo.status.value)
    }

    @Test
    fun authorizeUrlEncodesRedirectWhenConfigured() {
        val repo = repository(handler = happyPath)
        val url = repo.authorizeUrl()

        assertTrue(url != null)
        assertTrue(url.contains("client_id=client-abc"))
        assertTrue(url.contains("redirect_uri=platypus%3A%2F%2Foauth%2Fcallback"))
    }

    @Test
    fun authorizeUrlNullWhenNotConfigured() {
        val repo = repository(
            config = AuthConfig(backendBaseUrl = "", oauthClientId = "", redirectUri = "platypus://oauth/callback"),
            handler = happyPath,
        )
        assertEquals(null, repo.authorizeUrl())
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    }
}
