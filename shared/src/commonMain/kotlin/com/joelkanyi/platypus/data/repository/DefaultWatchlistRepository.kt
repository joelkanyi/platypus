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
import com.joelkanyi.platypus.core.result.safeApiCall
import com.joelkanyi.platypus.data.local.WatchedRepoDao
import com.joelkanyi.platypus.data.local.toDomain
import com.joelkanyi.platypus.data.local.toEntity
import com.joelkanyi.platypus.data.remote.api.RepositoriesApi
import com.joelkanyi.platypus.data.remote.mapper.toDomain
import com.joelkanyi.platypus.data.remote.network.ktorErrorMapper
import com.joelkanyi.platypus.domain.model.Repository
import com.joelkanyi.platypus.domain.model.RepositoryPage
import com.joelkanyi.platypus.domain.model.WatchedRepo
import com.joelkanyi.platypus.domain.repository.AuthRepository
import com.joelkanyi.platypus.domain.repository.WatchlistRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultWatchlistRepository(
    private val watchedRepoDao: WatchedRepoDao,
    private val authRepository: AuthRepository,
) : WatchlistRepository {

    override fun watchedAll(): Flow<List<WatchedRepo>> =
        watchedRepoDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun watched(accountId: String): Flow<List<WatchedRepo>> =
        watchedRepoDao.observeByAccount(accountId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun browse(
        accountId: String,
        workspaceSlug: String,
        query: String?,
        cursor: String?,
    ): NetworkResult<RepositoryPage> {
        val client = authRepository.authenticatedClient(accountId)
            ?: return NetworkResult.Failure.Http(code = 401, message = "This account is signed out.")
        val api = RepositoriesApi(client)
        return safeApiCall(::ktorErrorMapper) {
            val page = if (cursor.isNullOrBlank()) api.list(workspaceSlug, query) else api.page(cursor)
            RepositoryPage(
                repositories = page.values.map { it.toDomain(workspaceSlug) },
                next = page.next,
            )
        }
    }

    override suspend fun watch(accountId: String, repo: Repository) {
        watchedRepoDao.upsert(repo.toEntity(accountId))
    }

    override suspend fun unwatch(accountId: String, workspaceSlug: String, repoSlug: String) {
        watchedRepoDao.delete(accountId, workspaceSlug, repoSlug)
    }

    override suspend fun clearAccount(accountId: String) {
        watchedRepoDao.deleteByAccount(accountId)
    }
}
