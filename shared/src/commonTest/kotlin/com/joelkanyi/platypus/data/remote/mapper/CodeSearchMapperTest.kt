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
import com.joelkanyi.platypus.data.remote.dto.ContentLineDto
import com.joelkanyi.platypus.data.remote.dto.ContentMatchDto
import com.joelkanyi.platypus.data.remote.dto.SearchCommitDto
import com.joelkanyi.platypus.data.remote.dto.SearchFileDto
import com.joelkanyi.platypus.data.remote.dto.SearchRepositoryDto
import com.joelkanyi.platypus.data.remote.dto.SearchSegmentDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeSearchMapperTest {

    private val dto = CodeSearchResultDto(
        type = "code_search_result",
        contentMatchCount = 3,
        contentMatches = listOf(
            ContentMatchDto(
                lines = listOf(
                    ContentLineDto(
                        line = 12,
                        segments = listOf(
                            SearchSegmentDto("val ", false),
                            SearchSegmentDto("token", true),
                            SearchSegmentDto(" = load()", false),
                        ),
                    ),
                ),
            ),
        ),
        file = SearchFileDto(
            path = "src/main/kotlin/Auth.kt",
            commit = SearchCommitDto(
                hash = "abc123def",
                repository = SearchRepositoryDto(name = "api", fullName = "acme/api-gateway"),
            ),
        ),
    )

    @Test
    fun maps_repo_and_workspace_from_full_name() {
        val result = dto.toDomain(fallbackWorkspace = "fallback")
        assertEquals("acme", result.workspaceSlug)
        assertEquals("api-gateway", result.repoSlug)
        assertEquals("api", result.repoName)
    }

    @Test
    fun maps_path_hash_and_match_count() {
        val result = dto.toDomain(fallbackWorkspace = "acme")
        assertEquals("src/main/kotlin/Auth.kt", result.path)
        assertEquals("Auth.kt", result.fileName)
        assertEquals("src/main/kotlin/", result.directory)
        assertEquals("abc123def", result.commitHash)
        assertEquals(3, result.matchCount)
    }

    @Test
    fun maps_segments_with_match_flags() {
        val result = dto.toDomain(fallbackWorkspace = "acme")
        val segments = result.snippet.single().segments
        assertEquals(3, segments.size)
        assertTrue(segments[1].isMatch)
        assertEquals("token", segments[1].text)
        assertTrue(!segments[0].isMatch)
    }

    @Test
    fun falls_back_to_workspace_when_full_name_blank() {
        val result = CodeSearchResultDto(file = SearchFileDto(path = "x")).toDomain(fallbackWorkspace = "acme")
        assertEquals("acme", result.workspaceSlug)
        assertEquals("", result.repoSlug)
    }
}
