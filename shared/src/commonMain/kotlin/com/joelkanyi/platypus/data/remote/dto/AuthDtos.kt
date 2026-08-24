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
package com.joelkanyi.platypus.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExchangeRequestDto(val code: String, val redirectUri: String? = null)

@Serializable
data class RefreshRequestDto(val refreshToken: String)

@Serializable
data class TokenResponseDto(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresIn: Long = 0,
    val scopes: String? = null,
)
