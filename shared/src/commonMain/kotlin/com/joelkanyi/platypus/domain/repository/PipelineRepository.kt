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
package com.joelkanyi.platypus.domain.repository

import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.domain.model.Deployment
import com.joelkanyi.platypus.domain.model.Pipeline
import com.joelkanyi.platypus.domain.model.PipelineStep
import com.joelkanyi.platypus.domain.model.PipelineTriggerRequest
import com.joelkanyi.platypus.domain.model.RepoRef
import com.joelkanyi.platypus.domain.model.Schedule

interface PipelineRepository {

    suspend fun pipelines(repo: RepoRef): NetworkResult<List<Pipeline>>

    suspend fun pipeline(repo: RepoRef, uuid: String): NetworkResult<Pipeline>

    suspend fun steps(repo: RepoRef, uuid: String): NetworkResult<List<PipelineStep>>

    suspend fun stepLog(repo: RepoRef, pipelineUuid: String, stepUuid: String): NetworkResult<String>

    suspend fun trigger(repo: RepoRef, request: PipelineTriggerRequest): NetworkResult<Pipeline>

    suspend fun stop(repo: RepoRef, uuid: String): NetworkResult<Unit>

    suspend fun deployments(repo: RepoRef): NetworkResult<List<Deployment>>

    suspend fun schedules(repo: RepoRef): NetworkResult<List<Schedule>>
}
