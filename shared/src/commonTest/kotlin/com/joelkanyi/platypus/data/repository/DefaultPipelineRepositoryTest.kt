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
import com.joelkanyi.platypus.data.remote.PlatypusJson
import com.joelkanyi.platypus.domain.model.Account
import com.joelkanyi.platypus.domain.model.AuthMode
import com.joelkanyi.platypus.domain.model.BitbucketUser
import com.joelkanyi.platypus.domain.model.PipelineStatus
import com.joelkanyi.platypus.domain.model.PipelineTriggerRequest
import com.joelkanyi.platypus.domain.model.RefType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

private val account = Account(
    id = "1",
    user = BitbucketUser(uuid = "{me}", accountId = "a-1", nickname = "joel", displayName = "Joel", avatarUrl = null),
    mode = AuthMode.API_TOKEN,
)

private const val LIST =
    """{"values":[""" +
        """{"uuid":"{p1}","build_number":42,"state":{"name":"COMPLETED","result":{"name":"SUCCESSFUL"}},""" +
        """"target":{"ref_type":"branch","ref_name":"main"}},""" +
        """{"uuid":"{p2}","build_number":41,"state":{"name":"IN_PROGRESS"},""" +
        """"target":{"ref_type":"branch","ref_name":"dev"}}""" +
        """]}"""

private const val ONE = """{"uuid":"{p1}","build_number":42,"state":{"name":"IN_PROGRESS"}}"""

private const val STEPS =
    """{"values":[""" +
        """{"uuid":"{s1}","name":"Build","state":{"name":"COMPLETED","result":{"name":"SUCCESSFUL"}}},""" +
        """{"uuid":"{s2}","name":"Deploy","state":{"name":"IN_PROGRESS"}}""" +
        """]}"""

private const val TRIGGERED = """{"uuid":"{p9}","build_number":43,"state":{"name":"PENDING"}}"""

class DefaultPipelineRepositoryTest {

    private fun repository(handler: MockRequestHandler): DefaultPipelineRepository {
        val client = HttpClient(MockEngine) {
            expectSuccess = true
            install(ContentNegotiation) { json(PlatypusJson) }
            engine { addHandler(handler) }
        }
        return DefaultPipelineRepository(FakeAuthRepository(client = client, accounts = listOf(account)))
    }

    private val router: MockRequestHandler = { request ->
        val path = request.url.encodedPath
        when {
            path.endsWith("/pipelines/") && request.method == HttpMethod.Get ->
                respond(LIST, HttpStatusCode.OK, jsonHeaders)
            path.endsWith("/stopPipeline") && request.method == HttpMethod.Post ->
                respond("", HttpStatusCode.NoContent)
            path.endsWith("/steps/") && request.method == HttpMethod.Get ->
                respond(STEPS, HttpStatusCode.OK, jsonHeaders)
            path.endsWith("/log") && request.method == HttpMethod.Get ->
                respond("+ ./gradlew build\nBUILD SUCCESSFUL", HttpStatusCode.OK, jsonHeaders)
            path.endsWith("/pipelines/") && request.method == HttpMethod.Post ->
                respond(TRIGGERED, HttpStatusCode.Created, jsonHeaders)
            request.method == HttpMethod.Get -> respond(ONE, HttpStatusCode.OK, jsonHeaders)
            else -> respond("", HttpStatusCode.OK)
        }
    }

    @Test
    fun pipelines_parses_list() = runTest {
        val result = repository(router).pipelines("1", "acme", "api")
        assertIs<NetworkResult.Success<*>>(result)
        val data = (result as NetworkResult.Success).data
        assertEquals(2, data.size)
        assertEquals(42, data.first().buildNumber)
        assertEquals(PipelineStatus.SUCCESSFUL, data.first().status)
    }

    @Test
    fun steps_parses_list() = runTest {
        val result = repository(router).steps("1", "acme", "api", "{p1}")
        assertIs<NetworkResult.Success<*>>(result)
        val data = (result as NetworkResult.Success).data
        assertEquals(2, data.size)
        assertEquals("Build", data.first().name)
        assertEquals(PipelineStatus.IN_PROGRESS, data[1].status)
    }

    @Test
    fun step_log_returns_text() = runTest {
        val result = repository(router).stepLog("1", "acme", "api", "{p1}", "{s1}")
        assertIs<NetworkResult.Success<*>>(result)
        assertTrue((result as NetworkResult.Success).data.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun trigger_returns_new_pipeline() = runTest {
        val request = PipelineTriggerRequest(refType = RefType.BRANCH, refName = "main")
        val result = repository(router).trigger("1", "acme", "api", request)
        assertIs<NetworkResult.Success<*>>(result)
        assertEquals(43, (result as NetworkResult.Success).data.buildNumber)
    }

    @Test
    fun stop_succeeds() = runTest {
        val result = repository(router).stop("1", "acme", "api", "{p1}")
        assertIs<NetworkResult.Success<*>>(result)
    }
}
