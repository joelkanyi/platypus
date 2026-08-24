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
import com.joelkanyi.platypus.data.remote.api.PipelinesApi
import com.joelkanyi.platypus.data.remote.mapper.toDomain
import com.joelkanyi.platypus.data.remote.mapper.toDto
import com.joelkanyi.platypus.data.remote.network.collectPaged
import com.joelkanyi.platypus.data.remote.network.ktorErrorMapper
import com.joelkanyi.platypus.domain.model.Deployment
import com.joelkanyi.platypus.domain.model.Pipeline
import com.joelkanyi.platypus.domain.model.PipelineStep
import com.joelkanyi.platypus.domain.model.PipelineTriggerRequest
import com.joelkanyi.platypus.domain.model.RepoRef
import com.joelkanyi.platypus.domain.model.Schedule
import com.joelkanyi.platypus.domain.model.TestSummary
import com.joelkanyi.platypus.domain.repository.AuthRepository
import com.joelkanyi.platypus.domain.repository.PipelineRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultPipelineRepository(private val authRepository: AuthRepository) : PipelineRepository {

    override suspend fun pipelines(repo: RepoRef): NetworkResult<List<Pipeline>> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        return withClient(accountId) { client ->
            val api = client.api()
            collectPaged(
                firstPage = { api.list(workspaceSlug, repoSlug) },
                nextPage = { api.page(it) },
            ).map { it.toDomain() }
        }
    }

    override suspend fun pipeline(repo: RepoRef, uuid: String): NetworkResult<Pipeline> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        return withClient(accountId) { client ->
            client.api().get(workspaceSlug, repoSlug, uuid).toDomain()
        }
    }

    override suspend fun steps(repo: RepoRef, uuid: String): NetworkResult<List<PipelineStep>> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        return withClient(accountId) { client ->
            val api = client.api()
            val out = collectPaged(
                firstPage = { api.steps(workspaceSlug, repoSlug, uuid) },
                nextPage = { api.stepsPage(it) },
            ).map { it.toDomain() }
            coroutineScope {
                out.map { step ->
                    async {
                        val report = runCatching {
                            api.testReport(workspaceSlug, repoSlug, uuid, step.uuid)
                        }.getOrNull()
                        if (report != null && report.numberOfTestCases > 0) {
                            step.copy(
                                testSummary = TestSummary(
                                    passed = report.numberOfSuccessfulTestCases,
                                    failed = report.numberOfFailedTestCases + report.numberOfErrorTestCases,
                                    skipped = report.numberOfSkippedTestCases,
                                    total = report.numberOfTestCases,
                                ),
                            )
                        } else {
                            step
                        }
                    }
                }.awaitAll()
            }
        }
    }

    override suspend fun stepLog(repo: RepoRef, pipelineUuid: String, stepUuid: String): NetworkResult<String> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        return withClient(accountId) { client ->
            client.api().stepLog(workspaceSlug, repoSlug, pipelineUuid, stepUuid)
        }
    }

    override suspend fun trigger(repo: RepoRef, request: PipelineTriggerRequest): NetworkResult<Pipeline> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        return withClient(accountId) { client ->
            client.api().trigger(workspaceSlug, repoSlug, request.toDto()).toDomain()
        }
    }

    override suspend fun stop(repo: RepoRef, uuid: String): NetworkResult<Unit> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        return withClient(accountId) { client ->
            client.api().stop(workspaceSlug, repoSlug, uuid)
        }
    }

    override suspend fun deployments(repo: RepoRef): NetworkResult<List<Deployment>> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        return withClient(accountId) { client ->
            val api = client.api()
            collectPaged(
                firstPage = { api.deployments(workspaceSlug, repoSlug) },
                nextPage = { api.deploymentsPage(it) },
            ).map { it.toDomain() }
        }
    }

    override suspend fun schedules(repo: RepoRef): NetworkResult<List<Schedule>> {
        val accountId = repo.accountId.value
        val workspaceSlug = repo.workspace.value
        val repoSlug = repo.repoSlug.value
        return withClient(accountId) { client ->
            client.api().schedules(workspaceSlug, repoSlug).values.map { it.toDomain() }
        }
    }

    private suspend inline fun <T> withClient(
        accountId: String,
        crossinline block: suspend (HttpClient) -> T,
    ): NetworkResult<T> {
        val client = authRepository.authenticatedClient(accountId)
            ?: return NetworkResult.Failure.Http(401, SIGNED_OUT)
        return safeApiCall(::ktorErrorMapper) { block(client) }
    }

    private fun HttpClient.api(): PipelinesApi = PipelinesApi(this)

    private companion object {
        const val SIGNED_OUT = "This account is signed out."
    }
}
