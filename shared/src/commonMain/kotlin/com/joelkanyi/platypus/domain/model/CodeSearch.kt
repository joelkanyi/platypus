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

data class CodeSegment(val text: String, val isMatch: Boolean)

data class CodeLine(val segments: List<CodeSegment>, val lineNumber: Int = 0)

data class CodeSearchResult(
    val workspaceSlug: String,
    val repoSlug: String,
    val repoName: String,
    val path: String,
    val commitHash: String,
    val matchCount: Int,
    val snippet: List<CodeLine>,
    val pathSegments: List<CodeSegment>,
) {
    val fileName: String get() = path.substringAfterLast('/')
    val directory: String get() = path.substringBeforeLast('/', "").let { if (it.isBlank()) "" else "$it/" }
}

data class SearchPage<T>(val items: List<T>, val next: String?, val totalFiles: Int? = null)
