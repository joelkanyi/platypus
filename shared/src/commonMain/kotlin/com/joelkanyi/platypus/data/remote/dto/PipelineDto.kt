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
package com.joelkanyi.platypus.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PipelineDto(
    val uuid: String = "",
    @SerialName("build_number") val buildNumber: Long = 0,
    val state: PipelineStateDto? = null,
    val creator: PrUserDto? = null,
    val trigger: PipelineTriggerDto? = null,
    val target: PipelineTargetDto? = null,
    @SerialName("created_on") val createdOn: String = "",
    @SerialName("completed_on") val completedOn: String? = null,
    @SerialName("duration_in_seconds") val durationInSeconds: Long = 0,
)

@Serializable
data class PipelineStateDto(
    val name: String = "",
    val result: PipelineResultDto? = null,
    val stage: PipelineStageDto? = null,
)

@Serializable
data class PipelineResultDto(val name: String = "")

@Serializable
data class PipelineStageDto(val name: String = "")

@Serializable
data class PipelineTriggerDto(val name: String = "")

@Serializable
data class PipelineTargetDto(
    @SerialName("ref_type") val refType: String? = null,
    @SerialName("ref_name") val refName: String? = null,
    val selector: PipelineSelectorDto? = null,
    val commit: PipelineCommitDto? = null,
    val pullrequest: PipelinePullRequestRefDto? = null,
)

@Serializable
data class PipelineSelectorDto(val type: String = "", val pattern: String? = null)

@Serializable
data class PipelineCommitDto(
    val hash: String = "",
    val message: String? = null,
    val summary: PipelineCommitSummaryDto? = null,
)

@Serializable
data class PipelineCommitSummaryDto(val raw: String = "")

@Serializable
data class PipelinePullRequestRefDto(val id: Long = 0)

@Serializable
data class PipelineStepDto(
    val uuid: String = "",
    val name: String = "",
    val state: PipelineStateDto? = null,
    @SerialName("started_on") val startedOn: String? = null,
    @SerialName("completed_on") val completedOn: String? = null,
    @SerialName("duration_in_seconds") val durationInSeconds: Long = 0,
)

@Serializable
data class PipelineTriggerRequestDto(
    val target: PipelineTargetRequestDto,
    val variables: List<PipelineVariableDto>? = null,
)

@Serializable
data class PipelineTargetRequestDto(
    val type: String,
    @SerialName("ref_type") val refType: String? = null,
    @SerialName("ref_name") val refName: String? = null,
    val selector: PipelineSelectorDto? = null,
    val commit: PipelineCommitDto? = null,
)

@Serializable
data class PipelineVariableDto(val key: String, val value: String, val secured: Boolean = false)

@Serializable
data class TestReportDto(
    @SerialName("number_of_test_cases") val numberOfTestCases: Int = 0,
    @SerialName("number_of_successful_test_cases") val numberOfSuccessfulTestCases: Int = 0,
    @SerialName("number_of_failed_test_cases") val numberOfFailedTestCases: Int = 0,
    @SerialName("number_of_skipped_test_cases") val numberOfSkippedTestCases: Int = 0,
    @SerialName("number_of_error_test_cases") val numberOfErrorTestCases: Int = 0,
)
