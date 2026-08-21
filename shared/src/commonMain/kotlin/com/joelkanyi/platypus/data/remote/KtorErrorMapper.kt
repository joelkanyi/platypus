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
package com.joelkanyi.platypus.data.remote

import com.joelkanyi.platypus.core.result.NetworkResult
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString

fun ktorErrorMapper(cause: Throwable): NetworkResult.Failure = when (cause) {
    is ResponseException ->
        NetworkResult.Failure.Http(cause.response.status.value, bitbucketErrorMessage(cause.message))

    is HttpRequestTimeoutException,
    is ConnectTimeoutException,
    is SocketTimeoutException,
    ->
        NetworkResult.Failure.Network(cause)

    is SerializationException ->
        NetworkResult.Failure.Serialization(cause)

    else -> NetworkResult.Failure.Network(cause)
}

private fun bitbucketErrorMessage(raw: String?): String? {
    if (raw == null) return null
    val start = raw.indexOf('{')
    val end = raw.lastIndexOf('}')
    if (start < 0 || end <= start) return null
    return runCatching {
        PlatypusJson.decodeFromString<BitbucketErrorEnvelope>(raw.substring(start, end + 1)).error?.message
    }.getOrNull()?.takeIf { it.isNotBlank() }
}

@Serializable
private data class BitbucketErrorEnvelope(val error: BitbucketErrorBody? = null)

@Serializable
private data class BitbucketErrorBody(val message: String? = null)
