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
package com.joelkanyi.platypus.domain.repository

import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.domain.model.Account
import com.joelkanyi.platypus.domain.model.AuthStatus
import com.joelkanyi.platypus.domain.model.Workspace
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {

    val status: StateFlow<AuthStatus>

    val accounts: StateFlow<List<Account>>

    fun authorizeUrl(): String?

    suspend fun restore()

    suspend fun signInWithApiToken(email: String, token: String): NetworkResult<Account>

    suspend fun completeOAuth(code: String): NetworkResult<Account>

    suspend fun workspaces(accountId: String): NetworkResult<List<Workspace>>

    suspend fun signOut(accountId: String)

    suspend fun signOutAll()

    fun authenticatedClient(accountId: String): HttpClient?
}
