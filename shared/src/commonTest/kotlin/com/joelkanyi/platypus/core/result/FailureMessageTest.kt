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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FailureMessageTest {

    @Test
    fun serverMessageIsPreferredWhenPresent() {
        val message = NetworkResult.Failure.Http(401, "API Token provided has no Bitbucket scopes.").userMessage()
        assertEquals("API Token provided has no Bitbucket scopes.", message)
    }

    @Test
    fun unauthorizedMentionsCredentials() {
        val message = NetworkResult.Failure.Http(401).userMessage()
        assertTrue(message.contains("credentials", ignoreCase = true))
    }

    @Test
    fun forbiddenMentionsScopes() {
        val message = NetworkResult.Failure.Http(403).userMessage()
        assertTrue(message.contains("scopes", ignoreCase = true))
    }

    @Test
    fun serverErrorsShareOneMessage() {
        assertEquals(
            NetworkResult.Failure.Http(500).userMessage(),
            NetworkResult.Failure.Http(503).userMessage(),
        )
    }

    @Test
    fun unmappedHttpCodeIncludesTheCode() {
        assertTrue(NetworkResult.Failure.Http(418).userMessage().contains("418"))
    }

    @Test
    fun networkFailureMentionsConnection() {
        val message = NetworkResult.Failure.Network(RuntimeException("boom")).userMessage()
        assertTrue(message.contains("connection", ignoreCase = true))
    }
}
