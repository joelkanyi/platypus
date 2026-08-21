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

data class RepositoryDetail(
    val name: String,
    val fullName: String,
    val description: String,
    val language: String,
    val size: Long,
    val updatedOn: String,
    val isPrivate: Boolean,
    val defaultBranch: String,
    val avatarUrl: String?,
    val webUrl: String?,
)

enum class SrcEntryType { DIRECTORY, FILE }

data class SrcEntry(val path: String, val type: SrcEntryType, val size: Long) {
    val name: String get() = path.trimEnd('/').substringAfterLast('/')
}

data class DirectoryListing(val entries: List<SrcEntry>, val next: String?)

data class RepoFile(
    val path: String,
    val lines: List<String>,
    val truncatedAtLine: Int?,
    val renderable: Boolean,
    val webUrl: String?,
) {
    val name: String get() = path.substringAfterLast('/')
}
