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
package com.joelkanyi.platypus.domain.model

enum class PipelineStatus {
    PENDING,
    IN_PROGRESS,
    PAUSED,
    SUCCESSFUL,
    FAILED,
    ERROR,
    STOPPED,
    SKIPPED,
    UNKNOWN,
    ;

    val isRunning: Boolean get() = this == PENDING || this == IN_PROGRESS || this == PAUSED
}

enum class RefType { BRANCH, TAG }

data class Pipeline(
    val uuid: String,
    val buildNumber: Long,
    val status: PipelineStatus,
    val triggerName: String,
    val refType: RefType?,
    val refName: String?,
    val commitHash: String?,
    val commitMessage: String?,
    val selectorPattern: String?,
    val pullRequestId: Long?,
    val creatorName: String,
    val creatorAvatarUrl: String?,
    val createdOn: String,
    val completedOn: String?,
    val durationSeconds: Long,
)

data class PipelineStep(
    val uuid: String,
    val name: String,
    val status: PipelineStatus,
    val startedOn: String?,
    val completedOn: String?,
    val durationSeconds: Long,
    val testSummary: TestSummary? = null,
)

data class TestSummary(val passed: Int, val failed: Int, val skipped: Int, val total: Int)

data class PipelineVariable(val key: String, val value: String, val secured: Boolean = false)

data class PipelineTriggerRequest(
    val refType: RefType,
    val refName: String,
    val commitHash: String? = null,
    val customPattern: String? = null,
    val variables: List<PipelineVariable> = emptyList(),
)

fun Pipeline.rerunRequest(): PipelineTriggerRequest? {
    val ref = refName?.takeIf { it.isNotBlank() } ?: return null
    return PipelineTriggerRequest(
        refType = refType ?: RefType.BRANCH,
        refName = ref,
        commitHash = commitHash,
        customPattern = selectorPattern,
    )
}
