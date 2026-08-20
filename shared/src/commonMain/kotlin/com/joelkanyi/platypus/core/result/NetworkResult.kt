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
package com.joelkanyi.platypus.core.result

sealed interface NetworkResult<out T> {

    data class Success<T>(val data: T) : NetworkResult<T>

    sealed interface Failure : NetworkResult<Nothing> {
        data class Http(val code: Int, val message: String? = null) : Failure
        data class Network(val cause: Throwable) : Failure
        data class Serialization(val cause: Throwable) : Failure
        data class Unknown(val cause: Throwable) : Failure
    }
}

inline fun <T, R> NetworkResult<T>.map(transform: (T) -> R): NetworkResult<R> = when (this) {
    is NetworkResult.Success -> NetworkResult.Success(transform(data))
    is NetworkResult.Failure -> this
}

fun <T> NetworkResult<T>.getOrNull(): T? = (this as? NetworkResult.Success)?.data

inline fun <T> NetworkResult<T>.onSuccess(action: (T) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Success) action(data)
    return this
}

inline fun <T> NetworkResult<T>.onFailure(action: (NetworkResult.Failure) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Failure) action(this)
    return this
}
