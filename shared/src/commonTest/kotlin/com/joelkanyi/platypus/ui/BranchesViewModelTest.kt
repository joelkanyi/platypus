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
package com.joelkanyi.platypus.ui

import com.joelkanyi.platypus.data.remote.network.PlatypusJson
import com.joelkanyi.platypus.data.repository.DefaultRepoContentRepository
import com.joelkanyi.platypus.data.repository.FakeAuthRepository
import com.joelkanyi.platypus.domain.model.Account
import com.joelkanyi.platypus.domain.model.AuthMode
import com.joelkanyi.platypus.domain.model.BitbucketUser
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BranchesViewModelTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val account = Account(
        id = "1",
        user = BitbucketUser(
            uuid = "{me}",
            accountId = "a-1",
            nickname = "joel",
            displayName = "Joel",
            avatarUrl = null,
        ),
        mode = AuthMode.API_TOKEN,
    )

    private fun client(handler: MockRequestHandler): HttpClient = HttpClient(MockEngine) {
        expectSuccess = true
        install(ContentNegotiation) { json(PlatypusJson) }
        engine { addHandler(handler) }
    }

    private fun repository(client: HttpClient?) =
        DefaultRepoContentRepository(FakeAuthRepository(client = client, accounts = listOf(account)))

    @Test
    fun loadsBranchesOnInit() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val branchesJson = """{"values":[{"name":"main"},{"name":"dev"}]}"""
            val viewModel = BranchesViewModel(
                repository(client { respond(branchesJson, HttpStatusCode.OK, jsonHeaders) }),
                accountId = "1",
                workspace = "acme",
                repoSlug = "api-gateway",
            )

            val state = viewModel.uiState.first { !it.isLoading }
            assertEquals(listOf("main", "dev"), state.branches.map { it.name })
            assertNull(state.error)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun exposesErrorWhenAccountSignedOut() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val viewModel = BranchesViewModel(
                repository(client = null),
                accountId = "1",
                workspace = "acme",
                repoSlug = "api-gateway",
            )

            val state = viewModel.uiState.first { !it.isLoading }
            assertNotNull(state.error)
            assertTrue(state.branches.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }
}
