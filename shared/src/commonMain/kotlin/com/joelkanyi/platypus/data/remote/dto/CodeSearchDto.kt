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
data class CodeSearchResultDto(
    val type: String = "",
    @SerialName("content_match_count") val contentMatchCount: Int = 0,
    @SerialName("content_matches") val contentMatches: List<ContentMatchDto> = emptyList(),
    @SerialName("path_matches") val pathMatches: List<SearchSegmentDto> = emptyList(),
    val file: SearchFileDto? = null,
)

@Serializable
data class ContentMatchDto(val lines: List<ContentLineDto> = emptyList())

@Serializable
data class ContentLineDto(val line: Int = 0, val segments: List<SearchSegmentDto> = emptyList())

@Serializable
data class SearchSegmentDto(val text: String = "", val match: Boolean = false)

@Serializable
data class SearchFileDto(val path: String = "", val type: String = "", val commit: SearchCommitDto? = null)

@Serializable
data class SearchCommitDto(val hash: String = "", val repository: SearchRepositoryDto? = null)

@Serializable
data class SearchRepositoryDto(
    val name: String = "",
    @SerialName("full_name") val fullName: String = "",
    val uuid: String = "",
)
