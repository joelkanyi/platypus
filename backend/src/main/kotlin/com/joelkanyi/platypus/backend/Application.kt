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
package com.joelkanyi.platypus.backend

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.basicAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private const val TOKEN_ENDPOINT = "https://bitbucket.org/site/oauth2/access_token"

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(CIO, port = port, host = "0.0.0.0") { module() }.start(wait = true)
}

fun Application.module() {
    val clientId = System.getenv("BITBUCKET_CLIENT_ID").orEmpty()
    val clientSecret = System.getenv("BITBUCKET_CLIENT_SECRET").orEmpty()
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    val http = HttpClient(ClientCIO) {
        expectSuccess = false
        install(ClientContentNegotiation) { json(json) }
    }

    install(ContentNegotiation) { json(json) }

    routing {
        get("/health") { call.respondText("ok") }

        post("/auth/exchange") {
            if (clientId.isBlank() || clientSecret.isBlank()) {
                call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse("oauth_not_configured"))
                return@post
            }
            val request = call.receive<ExchangeRequest>()
            val response = http.submitForm(
                url = TOKEN_ENDPOINT,
                formParameters = parameters {
                    append("grant_type", "authorization_code")
                    append("code", request.code)
                    request.redirectUri?.let { append("redirect_uri", it) }
                },
            ) {
                basicAuth(clientId, clientSecret)
            }
            call.relayToken(response)
        }

        post("/auth/refresh") {
            if (clientId.isBlank() || clientSecret.isBlank()) {
                call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse("oauth_not_configured"))
                return@post
            }
            val request = call.receive<RefreshRequest>()
            val response = http.submitForm(
                url = TOKEN_ENDPOINT,
                formParameters = parameters {
                    append("grant_type", "refresh_token")
                    append("refresh_token", request.refreshToken)
                },
            ) {
                basicAuth(clientId, clientSecret)
            }
            call.relayToken(response)
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.relayToken(response: HttpResponse) {
    if (!response.status.isSuccess()) {
        respond(HttpStatusCode.fromValue(response.status.value), ErrorResponse("bitbucket_error"))
        return
    }
    val token = response.body<BitbucketToken>()
    respond(
        TokenResponse(
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            expiresIn = token.expiresIn,
            scopes = token.scopes,
        ),
    )
}

@Serializable
data class ExchangeRequest(val code: String, val redirectUri: String? = null)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class TokenResponse(val accessToken: String, val refreshToken: String?, val expiresIn: Long, val scopes: String?)

@Serializable
data class ErrorResponse(val error: String)

@Serializable
private data class BitbucketToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long = 0,
    @SerialName("scopes") val scopes: String? = null,
)
