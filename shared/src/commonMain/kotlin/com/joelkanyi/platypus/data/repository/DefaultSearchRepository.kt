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
import com.joelkanyi.platypus.core.result.safeApiCall
import com.joelkanyi.platypus.data.remote.api.SearchApi
import com.joelkanyi.platypus.data.remote.mapper.toDomain
import com.joelkanyi.platypus.data.remote.network.ktorErrorMapper
import com.joelkanyi.platypus.domain.model.CodeSearchResult
import com.joelkanyi.platypus.domain.model.SearchPage
import com.joelkanyi.platypus.domain.repository.AuthRepository
import com.joelkanyi.platypus.domain.repository.SearchRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultSearchRepository(private val authRepository: AuthRepository) : SearchRepository {

    override suspend fun code(
        accountId: String,
        workspaceSlug: String,
        query: String,
    ): NetworkResult<SearchPage<CodeSearchResult>> = withClient(accountId) { client ->
        val page = client.api().code(workspaceSlug, query)
        SearchPage(page.values.map { it.toDomain(workspaceSlug) }, page.next, page.size)
    }

    override suspend fun codePage(
        accountId: String,
        url: String,
        workspaceSlug: String,
    ): NetworkResult<SearchPage<CodeSearchResult>> = withClient(accountId) { client ->
        val page = client.api().codePage(url)
        SearchPage(page.values.map { it.toDomain(workspaceSlug) }, page.next)
    }

    private suspend inline fun <T> withClient(
        accountId: String,
        crossinline block: suspend (HttpClient) -> T,
    ): NetworkResult<T> {
        val client = authRepository.authenticatedClient(accountId)
            ?: return NetworkResult.Failure.Http(401, SIGNED_OUT)
        return safeApiCall(::ktorErrorMapper) { block(client) }
    }

    private fun HttpClient.api(): SearchApi = SearchApi(this)

    private companion object {
        const val SIGNED_OUT = "This account is signed out."
    }
}
