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
package com.joelkanyi.platypus.data.remote.api

import com.joelkanyi.platypus.data.remote.dto.ExchangeRequestDto
import com.joelkanyi.platypus.data.remote.dto.RefreshRequestDto
import com.joelkanyi.platypus.data.remote.dto.TokenResponseDto
import com.joelkanyi.platypus.domain.model.AuthConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthBackendApi(private val client: HttpClient, private val config: AuthConfig) {

    suspend fun exchange(code: String): TokenResponseDto = client.post("${config.backendBaseUrl}/auth/exchange") {
        contentType(ContentType.Application.Json)
        setBody(ExchangeRequestDto(code = code, redirectUri = config.redirectUri))
    }.body()

    suspend fun refresh(refreshToken: String): TokenResponseDto = client.post("${config.backendBaseUrl}/auth/refresh") {
        contentType(ContentType.Application.Json)
        setBody(RefreshRequestDto(refreshToken = refreshToken))
    }.body()
}
