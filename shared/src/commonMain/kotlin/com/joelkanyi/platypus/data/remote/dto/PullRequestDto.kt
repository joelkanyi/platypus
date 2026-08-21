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
data class PullRequestDto(
    val id: Long,
    val title: String = "",
    val state: String = "",
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("created_on") val createdOn: String = "",
    @SerialName("updated_on") val updatedOn: String = "",
    val author: PrUserDto? = null,
    val participants: List<PrParticipantDto> = emptyList(),
    val source: PrEndpointDto? = null,
    val destination: PrEndpointDto? = null,
    val links: LinksDto? = null,
)

@Serializable
data class PrUserDto(
    val uuid: String = "",
    @SerialName("display_name") val displayName: String = "",
    val nickname: String = "",
    val links: LinksDto? = null,
)

@Serializable
data class PrParticipantDto(
    val user: PrUserDto? = null,
    val role: String = "",
    val approved: Boolean = false,
    val state: String? = null,
)

@Serializable
data class PrEndpointDto(val branch: PrBranchDto? = null)

@Serializable
data class PrBranchDto(val name: String = "")
