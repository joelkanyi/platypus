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
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
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
): HttpClient = base.config {
    install(Auth) {
        bearer {
            loadTokens { BearerTokens(initialAccessToken, initialRefreshToken) }
            refreshTokens {
                val previous = oldTokens?.refreshToken ?: initialRefreshToken
                tokenRefresher(previous)
            }
            sendWithoutRequest { true }
        }
    }
}
