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

import com.joelkanyi.platypus.data.auth.SecureStore
import com.joelkanyi.platypus.domain.model.PrRelationship
import com.joelkanyi.platypus.domain.model.PullRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class InMemorySecureStore : SecureStore {
    private val map = mutableMapOf<String, String>()

    override suspend fun get(key: String): String? = map[key]

    override suspend fun set(key: String, value: String) {
        map[key] = value
    }

    override suspend fun remove(key: String) {
        map.remove(key)
    }
}

private fun pr(id: Long, relationship: PrRelationship) = PullRequest(
    id = id,
    title = "PR $id",
    authorName = "Ada",
    authorAvatarUrl = null,
    sourceBranch = "feature",
    destinationBranch = "main",
    commentCount = 2,
    updatedOn = "2026-08-21T09:00:00+00:00",
    webUrl = null,
    relationship = relationship,
    accountId = "1",
    accountLabel = "Joel",
    workspaceSlug = "acme",
    repoSlug = "api",
    repoName = "API",
)

class DefaultInboxCacheTest {

    @Test
    fun loadReturnsNullWhenEmpty() = runTest {
        val cache = DefaultInboxCache(InMemorySecureStore())
        assertNull(cache.load())
    }

    @Test
    fun savedInboxRoundTrips() = runTest {
        val cache = DefaultInboxCache(InMemorySecureStore())
        val prs = listOf(pr(1, PrRelationship.TO_REVIEW), pr(2, PrRelationship.MINE))
        cache.save(prs, updatedAtEpochMs = 1_700_000_000_000L)
        val loaded = cache.load()
        assertEquals(1_700_000_000_000L, loaded?.updatedAtEpochMs)
        assertEquals(listOf(1L, 2L), loaded?.pullRequests?.map { it.id })
        assertEquals(PrRelationship.TO_REVIEW, loaded?.pullRequests?.first()?.relationship)
    }

    @Test
    fun clearRemovesCache() = runTest {
        val cache = DefaultInboxCache(InMemorySecureStore())
        cache.save(listOf(pr(1, PrRelationship.MINE)), updatedAtEpochMs = 1L)
        cache.clear()
        assertNull(cache.load())
    }

    @Test
    fun corruptDataLoadsAsNull() = runTest {
        val store = InMemorySecureStore()
        store.set("inbox_cache_v1", "not json")
        assertNull(DefaultInboxCache(store).load())
    }
}
