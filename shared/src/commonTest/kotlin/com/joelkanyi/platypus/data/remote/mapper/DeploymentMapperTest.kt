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

import com.joelkanyi.platypus.data.remote.dto.DeploymentDto
import com.joelkanyi.platypus.data.remote.dto.DeploymentEnvironmentDto
import com.joelkanyi.platypus.data.remote.dto.DeploymentReleaseDto
import com.joelkanyi.platypus.data.remote.dto.DeploymentStateDto
import com.joelkanyi.platypus.data.remote.dto.DeploymentStatusDto
import com.joelkanyi.platypus.data.remote.dto.PipelineCommitDto
import com.joelkanyi.platypus.data.remote.dto.PipelineSelectorDto
import com.joelkanyi.platypus.data.remote.dto.PipelineTargetDto
import com.joelkanyi.platypus.data.remote.dto.ScheduleDto
import com.joelkanyi.platypus.domain.model.DeploymentStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class DeploymentMapperTest {

    @Test
    fun deployment_completed_successful_maps() {
        val dto = DeploymentDto(
            uuid = "{d1}",
            state = DeploymentStateDto(name = "COMPLETED", status = DeploymentStatusDto("SUCCESSFUL")),
            environment = DeploymentEnvironmentDto(name = "Production"),
            release = DeploymentReleaseDto(name = "v1", commit = PipelineCommitDto(hash = "deadbeef")),
            lastUpdateTime = "2026-08-23T09:00:00+00:00",
        )
        val d = dto.toDomain()
        assertEquals("Production", d.environmentName)
        assertEquals(DeploymentStatus.SUCCESSFUL, d.status)
        assertEquals("deadbeef", d.commitHash)
    }

    @Test
    fun deployment_in_progress_maps() {
        val dto = DeploymentDto(uuid = "{d2}", state = DeploymentStateDto(name = "IN_PROGRESS"))
        assertEquals(DeploymentStatus.IN_PROGRESS, dto.toDomain().status)
    }

    @Test
    fun schedule_maps_target_and_cron() {
        val dto = ScheduleDto(
            uuid = "{s1}",
            enabled = true,
            target = PipelineTargetDto(refName = "main", selector = PipelineSelectorDto("custom", "nightly")),
            cronPattern = "0 0 * * *",
        )
        val s = dto.toDomain()
        assertEquals(true, s.enabled)
        assertEquals("main", s.refName)
        assertEquals("nightly", s.selectorPattern)
        assertEquals("0 0 * * *", s.cronPattern)
    }
}
