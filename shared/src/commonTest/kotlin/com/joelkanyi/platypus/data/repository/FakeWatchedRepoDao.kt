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

import com.joelkanyi.platypus.data.local.WatchedRepoDao
import com.joelkanyi.platypus.data.local.WatchedRepoEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeWatchedRepoDao : WatchedRepoDao {

    private val rows = MutableStateFlow<List<WatchedRepoEntity>>(emptyList())

    override fun observeAll(): Flow<List<WatchedRepoEntity>> =
        rows.map { list -> list.sortedBy { it.repoName.lowercase() } }

    override fun observeByAccount(accountId: String): Flow<List<WatchedRepoEntity>> =
        rows.map { list -> list.filter { it.accountId == accountId }.sortedBy { it.repoName.lowercase() } }

    override suspend fun upsert(repo: WatchedRepoEntity) {
        rows.update { current -> current.filterNot { it.sameKeyAs(repo) } + repo }
    }

    override suspend fun delete(accountId: String, workspaceSlug: String, repoSlug: String) {
        rows.update { current ->
            current.filterNot {
                it.accountId == accountId && it.workspaceSlug == workspaceSlug && it.repoSlug == repoSlug
            }
        }
    }

    override suspend fun deleteByAccount(accountId: String) {
        rows.update { current -> current.filterNot { it.accountId == accountId } }
    }

    private fun WatchedRepoEntity.sameKeyAs(other: WatchedRepoEntity): Boolean =
        accountId == other.accountId && workspaceSlug == other.workspaceSlug && repoSlug == other.repoSlug
}
