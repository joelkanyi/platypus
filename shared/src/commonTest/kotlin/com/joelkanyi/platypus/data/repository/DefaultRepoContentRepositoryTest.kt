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
import com.joelkanyi.platypus.data.remote.network.PlatypusJson
import com.joelkanyi.platypus.domain.model.AccountId
import com.joelkanyi.platypus.domain.model.Branch
import com.joelkanyi.platypus.domain.model.RepoRef
import com.joelkanyi.platypus.domain.model.RepoSlug
import com.joelkanyi.platypus.domain.model.WorkspaceSlug
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
private val repoRef = RepoRef(AccountId("1"), WorkspaceSlug("acme"), RepoSlug("api"))
private const val BRANCHES =
    """{"values":[{"name":"main","target":{"hash":"abc1234"}},{"name":"dev","target":{"hash":"def5678"}}]}"""

class DefaultRepoContentRepositoryTest {

    @Test
    fun branchesAreCachedAfterFirstFetch() = runTest {
        var branchCalls = 0
        val client = HttpClient(MockEngine) {
            expectSuccess = true
            install(ContentNegotiation) { json(PlatypusJson) }
            engine {
                addHandler { request ->
                    if (request.url.encodedPath.endsWith("/refs/branches")) branchCalls++
                    respond(BRANCHES, HttpStatusCode.OK, jsonHeaders)
                }
            }
        }
        val repository = DefaultRepoContentRepository(FakeAuthRepository(client = client))

        val first = assertIs<NetworkResult.Success<List<Branch>>>(repository.branches(repoRef))
        val second = assertIs<NetworkResult.Success<List<Branch>>>(repository.branches(repoRef))

        assertEquals(1, branchCalls)
        assertEquals(listOf("main", "dev"), first.data.map { it.name })
        assertEquals(first.data, second.data)
    }

    @Test
    fun clearCacheForcesRefetch() = runTest {
        var branchCalls = 0
        val client = HttpClient(MockEngine) {
            expectSuccess = true
            install(ContentNegotiation) { json(PlatypusJson) }
            engine {
                addHandler { request ->
                    if (request.url.encodedPath.endsWith("/refs/branches")) branchCalls++
                    respond(BRANCHES, HttpStatusCode.OK, jsonHeaders)
                }
            }
        }
        val repository = DefaultRepoContentRepository(FakeAuthRepository(client = client))

        repository.branches(repoRef)
        repository.clearCache()
        repository.branches(repoRef)

        assertEquals(2, branchCalls)
    }
}
