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
package com.joelkanyi.platypus.feature.repo.file

object FileFind {

    fun matchingLines(query: String, lines: List<String>): List<Int> {
        if (query.isBlank()) return emptyList()
        val jumpLine = lineJump(query)
        return if (jumpLine != null) {
            val lastLine = (lines.size - 1).coerceAtLeast(0)
            listOf((jumpLine - 1).coerceIn(0, lastLine))
        } else {
            lines.indices.filter { lines[it].contains(query, ignoreCase = true) }
        }
    }

    fun nextIndex(current: Int, matchCount: Int): Int = if (matchCount == 0) current else (current + 1) % matchCount

    fun previousIndex(current: Int, matchCount: Int): Int =
        if (matchCount == 0) current else (current - 1 + matchCount) % matchCount

    private fun lineJump(query: String): Int? =
        if (query.startsWith(":")) query.removePrefix(":").toIntOrNull() else null
}
