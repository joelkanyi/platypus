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
package com.joelkanyi.platypus.domain.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class IdentifiersSerializationTest {

    private val json = Json

    @Test
    fun stringIdentifiersInlineToTheirRawString() {
        val ref = RepoRef(AccountId("acct-1"), WorkspaceSlug("acme"), RepoSlug("api-gateway"))

        val encoded = json.encodeToString(ref)

        assertEquals("""{"accountId":"acct-1","workspace":"acme","repoSlug":"api-gateway"}""", encoded)
        assertEquals(ref, json.decodeFromString<RepoRef>(encoded))
    }

    @Test
    fun prIdInlinesToANumber() {
        val prRef = PrRef(
            repo = RepoRef(AccountId("a"), WorkspaceSlug("w"), RepoSlug("r")),
            id = PrId(42),
        )

        val encoded = json.encodeToString(prRef)

        assertEquals("""{"repo":{"accountId":"a","workspace":"w","repoSlug":"r"},"id":42}""", encoded)
        assertEquals(prRef, json.decodeFromString<PrRef>(encoded))
    }

    @Test
    fun mergePairKeepsSourceAndDestinationDistinct() {
        val pair = MergePair(source = CommitHash("aaa"), destination = CommitHash("bbb"))

        val encoded = json.encodeToString(pair)

        assertEquals("""{"source":"aaa","destination":"bbb"}""", encoded)
        assertEquals(pair, json.decodeFromString<MergePair>(encoded))
    }
}
