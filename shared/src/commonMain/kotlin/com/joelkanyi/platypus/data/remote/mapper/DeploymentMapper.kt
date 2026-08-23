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
import com.joelkanyi.platypus.data.remote.dto.ScheduleDto
import com.joelkanyi.platypus.domain.model.Deployment
import com.joelkanyi.platypus.domain.model.DeploymentStatus
import com.joelkanyi.platypus.domain.model.Schedule

fun DeploymentDto.toDomain(): Deployment {
    val stateName = state?.name?.uppercase()
    val statusName = state?.status?.name?.uppercase()
    val status = when {
        stateName == "IN_PROGRESS" -> DeploymentStatus.IN_PROGRESS
        stateName == "UNDEPLOYED" -> DeploymentStatus.UNDEPLOYED
        statusName == "FAILED" || statusName == "ERROR" || stateName == "FAILED" -> DeploymentStatus.FAILED
        statusName == "SUCCESSFUL" || stateName == "COMPLETED" -> DeploymentStatus.SUCCESSFUL
        else -> DeploymentStatus.UNKNOWN
    }
    val label = statusName?.lowercaseCapitalized()
        ?: stateName?.lowercaseCapitalized()
        ?: "Unknown"
    return Deployment(
        uuid = uuid,
        environmentName = environment?.name.orEmpty().ifBlank { "Environment" },
        environmentType = environment?.environmentType?.name?.takeIf { it.isNotBlank() },
        status = status,
        statusLabel = label,
        commitHash = release?.commit?.hash?.takeIf { it.isNotBlank() },
        releaseName = release?.name?.takeIf { it.isNotBlank() },
        deployerName = state?.deployer?.displayName?.takeIf { it.isNotBlank() },
        updatedOn = (state?.completedOn ?: lastUpdateTime ?: state?.startedOn)?.takeIf { it.isNotBlank() },
    )
}

fun ScheduleDto.toDomain(): Schedule = Schedule(
    uuid = uuid,
    enabled = enabled,
    refName = target?.refName?.takeIf { it.isNotBlank() },
    selectorPattern = target?.selector?.pattern?.takeIf { it.isNotBlank() },
    cronPattern = cronPattern,
)

private fun String.lowercaseCapitalized(): String =
    lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
