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
data class CommentDto(
    val id: Long,
    val content: CommentContentDto? = null,
    val user: PrUserDto? = null,
    @SerialName("created_on") val createdOn: String = "",
    @SerialName("updated_on") val updatedOn: String = "",
    val deleted: Boolean = false,
    val parent: CommentParentDto? = null,
    val inline: CommentInlineDto? = null,
    val resolution: CommentResolutionDto? = null,
)

@Serializable
data class CommentContentDto(val raw: String = "", val html: String = "")

@Serializable
data class CommentParentDto(val id: Long)

@Serializable
data class CommentInlineDto(val path: String = "", val from: Int? = null, val to: Int? = null)

@Serializable
data class CommentResolutionDto(@SerialName("type") val type: String = "")

@Serializable
data class CommentRequestDto(
    val content: CommentContentDto,
    val parent: CommentParentDto? = null,
    val inline: CommentInlineDto? = null,
)

@Serializable
data class ActivityDto(
    val approval: ActivityApprovalDto? = null,
    @SerialName("changes_requested") val changesRequested: ActivityApprovalDto? = null,
    val update: ActivityUpdateDto? = null,
    val comment: CommentDto? = null,
)

@Serializable
data class ActivityApprovalDto(val date: String = "", val user: PrUserDto? = null)

@Serializable
data class ActivityUpdateDto(
    val date: String = "",
    val author: PrUserDto? = null,
    val state: String = "",
    val title: String = "",
)

@Serializable
data class MergeRequestDto(
    @SerialName("merge_strategy") val mergeStrategy: String,
    val message: String? = null,
    @SerialName("close_source_branch") val closeSourceBranch: Boolean = false,
)
