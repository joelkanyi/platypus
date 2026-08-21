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
data class RepositoryDetailDto(
    val name: String = "",
    val slug: String = "",
    @SerialName("full_name") val fullName: String = "",
    val description: String = "",
    val language: String = "",
    val size: Long = 0,
    @SerialName("updated_on") val updatedOn: String = "",
    @SerialName("is_private") val isPrivate: Boolean = false,
    val mainbranch: BranchRefDto? = null,
    val links: LinksDto? = null,
)

@Serializable
data class BranchRefDto(val name: String = "")

@Serializable
data class SrcEntryDto(val path: String = "", val type: String = "", val size: Long = 0, val mimetype: String? = null)
