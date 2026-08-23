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
package com.joelkanyi.platypus.data.remote.api

import com.joelkanyi.platypus.data.remote.BITBUCKET_API_BASE
import com.joelkanyi.platypus.data.remote.dto.DeploymentDto
import com.joelkanyi.platypus.data.remote.dto.PageDto
import com.joelkanyi.platypus.data.remote.dto.PipelineDto
import com.joelkanyi.platypus.data.remote.dto.PipelineStepDto
import com.joelkanyi.platypus.data.remote.dto.PipelineTriggerRequestDto
import com.joelkanyi.platypus.data.remote.dto.ScheduleDto
import com.joelkanyi.platypus.data.remote.dto.TestReportDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart

class PipelinesApi(private val client: HttpClient) {

    suspend fun list(workspaceSlug: String, repoSlug: String): PageDto<PipelineDto> =
        client.get(collectionUrl(workspaceSlug, repoSlug)) {
            parameter("sort", "-created_on")
            parameter("pagelen", PAGE_LEN)
        }.body()

    suspend fun page(url: String): PageDto<PipelineDto> = client.get(url).body()

    suspend fun get(workspaceSlug: String, repoSlug: String, uuid: String): PipelineDto =
        client.get(pipelineUrl(workspaceSlug, repoSlug, uuid)).body()

    suspend fun steps(workspaceSlug: String, repoSlug: String, uuid: String): PageDto<PipelineStepDto> =
        client.get("${pipelineUrl(workspaceSlug, repoSlug, uuid)}/steps/") {
            parameter("pagelen", PAGE_LEN)
        }.body()

    suspend fun stepsPage(url: String): PageDto<PipelineStepDto> = client.get(url).body()

    suspend fun testReport(
        workspaceSlug: String,
        repoSlug: String,
        pipelineUuid: String,
        stepUuid: String,
    ): TestReportDto = client.get(
        "${pipelineUrl(workspaceSlug, repoSlug, pipelineUuid)}/steps/${stepUuid.encodeURLPathPart()}/test_reports",
    ).body()

    suspend fun stepLog(workspaceSlug: String, repoSlug: String, pipelineUuid: String, stepUuid: String): String =
        client.get(
            "${pipelineUrl(workspaceSlug, repoSlug, pipelineUuid)}/steps/${stepUuid.encodeURLPathPart()}/log",
        ) {
            accept(ContentType.Text.Plain)
            accept(ContentType.Any)
        }.bodyAsText()

    suspend fun trigger(workspaceSlug: String, repoSlug: String, body: PipelineTriggerRequestDto): PipelineDto =
        client.post(collectionUrl(workspaceSlug, repoSlug)) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun stop(workspaceSlug: String, repoSlug: String, uuid: String) {
        client.post("${pipelineUrl(workspaceSlug, repoSlug, uuid)}/stopPipeline")
    }

    suspend fun deployments(workspaceSlug: String, repoSlug: String): PageDto<DeploymentDto> =
        client.get("$BITBUCKET_API_BASE/repositories/$workspaceSlug/$repoSlug/deployments/") {
            parameter("pagelen", PAGE_LEN)
        }.body()

    suspend fun deploymentsPage(url: String): PageDto<DeploymentDto> = client.get(url).body()

    suspend fun schedules(workspaceSlug: String, repoSlug: String): PageDto<ScheduleDto> =
        client.get("$BITBUCKET_API_BASE/repositories/$workspaceSlug/$repoSlug/pipelines_config/schedules/") {
            parameter("pagelen", PAGE_LEN)
        }.body()

    private fun collectionUrl(workspaceSlug: String, repoSlug: String): String =
        "$BITBUCKET_API_BASE/repositories/$workspaceSlug/$repoSlug/pipelines/"

    private fun pipelineUrl(workspaceSlug: String, repoSlug: String, uuid: String): String =
        "${collectionUrl(workspaceSlug, repoSlug)}${uuid.encodeURLPathPart()}"

    private companion object {
        const val PAGE_LEN = 30
    }
}
