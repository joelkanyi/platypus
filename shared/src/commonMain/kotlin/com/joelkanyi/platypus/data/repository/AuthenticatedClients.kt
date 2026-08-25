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
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.plugin
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
internal fun apiTokenClient(base: HttpClient, email: String, token: String): HttpClient {
    val authHeader = "Basic " + Base64.encode("$email:$token".encodeToByteArray())
    return base.config {
        defaultRequest {
            header(HttpHeaders.Authorization, authHeader)
        }
    }
}

internal fun oauthClient(
    base: HttpClient,
    initialAccessToken: String,
    initialRefreshToken: String,
    tokenRefresher: suspend (String) -> BearerTokens?,
): HttpClient {
    val session = OAuthSession(initialAccessToken, initialRefreshToken, tokenRefresher)
    val client = base.config {}
    client.plugin(HttpSend).intercept { request ->
        val access = session.currentAccessToken()
        if (access.isNotEmpty()) {
            request.headers[HttpHeaders.Authorization] = "Bearer $access"
        }
        val call = execute(request)
        if (call.response.status != HttpStatusCode.Unauthorized) {
            return@intercept call
        }
        val refreshed = session.refresh(access) ?: return@intercept call
        request.headers[HttpHeaders.Authorization] = "Bearer $refreshed"
        execute(request)
    }
    return client
}

/**
 * Refreshes the OAuth access token on a 401 and retries the request.
 *
 * Bitbucket answers an expired or missing token with `WWW-Authenticate: OAuth`, not `Bearer`,
 * so Ktor's [io.ktor.client.plugins.auth.providers.bearer] provider never recognises the
 * challenge and never refreshes. Here the retry keys off the 401 status alone, and the mutex
 * collapses concurrent 401s into a single refresh.
 */
private class OAuthSession(
    initialAccessToken: String,
    initialRefreshToken: String,
    private val tokenRefresher: suspend (String) -> BearerTokens?,
) {
    private val mutex = Mutex()
    private var accessToken = initialAccessToken
    private var refreshToken = initialRefreshToken

    suspend fun currentAccessToken(): String = mutex.withLock { accessToken }

    suspend fun refresh(usedToken: String): String? = mutex.withLock {
        if (accessToken.isNotEmpty() && accessToken != usedToken) {
            return@withLock accessToken
        }
        val tokens = tokenRefresher(refreshToken) ?: return@withLock null
        accessToken = tokens.accessToken
        refreshToken = tokens.refreshToken ?: refreshToken
        accessToken
    }
}
