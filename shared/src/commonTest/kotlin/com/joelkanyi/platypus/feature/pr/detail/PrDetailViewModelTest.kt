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
package com.joelkanyi.platypus.feature.pr.detail

import com.joelkanyi.platypus.data.remote.network.PlatypusJson
import com.joelkanyi.platypus.data.repository.DefaultPullRequestRepository
import com.joelkanyi.platypus.data.repository.FakeAuthRepository
import com.joelkanyi.platypus.domain.model.Account
import com.joelkanyi.platypus.domain.model.AuthMode
import com.joelkanyi.platypus.domain.model.BitbucketUser
import com.joelkanyi.platypus.domain.model.MergeStrategy
import com.joelkanyi.platypus.domain.model.PrState
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
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

@OptIn(ExperimentalCoroutinesApi::class)
class PrDetailViewModelTest {

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
    private val mergedDetail =
        """{"id":7,"title":"Add feature","state":"MERGED","author":{"uuid":"{other}","display_name":"Ada"}}"""

    private val client = HttpClient(MockEngine) {
        expectSuccess = true
        install(ContentNegotiation) { json(PlatypusJson) }
        engine {
            addHandler { request ->
                val path = request.url.encodedPath
                val body = if (path.endsWith("/pullrequests/7")) mergedDetail else """{"values":[]}"""
                respond(body, HttpStatusCode.OK, jsonHeaders)
            }
        }
    }

    private fun viewModel() = PrDetailViewModel(
        repository = DefaultPullRequestRepository(FakeAuthRepository(client = client, accounts = listOf(account))),
        accountId = "1",
        workspace = "acme",
        repoSlug = "api",
        prId = 7,
        initialMergeStrategy = MergeStrategy.MERGE_COMMIT,
        defaultCloseSourceBranch = false,
    )

    @Test
    fun loadsThePullRequestDetailThroughTheTypedReference() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val state = viewModel().uiState.first { !it.isLoading }

            assertEquals(7L, state.detail?.id)
            assertEquals(PrState.MERGED, state.detail?.state)
            assertEquals(null, state.error)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
