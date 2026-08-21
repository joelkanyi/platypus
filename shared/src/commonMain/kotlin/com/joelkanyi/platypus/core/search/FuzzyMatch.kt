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
package com.joelkanyi.platypus.core.search

/**
 * Subsequence fuzzy score for a path. Returns null when [query] is not a subsequence of [text];
 * otherwise a score where LOWER is a better match. A contiguous match inside the file name is
 * ranked well ahead of a scattered match across the full path.
 */
fun fuzzyScore(query: String, text: String): Int? {
    val q = query.lowercase().trim()
    if (q.isEmpty()) return 0
    val t = text.lowercase()

    var queryIndex = 0
    var previous = -1
    var gaps = 0
    for (i in t.indices) {
        if (queryIndex < q.length && t[i] == q[queryIndex]) {
            if (previous in 0 until i) gaps += i - previous - 1
            previous = i
            queryIndex++
        }
    }
    if (queryIndex < q.length) return null

    val baseName = t.substringAfterLast('/')
    val contiguousInName = baseName.contains(q)
    return gaps + t.length / 20 + if (contiguousInName) -500 else 0
}

/** Top [limit] paths matching [query], best first. */
fun fuzzyFilter(query: String, paths: List<String>, limit: Int = 50): List<String> {
    if (query.isBlank()) return emptyList()
    return paths
        .mapNotNull { path -> fuzzyScore(query, path)?.let { path to it } }
        .sortedBy { it.second }
        .take(limit)
        .map { it.first }
}
