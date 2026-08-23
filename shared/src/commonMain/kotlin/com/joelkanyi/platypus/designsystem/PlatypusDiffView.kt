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
package com.joelkanyi.platypus.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlin.math.max

enum class DiffRowType { HUNK, CONTEXT, ADD, DELETE }

data class DiffRow(val type: DiffRowType, val oldLine: Int?, val newLine: Int?, val text: String)

fun parseDiffRows(lines: List<String>): List<DiffRow> {
    val rows = mutableListOf<DiffRow>()
    var oldLine = 0
    var newLine = 0
    for (line in lines) {
        when {
            line.startsWith("@@") -> {
                val match = HUNK_HEADER.find(line)
                oldLine = match?.groupValues?.get(1)?.toIntOrNull() ?: oldLine
                newLine = match?.groupValues?.get(2)?.toIntOrNull() ?: newLine
                rows += DiffRow(DiffRowType.HUNK, null, null, line)
            }
            isMetadata(line) -> Unit
            line.startsWith("+") -> {
                rows += DiffRow(DiffRowType.ADD, null, newLine, line.substring(1))
                newLine++
            }
            line.startsWith("-") -> {
                rows += DiffRow(DiffRowType.DELETE, oldLine, null, line.substring(1))
                oldLine++
            }
            else -> {
                rows += DiffRow(DiffRowType.CONTEXT, oldLine, newLine, line.removePrefix(" "))
                oldLine++
                newLine++
            }
        }
    }
    return rows
}

/**
 * Renders a unified diff one lazy row per line, matching [PlatypusCodeView]: when [wrap] is true the
 * code soft-wraps; when false the whole block (gutters included) scrolls horizontally as a single unit,
 * sized to the widest line via the monospace character advance. Add/remove rows carry a tint; git
 * metadata lines are hidden and hunk headers styled.
 */
@Composable
fun PlatypusDiffView(
    lines: List<String>,
    wrap: Boolean,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val rows = remember(lines) { parseDiffRows(lines) }
    val mono = rememberCodeFontFamily()
    val codeStyle = remember(mono) { TextStyle(fontFamily = mono, fontSize = 13.sp, lineHeight = 13.sp * 1.5f) }
    val maxLineDigits = max(2, (rows.maxOfOrNull { max(it.oldLine ?: 0, it.newLine ?: 0) } ?: 0).toString().length)
    val gutterWidth = (maxLineDigits * 9 + 12).dp

    if (wrap) {
        LazyColumn(state = listState, modifier = modifier) {
            diffRows(rows, codeStyle, gutterWidth, wrap = true)
        }
    } else {
        val density = LocalDensity.current
        val charWidth = with(density) { (13.sp.toPx() * 0.6f).toDp() }
        val maxChars = remember(rows) { rows.maxOfOrNull { it.text.length } ?: 0 }
        val contentWidth = gutterWidth * 2 + 16.dp + charWidth * maxChars + 24.dp
        Box(modifier = modifier.horizontalScroll(rememberScrollState())) {
            LazyColumn(state = listState, modifier = Modifier.width(contentWidth).fillMaxHeight()) {
                diffRows(rows, codeStyle, gutterWidth, wrap = false)
            }
        }
    }
}

private fun LazyListScope.diffRows(rows: List<DiffRow>, codeStyle: TextStyle, gutterWidth: Dp, wrap: Boolean) {
    items(rows.size) { index ->
        val row = rows[index]
        DiffLine(row = row, codeStyle = codeStyle, gutterWidth = gutterWidth, wrap = wrap)
    }
}

@Composable
private fun DiffLine(row: DiffRow, codeStyle: TextStyle, gutterWidth: Dp, wrap: Boolean) {
    val colors = JengaTheme.colors
    if (row.type == DiffRowType.HUNK) {
        JengaText(
            text = row.text,
            style = codeStyle,
            color = colors.textMuted,
            softWrap = false,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        return
    }
    val rowBackground = when (row.type) {
        DiffRowType.ADD -> colors.successContainer
        DiffRowType.DELETE -> colors.errorContainer
        else -> Color.Transparent
    }
    val sign = when (row.type) {
        DiffRowType.ADD -> "+"
        DiffRowType.DELETE -> "-"
        else -> " "
    }
    val signColor = when (row.type) {
        DiffRowType.ADD -> colors.success
        DiffRowType.DELETE -> colors.error
        else -> colors.textMuted
    }
    Row(modifier = Modifier.fillMaxWidth().background(rowBackground)) {
        Gutter(row.oldLine, gutterWidth, codeStyle, colors.textMuted)
        Gutter(row.newLine, gutterWidth, codeStyle, colors.textMuted)
        JengaText(
            text = sign,
            style = codeStyle,
            color = signColor,
            modifier = Modifier.width(16.dp),
        )
        JengaText(
            text = row.text.ifEmpty { " " },
            style = codeStyle,
            color = colors.textPrimary,
            softWrap = wrap,
            maxLines = if (wrap) Int.MAX_VALUE else 1,
            modifier = if (wrap) Modifier.weight(1f).padding(end = 12.dp) else Modifier.padding(end = 12.dp),
        )
    }
}

@Composable
private fun Gutter(number: Int?, gutterWidth: Dp, style: TextStyle, color: Color) {
    JengaText(
        text = number?.toString().orEmpty(),
        style = style,
        color = color,
        textAlign = TextAlign.End,
        softWrap = false,
        maxLines = 1,
        modifier = Modifier.width(gutterWidth).padding(end = 6.dp),
    )
}

private fun isMetadata(line: String): Boolean = line.startsWith("diff ") ||
    line.startsWith("index ") ||
    line.startsWith("--- ") ||
    line.startsWith("+++ ") ||
    line.startsWith("new file") ||
    line.startsWith("deleted file") ||
    line.startsWith("old mode") ||
    line.startsWith("new mode") ||
    line.startsWith("similarity ") ||
    line.startsWith("rename ") ||
    line.startsWith("copy ") ||
    line.startsWith("\\ No newline")

private val HUNK_HEADER = Regex("""@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@""")
