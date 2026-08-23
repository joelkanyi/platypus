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
data class DeploymentDto(
    val uuid: String = "",
    val state: DeploymentStateDto? = null,
    val environment: DeploymentEnvironmentDto? = null,
    val release: DeploymentReleaseDto? = null,
    @SerialName("last_update_time") val lastUpdateTime: String? = null,
)

@Serializable
data class DeploymentStateDto(
    val name: String = "",
    val status: DeploymentStatusDto? = null,
    val deployer: PrUserDto? = null,
    @SerialName("started_on") val startedOn: String? = null,
    @SerialName("completed_on") val completedOn: String? = null,
)

@Serializable
data class DeploymentStatusDto(val name: String = "")

@Serializable
data class DeploymentEnvironmentDto(
    val name: String = "",
    @SerialName("environment_type") val environmentType: EnvironmentTypeDto? = null,
)

@Serializable
data class EnvironmentTypeDto(val name: String = "")

@Serializable
data class DeploymentReleaseDto(
    val name: String? = null,
    val commit: PipelineCommitDto? = null,
    @SerialName("created_on") val createdOn: String? = null,
)

@Serializable
data class ScheduleDto(
    val uuid: String = "",
    val enabled: Boolean = false,
    val target: PipelineTargetDto? = null,
    @SerialName("cron_pattern") val cronPattern: String = "",
)
