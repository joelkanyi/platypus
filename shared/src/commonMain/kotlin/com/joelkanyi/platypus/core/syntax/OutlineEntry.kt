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
package com.joelkanyi.platypus.core.syntax

data class OutlineEntry(val line: Int, val label: String)

/**
 * A heuristic, regex-based outline of a file's top-level declarations. It is deliberately labelled
 * "Outline" in the UI and never presented as jump-to-definition, since Bitbucket exposes no symbol index.
 */
fun outlineOf(fileName: String, lines: List<String>): List<OutlineEntry> {
    val patterns = patternsFor(fileName)
    if (patterns.isEmpty()) return emptyList()
    val out = mutableListOf<OutlineEntry>()
    lines.forEachIndexed { index, line ->
        for (pattern in patterns) {
            val match = pattern.find(line) ?: continue
            val keyword = match.groupValues.getOrNull(1).orEmpty().trim()
            val name = match.groupValues.lastOrNull().orEmpty()
            if (name.isNotBlank()) {
                out += OutlineEntry(index, listOf(keyword, name).filter { it.isNotBlank() }.joinToString(" "))
                break
            }
        }
    }
    return out
}

private fun patternsFor(fileName: String): List<Regex> = when (fileName.substringAfterLast('.', "").lowercase()) {
    "kt", "kts" -> listOf(Regex("""^\s*(?:[\w@]+\s+)*(fun|class|interface|object|enum class)\s+`?(\w+)"""))
    "java" -> listOf(Regex("""^\s*(?:[\w@]+\s+)*(class|interface|enum|record)\s+(\w+)"""))
    "js", "jsx", "ts", "tsx", "mjs", "cjs" -> listOf(
        Regex("""^\s*(?:export\s+)?(?:default\s+)?(function|class|const|interface|type)\s+(\w+)"""),
    )
    "py" -> listOf(Regex("""^\s*(def|class)\s+(\w+)"""))
    "swift" -> listOf(Regex("""^\s*(?:[\w@]+\s+)*(func|class|struct|enum|protocol|extension)\s+(\w+)"""))
    "go" -> listOf(Regex("""^\s*(func|type)\s+(\w+)"""))
    "rs" -> listOf(Regex("""^\s*(?:pub\s+)?(fn|struct|enum|trait|impl)\s+(\w+)"""))
    else -> emptyList()
}
