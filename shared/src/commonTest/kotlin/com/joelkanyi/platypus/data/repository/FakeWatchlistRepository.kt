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
import com.joelkanyi.platypus.domain.model.Repository
import com.joelkanyi.platypus.domain.model.RepositoryPage
import com.joelkanyi.platypus.domain.model.WatchedRepo
import com.joelkanyi.platypus.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeWatchlistRepository(private val watched: List<WatchedRepo>) : WatchlistRepository {

    override fun watchedAll(): Flow<List<WatchedRepo>> = flowOf(watched)

    override fun watched(accountId: String): Flow<List<WatchedRepo>> =
        flowOf(watched.filter { it.accountId == accountId })

    override suspend fun browse(
        accountId: String,
        workspaceSlug: String,
        query: String?,
        cursor: String?,
    ): NetworkResult<RepositoryPage> = error("unused in these tests")

    override suspend fun watch(accountId: String, repo: Repository) = Unit

    override suspend fun unwatch(accountId: String, workspaceSlug: String, repoSlug: String) = Unit

    override suspend fun clearAccount(accountId: String) = Unit
}
