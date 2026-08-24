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
package com.joelkanyi.platypus.feature.search

import com.joelkanyi.platypus.domain.model.CodeLine
import com.joelkanyi.platypus.domain.model.CodeSegment
import kotlin.test.Test
import kotlin.test.assertEquals

class CodeSnippetTest {

    private fun line(vararg segments: String) = CodeLine(segments.map { CodeSegment(it, isMatch = false) })

    private fun CodeLine.text() = segments.joinToString("") { it.text }

    @Test
    fun stripsTheCommonLeadingIndentAcrossNonBlankLines() {
        val result = dedent(listOf(line("        if (x) {"), line("            call()"), line("        }")))

        assertEquals(listOf("if (x) {", "    call()", "}"), result.map { it.text() })
    }

    @Test
    fun ignoresBlankLinesWhenComputingTheCommonIndent() {
        val result = dedent(listOf(line("    a"), line(""), line("    b")))

        assertEquals(listOf("a", "", "b"), result.map { it.text() })
    }

    @Test
    fun leavesLinesUntouchedWhenThereIsNoCommonIndent() {
        val input = listOf(line("a"), line("    b"))

        assertEquals(input, dedent(input))
    }

    @Test
    fun dropsIndentThatSpansMultipleSegments() {
        val result =
            dedent(listOf(CodeLine(listOf(CodeSegment("  ", isMatch = false), CodeSegment("  x", isMatch = true)))))

        assertEquals(listOf("x"), result.map { it.text() })
    }
}
