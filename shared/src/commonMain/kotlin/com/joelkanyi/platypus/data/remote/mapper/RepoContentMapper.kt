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

import com.joelkanyi.platypus.data.remote.dto.RepositoryDetailDto
import com.joelkanyi.platypus.data.remote.dto.SrcEntryDto
import com.joelkanyi.platypus.domain.model.RepositoryDetail
import com.joelkanyi.platypus.domain.model.SrcEntry
import com.joelkanyi.platypus.domain.model.SrcEntryType

fun RepositoryDetailDto.toDomain(): RepositoryDetail = RepositoryDetail(
    name = name.ifBlank { slug },
    fullName = fullName,
    description = description,
    language = language,
    size = size,
    updatedOn = updatedOn,
    isPrivate = isPrivate,
    defaultBranch = mainbranch?.name?.ifBlank { "main" } ?: "main",
    avatarUrl = links?.avatar?.href,
    webUrl = links?.html?.href,
)

fun SrcEntryDto.toDomain(): SrcEntry = SrcEntry(
    path = path,
    type = if (type == "commit_directory") SrcEntryType.DIRECTORY else SrcEntryType.FILE,
    size = size,
)
