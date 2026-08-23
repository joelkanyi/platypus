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
import com.joelkanyi.platypus.data.remote.dto.PipelineCommitSummaryDto
import com.joelkanyi.platypus.data.remote.dto.PipelineDto
import com.joelkanyi.platypus.data.remote.dto.PipelineResultDto
import com.joelkanyi.platypus.data.remote.dto.PipelineSelectorDto
import com.joelkanyi.platypus.data.remote.dto.PipelineStateDto
import com.joelkanyi.platypus.data.remote.dto.PipelineTargetDto
import com.joelkanyi.platypus.domain.model.PipelineStatus
import com.joelkanyi.platypus.domain.model.PipelineTriggerRequest
import com.joelkanyi.platypus.domain.model.RefType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PipelineMapperTest {

    @Test
    fun completed_successful_maps_to_successful() {
        val state = PipelineStateDto(name = "COMPLETED", result = PipelineResultDto("SUCCESSFUL"))
        assertEquals(PipelineStatus.SUCCESSFUL, state.toStatus())
    }

    @Test
    fun completed_failed_maps_to_failed() {
        val state = PipelineStateDto(name = "COMPLETED", result = PipelineResultDto("FAILED"))
        assertEquals(PipelineStatus.FAILED, state.toStatus())
    }

    @Test
    fun in_progress_maps_to_in_progress_and_is_running() {
        val state = PipelineStateDto(name = "IN_PROGRESS")
        assertEquals(PipelineStatus.IN_PROGRESS, state.toStatus())
        assertEquals(true, state.toStatus().isRunning)
    }

    @Test
    fun halted_maps_to_stopped() {
        assertEquals(PipelineStatus.STOPPED, PipelineStateDto(name = "HALTED").toStatus())
    }

    @Test
    fun pipeline_dto_maps_fields() {
        val dto = PipelineDto(
            uuid = "{abc}",
            buildNumber = 42,
            state = PipelineStateDto(name = "COMPLETED", result = PipelineResultDto("SUCCESSFUL")),
            target = PipelineTargetDto(
                refType = "branch",
                refName = "main",
                commit = PipelineCommitDto(hash = "deadbeef", summary = PipelineCommitSummaryDto("Fix bug")),
                selector = PipelineSelectorDto(type = "custom", pattern = "deploy"),
            ),
            durationInSeconds = 120,
        )
        val pipeline = dto.toDomain()
        assertEquals(42, pipeline.buildNumber)
        assertEquals(PipelineStatus.SUCCESSFUL, pipeline.status)
        assertEquals(RefType.BRANCH, pipeline.refType)
        assertEquals("main", pipeline.refName)
        assertEquals("deadbeef", pipeline.commitHash)
        assertEquals("Fix bug", pipeline.commitMessage)
        assertEquals("deploy", pipeline.selectorPattern)
    }

    @Test
    fun default_branch_trigger_body_uses_ref_target_without_selector() {
        val dto = PipelineTriggerRequest(refType = RefType.BRANCH, refName = "main").toDto()
        assertEquals("pipeline_ref_target", dto.target.type)
        assertEquals("branch", dto.target.refType)
        assertEquals("main", dto.target.refName)
        assertNull(dto.target.selector)
        assertNull(dto.target.commit)
    }

    @Test
    fun custom_pipeline_trigger_body_includes_custom_selector() {
        val dto = PipelineTriggerRequest(
            refType = RefType.BRANCH,
            refName = "main",
            customPattern = "deploy-production",
        ).toDto()
        assertEquals("pipeline_ref_target", dto.target.type)
        assertEquals("custom", dto.target.selector?.type)
        assertEquals("deploy-production", dto.target.selector?.pattern)
    }

    @Test
    fun commit_pinned_trigger_body_uses_commit_target() {
        val dto = PipelineTriggerRequest(
            refType = RefType.BRANCH,
            refName = "main",
            commitHash = "deadbeef",
        ).toDto()
        assertEquals("pipeline_commit_target", dto.target.type)
        assertEquals("deadbeef", dto.target.commit?.hash)
    }
}
