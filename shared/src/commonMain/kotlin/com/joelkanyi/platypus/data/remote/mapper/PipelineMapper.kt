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
package com.joelkanyi.platypus.data.remote.mapper

import com.joelkanyi.platypus.data.remote.dto.PipelineCommitDto
import com.joelkanyi.platypus.data.remote.dto.PipelineDto
import com.joelkanyi.platypus.data.remote.dto.PipelineSelectorDto
import com.joelkanyi.platypus.data.remote.dto.PipelineStateDto
import com.joelkanyi.platypus.data.remote.dto.PipelineStepDto
import com.joelkanyi.platypus.data.remote.dto.PipelineTargetRequestDto
import com.joelkanyi.platypus.data.remote.dto.PipelineTriggerRequestDto
import com.joelkanyi.platypus.data.remote.dto.PipelineVariableDto
import com.joelkanyi.platypus.domain.model.Pipeline
import com.joelkanyi.platypus.domain.model.PipelineStatus
import com.joelkanyi.platypus.domain.model.PipelineStep
import com.joelkanyi.platypus.domain.model.PipelineTriggerRequest
import com.joelkanyi.platypus.domain.model.RefType

fun PipelineStateDto?.toStatus(): PipelineStatus = when (this?.name?.uppercase()) {
    "PENDING" -> PipelineStatus.PENDING
    "IN_PROGRESS" -> PipelineStatus.IN_PROGRESS
    "PAUSED" -> PipelineStatus.PAUSED
    "HALTED" -> PipelineStatus.STOPPED
    "COMPLETED" -> when (result?.name?.uppercase()) {
        "SUCCESSFUL" -> PipelineStatus.SUCCESSFUL
        "FAILED" -> PipelineStatus.FAILED
        "ERROR" -> PipelineStatus.ERROR
        "STOPPED" -> PipelineStatus.STOPPED
        "SKIPPED" -> PipelineStatus.SKIPPED
        else -> PipelineStatus.UNKNOWN
    }
    else -> PipelineStatus.UNKNOWN
}

private fun refTypeOf(raw: String?): RefType? = when (raw?.lowercase()) {
    "branch", "named_branch" -> RefType.BRANCH
    "tag" -> RefType.TAG
    else -> null
}

fun PipelineDto.toDomain(): Pipeline = Pipeline(
    uuid = uuid,
    buildNumber = buildNumber,
    status = state.toStatus(),
    triggerName = trigger?.name.orEmpty(),
    refType = refTypeOf(target?.refType),
    refName = target?.refName,
    commitHash = target?.commit?.hash?.takeIf { it.isNotBlank() },
    commitMessage = target?.commit?.summary?.raw?.takeIf { it.isNotBlank() }
        ?: target?.commit?.message?.takeIf { it.isNotBlank() },
    selectorPattern = target?.selector?.takeIf { it.type == "custom" }?.pattern,
    pullRequestId = target?.pullrequest?.id?.takeIf { it != 0L },
    creatorName = creator?.displayName?.takeIf { it.isNotBlank() } ?: creator?.nickname.orEmpty(),
    creatorAvatarUrl = creator?.links?.avatar?.href,
    createdOn = createdOn,
    completedOn = completedOn,
    durationSeconds = durationInSeconds,
)

fun PipelineStepDto.toDomain(): PipelineStep = PipelineStep(
    uuid = uuid,
    name = name.ifBlank { "Step" },
    status = state.toStatus(),
    startedOn = startedOn,
    completedOn = completedOn,
    durationSeconds = durationInSeconds,
)

fun PipelineTriggerRequest.toDto(): PipelineTriggerRequestDto {
    val selector = customPattern?.takeIf { it.isNotBlank() }?.let { PipelineSelectorDto(type = "custom", pattern = it) }
    val target = if (commitHash != null && commitHash.isNotBlank()) {
        PipelineTargetRequestDto(
            type = "pipeline_commit_target",
            refType = refType.wire,
            refName = refName,
            selector = selector,
            commit = PipelineCommitDto(hash = commitHash),
        )
    } else {
        PipelineTargetRequestDto(
            type = "pipeline_ref_target",
            refType = refType.wire,
            refName = refName,
            selector = selector,
        )
    }
    return PipelineTriggerRequestDto(
        target = target,
        variables = variables.takeIf { it.isNotEmpty() }
            ?.map { PipelineVariableDto(it.key, it.value, it.secured) },
    )
}

private val RefType.wire: String get() = when (this) {
    RefType.BRANCH -> "branch"
    RefType.TAG -> "tag"
}
