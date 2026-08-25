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

import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.domain.model.Account
import com.joelkanyi.platypus.domain.model.AuthStatus
import com.joelkanyi.platypus.domain.model.Workspace
import com.joelkanyi.platypus.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeAuthRepository(private val client: HttpClient?, accounts: List<Account> = emptyList()) : AuthRepository {

    override val status: StateFlow<AuthStatus> = MutableStateFlow(AuthStatus.SignedIn)
    override val accounts: StateFlow<List<Account>> = MutableStateFlow(accounts)

    override fun authorizeUrl(): String? = null

    override suspend fun restore() = Unit

    override suspend fun signInWithApiToken(email: String, token: String): NetworkResult<Account> =
        error("unused in these tests")

    override suspend fun completeOAuth(code: String, state: String?): NetworkResult<Account> =
        error("unused in these tests")

    override suspend fun workspaces(accountId: String): NetworkResult<List<Workspace>> = error("unused in these tests")

    override suspend fun signOut(accountId: String) = Unit

    override suspend fun signOutAll() = Unit

    override fun authenticatedClient(accountId: String): HttpClient? = client
}
