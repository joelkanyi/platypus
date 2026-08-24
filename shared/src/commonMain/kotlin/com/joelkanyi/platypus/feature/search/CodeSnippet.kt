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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.joelkanyi.platypus.domain.model.CodeLine
import com.joelkanyi.platypus.domain.model.CodeSegment

internal fun highlightedText(line: CodeLine, matchColor: Color): AnnotatedString = buildAnnotatedString {
    line.segments.forEach { segment ->
        if (segment.isMatch) {
            withStyle(SpanStyle(color = matchColor, fontWeight = FontWeight.Bold)) { append(segment.text) }
        } else {
            append(segment.text)
        }
    }
}

internal fun dedent(lines: List<CodeLine>): List<CodeLine> {
    if (lines.isEmpty()) return lines
    val commonIndent = lines.mapNotNull { line ->
        val text = line.segments.joinToString("") { it.text }
        if (text.isBlank()) null else text.indexOfFirst { !it.isWhitespace() }.takeIf { it >= 0 }
    }.minOrNull() ?: 0
    if (commonIndent == 0) return lines
    return lines.map { line -> CodeLine(dropLeading(line.segments, commonIndent)) }
}

private fun dropLeading(segments: List<CodeSegment>, count: Int) = buildList {
    var remaining = count
    for (segment in segments) {
        if (remaining <= 0) {
            add(segment)
        } else {
            val drop = minOf(remaining, segment.text.length)
            remaining -= drop
            add(segment.copy(text = segment.text.substring(drop)))
        }
    }
}
