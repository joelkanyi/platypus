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
import com.joelkanyi.platypus.domain.model.Schedule

interface PipelineRepository {

    suspend fun pipelines(accountId: String, workspaceSlug: String, repoSlug: String): NetworkResult<List<Pipeline>>

    suspend fun pipeline(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        uuid: String,
    ): NetworkResult<Pipeline>

    suspend fun steps(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        uuid: String,
    ): NetworkResult<List<PipelineStep>>

    suspend fun stepLog(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        pipelineUuid: String,
        stepUuid: String,
    ): NetworkResult<String>

    suspend fun trigger(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
        request: PipelineTriggerRequest,
    ): NetworkResult<Pipeline>

    suspend fun stop(accountId: String, workspaceSlug: String, repoSlug: String, uuid: String): NetworkResult<Unit>

    suspend fun deployments(
        accountId: String,
        workspaceSlug: String,
        repoSlug: String,
    ): NetworkResult<List<Deployment>>

    suspend fun schedules(accountId: String, workspaceSlug: String, repoSlug: String): NetworkResult<List<Schedule>>
}
