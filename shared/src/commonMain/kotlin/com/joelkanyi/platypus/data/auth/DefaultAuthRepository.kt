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
import com.joelkanyi.platypus.core.result.safeApiCall
import com.joelkanyi.platypus.data.remote.api.UserApi
import com.joelkanyi.platypus.data.remote.ktorErrorMapper
import com.joelkanyi.platypus.data.remote.mapper.toDomain
import com.joelkanyi.platypus.domain.model.Account
import com.joelkanyi.platypus.domain.model.AuthMode
import com.joelkanyi.platypus.domain.model.AuthStatus
import com.joelkanyi.platypus.domain.model.BitbucketUser
import com.joelkanyi.platypus.domain.model.Credential
import com.joelkanyi.platypus.domain.model.Workspace
import com.joelkanyi.platypus.domain.repository.AuthRepository
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultAuthRepository(
    private val config: AuthConfig,
    private val accountStore: AccountStore,
    private val baseClientFactory: () -> HttpClient,
) : AuthRepository {

    private val baseClient: HttpClient by lazy { baseClientFactory() }
    private val backend: AuthBackendApi by lazy { AuthBackendApi(baseClient, config) }

    private val sessions = mutableMapOf<String, Session>()

    private val _status = MutableStateFlow<AuthStatus>(AuthStatus.Unknown)
    override val status: StateFlow<AuthStatus> = _status.asStateFlow()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    override val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    override fun authenticatedClient(accountId: String): HttpClient? = sessions[accountId]?.client

    override fun authorizeUrl(): String? {
        if (!config.isOAuthConfigured) return null
        val redirect = config.redirectUri.encodeURLParameter()
        return "${config.authorizeEndpoint}?client_id=${config.oauthClientId}&response_type=code&redirect_uri=$redirect"
    }

    override suspend fun restore() {
        accountStore.read().forEach { rebuildSession(it) }
        publish()
    }

    override suspend fun signInWithApiToken(email: String, token: String): NetworkResult<Account> {
        val client = apiTokenClient(baseClientFactory(), email, token)
        return finalize(client, AuthMode.API_TOKEN) { user ->
            StoredAccount(user.uuid, user, Credential.ApiToken(email, token))
        }
    }

    override suspend fun completeOAuth(code: String): NetworkResult<Account> {
        val tokens = when (val exchange = safeApiCall(::ktorErrorMapper) { backend.exchange(code) }) {
            is NetworkResult.Success -> exchange.data
            is NetworkResult.Failure -> return exchange
        }
        var accountId: String? = null
        val client = oauthClient(
            base = baseClientFactory(),
            initialAccessToken = tokens.accessToken,
            initialRefreshToken = tokens.refreshToken.orEmpty(),
            tokenRefresher = { oldRefresh -> refreshOAuth(oldRefresh) { accountId } },
        )
        return finalize(client, AuthMode.OAUTH) { user ->
            accountId = user.uuid
            StoredAccount(user.uuid, user, Credential.OAuth(tokens.refreshToken.orEmpty()))
        }
    }

    override suspend fun workspaces(accountId: String): NetworkResult<List<Workspace>> {
        val client = sessions[accountId]?.client
            ?: return NetworkResult.Failure.Unknown(IllegalStateException("No session for $accountId"))
        return safeApiCall(::ktorErrorMapper) {
            UserApi(client).getWorkspaces().values.mapNotNull { it.workspace?.toDomain() }
        }
    }

    override suspend fun signOut(accountId: String) {
        accountStore.remove(accountId)
        sessions.remove(accountId)?.client?.close()
        publish()
    }

    override suspend fun signOutAll() {
        accountStore.clear()
        sessions.values.forEach { it.client.close() }
        sessions.clear()
        publish()
    }

    private suspend fun finalize(
        client: HttpClient,
        mode: AuthMode,
        toStored: (BitbucketUser) -> StoredAccount,
    ): NetworkResult<Account> {
        val result = safeApiCall(::ktorErrorMapper) { UserApi(client).getCurrentUser().toDomain() }
        return when (result) {
            is NetworkResult.Success -> {
                val stored = toStored(result.data)
                accountStore.upsert(stored)
                val account = Account(stored.id, result.data, mode)
                sessions[stored.id]?.client?.close()
                sessions[stored.id] = Session(account, client)
                publish()
                NetworkResult.Success(account)
            }
            is NetworkResult.Failure -> {
                client.close()
                result
            }
        }
    }

    private fun rebuildSession(stored: StoredAccount) {
        val client = when (val credential = stored.credential) {
            is Credential.ApiToken ->
                apiTokenClient(baseClientFactory(), credential.email, credential.token)

            is Credential.OAuth ->
                oauthClient(
                    base = baseClientFactory(),
                    initialAccessToken = "",
                    initialRefreshToken = credential.refreshToken,
                    tokenRefresher = { oldRefresh -> refreshOAuth(oldRefresh) { stored.id } },
                )
        }
        sessions[stored.id] = Session(Account(stored.id, stored.user, stored.credential.mode()), client)
    }

    private suspend fun refreshOAuth(oldRefresh: String, accountId: () -> String?): BearerTokens? {
        val result = safeApiCall(::ktorErrorMapper) { backend.refresh(oldRefresh) }
        val newTokens = (result as? NetworkResult.Success)?.data ?: return null
        val newRefresh = newTokens.refreshToken ?: oldRefresh
        val id = accountId()
        if (id != null) {
            accountStore.read().firstOrNull { it.id == id }?.let { existing ->
                accountStore.upsert(existing.copy(credential = Credential.OAuth(newRefresh)))
            }
        }
        return BearerTokens(newTokens.accessToken, newRefresh)
    }

    private fun publish() {
        _accounts.value = sessions.values.map { it.account }
        _status.value = if (sessions.isEmpty()) AuthStatus.SignedOut else AuthStatus.SignedIn
    }

    private fun Credential.mode(): AuthMode = when (this) {
        is Credential.ApiToken -> AuthMode.API_TOKEN
        is Credential.OAuth -> AuthMode.OAUTH
    }

    private class Session(val account: Account, val client: HttpClient)
}
