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

import com.joelkanyi.platypus.data.remote.dto.CodeSearchResultDto
import com.joelkanyi.platypus.data.remote.dto.SearchSegmentDto
import com.joelkanyi.platypus.domain.model.CodeLine
import com.joelkanyi.platypus.domain.model.CodeSearchResult
import com.joelkanyi.platypus.domain.model.CodeSegment

fun CodeSearchResultDto.toDomain(fallbackWorkspace: String): CodeSearchResult {
    val fullName = file?.commit?.repository?.fullName.orEmpty()
    val workspace = fullName.substringBefore('/', fallbackWorkspace).ifBlank { fallbackWorkspace }
    val repoSlug = fullName.substringAfterLast('/', "")
    val repoName = file?.commit?.repository?.name?.ifBlank { repoSlug } ?: repoSlug
    val lines = contentMatches.firstOrNull()?.lines.orEmpty().map { line ->
        CodeLine(line.segments.map { it.toDomain() }, lineNumber = line.line)
    }
    return CodeSearchResult(
        workspaceSlug = workspace,
        repoSlug = repoSlug,
        repoName = repoName,
        path = file?.path.orEmpty(),
        commitHash = file?.commit?.hash.orEmpty(),
        matchCount = contentMatchCount,
        snippet = lines,
        pathSegments = pathMatches.map { it.toDomain() },
    )
}

private fun SearchSegmentDto.toDomain(): CodeSegment = CodeSegment(text = text, isMatch = match)
