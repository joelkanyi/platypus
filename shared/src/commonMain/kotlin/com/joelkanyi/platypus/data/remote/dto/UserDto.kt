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
data class UserDto(
    val uuid: String,
    @SerialName("account_id") val accountId: String? = null,
    val nickname: String = "",
    @SerialName("display_name") val displayName: String = "",
    val links: LinksDto? = null,
)

@Serializable
data class WorkspaceDto(val uuid: String, val slug: String = "", val name: String = "", val links: LinksDto? = null)

@Serializable
data class WorkspaceMembershipDto(val workspace: WorkspaceDto? = null)

@Serializable
data class LinksDto(val avatar: LinkDto? = null, val html: LinkDto? = null)

@Serializable
data class LinkDto(val href: String? = null)

@Serializable
data class PageDto<T>(val values: List<T> = emptyList(), val next: String? = null, val size: Int? = null)
