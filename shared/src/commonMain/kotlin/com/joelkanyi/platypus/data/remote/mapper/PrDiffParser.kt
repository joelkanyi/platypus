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

import com.joelkanyi.platypus.domain.model.DiffFileStatus
import com.joelkanyi.platypus.domain.model.PrDiff
import com.joelkanyi.platypus.domain.model.PrDiffFile

fun parsePrDiff(raw: String): PrDiff {
    if (raw.isBlank()) return PrDiff(emptyList())
    val files = mutableListOf<PrDiffFile>()
    var current: MutableList<String>? = null
    for (line in raw.lines()) {
        if (line.startsWith("diff --git ")) {
            current?.let { files += toFile(it) }
            current = mutableListOf(line)
        } else {
            current?.add(line)
        }
    }
    current?.let { files += toFile(it) }
    return PrDiff(files)
}

private fun toFile(lines: List<String>): PrDiffFile {
    val header = lines.first()
    val path = pathOf(header, lines)
    val added = lines.count { it.startsWith("+") && !it.startsWith("+++") }
    val removed = lines.count { it.startsWith("-") && !it.startsWith("---") }
    val status = when {
        lines.any { it.startsWith("new file") } -> DiffFileStatus.ADDED
        lines.any { it.startsWith("deleted file") } -> DiffFileStatus.REMOVED
        lines.any { it.startsWith("rename ") } -> DiffFileStatus.RENAMED
        else -> DiffFileStatus.MODIFIED
    }
    return PrDiffFile(path = path, status = status, added = added, removed = removed, lines = lines)
}

private fun pathOf(header: String, lines: List<String>): String {
    lines.firstOrNull { it.startsWith("+++ b/") }?.let { return it.removePrefix("+++ b/") }
    lines.firstOrNull { it.startsWith("--- a/") }?.let { return it.removePrefix("--- a/") }
    val match = GIT_HEADER.find(header)
    return match?.groupValues?.get(2) ?: header.removePrefix("diff --git ")
}

private val GIT_HEADER = Regex("""a/(.+?) b/(.+)$""")
