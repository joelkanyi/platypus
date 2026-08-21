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
package com.joelkanyi.platypus.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedRepoDao {

    @Query("SELECT * FROM watched_repos ORDER BY repoName COLLATE NOCASE")
    fun observeAll(): Flow<List<WatchedRepoEntity>>

    @Query("SELECT * FROM watched_repos WHERE accountId = :accountId ORDER BY repoName COLLATE NOCASE")
    fun observeByAccount(accountId: String): Flow<List<WatchedRepoEntity>>

    @Upsert
    suspend fun upsert(repo: WatchedRepoEntity)

    @Query(
        "DELETE FROM watched_repos " +
            "WHERE accountId = :accountId AND workspaceSlug = :workspaceSlug AND repoSlug = :repoSlug",
    )
    suspend fun delete(accountId: String, workspaceSlug: String, repoSlug: String)
}
