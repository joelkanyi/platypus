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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
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

@Immutable
class DiffColors internal constructor(
    val addBackground: Color,
    val deleteBackground: Color,
    val hunkBackground: Color,
    val addSign: Color,
    val deleteSign: Color,
    val contextSign: Color,
    val gutterText: Color,
    val codeText: Color,
    val hunkText: Color,
)

object DiffDefaults {
    val FontSize: TextUnit = 13.sp

    internal const val GUTTER_DIGIT_WIDTH = 9
    internal const val GUTTER_PADDING = 12

    @Composable
    fun colors(
        addBackground: Color = JengaTheme.colors.successContainer,
        deleteBackground: Color = JengaTheme.colors.errorContainer,
        hunkBackground: Color = JengaTheme.colors.surfaceVariant,
        addSign: Color = JengaTheme.colors.success,
        deleteSign: Color = JengaTheme.colors.error,
        contextSign: Color = JengaTheme.colors.textMuted,
        gutterText: Color = JengaTheme.colors.textMuted,
        codeText: Color = JengaTheme.colors.textPrimary,
        hunkText: Color = JengaTheme.colors.textMuted,
    ): DiffColors = DiffColors(
        addBackground = addBackground,
        deleteBackground = deleteBackground,
        hunkBackground = hunkBackground,
        addSign = addSign,
        deleteSign = deleteSign,
        contextSign = contextSign,
        gutterText = gutterText,
        codeText = codeText,
        hunkText = hunkText,
    )
}

/**
 * Renders a unified diff, one row per line. Code rows share a single horizontal scroll so gutters and
 * code stay column-aligned when [wrap] is false; when [wrap] is true the code soft-wraps. Pass
 * [onLineLongPress] to make lines commentable (null leaves the diff read-only, no gesture installed),
 * and [lineContent] to render full-width content under a line (for example inline comment threads),
 * which never scrolls horizontally.
 */
@Composable
fun PlatypusDiffView(
    rows: List<DiffRow>,
    modifier: Modifier = Modifier,
    wrap: Boolean = false,
    colors: DiffColors = DiffDefaults.colors(),
    fontSize: TextUnit = DiffDefaults.FontSize,
    listState: LazyListState = rememberLazyListState(),
    onLineLongPress: ((DiffRow) -> Unit)? = null,
    lineContent: @Composable (DiffRow) -> Unit = {},
) {
    val mono = rememberCodeFontFamily()
    val codeStyle = remember(mono, fontSize) {
        TextStyle(fontFamily = mono, fontSize = fontSize, lineHeight = fontSize * CodeMetrics.LINE_HEIGHT_RATIO)
    }
    val density = LocalDensity.current
    val charWidth = remember(fontSize, density) {
        with(density) { (fontSize.toPx() * CodeMetrics.CHAR_ADVANCE_RATIO).toDp() }
    }
    val gutterWidth = remember(rows) {
        val digits = max(2, (rows.maxOfOrNull { max(it.oldLine ?: 0, it.newLine ?: 0) } ?: 0).toString().length)
        (digits * DiffDefaults.GUTTER_DIGIT_WIDTH + DiffDefaults.GUTTER_PADDING).dp
    }
    val codeWidth = remember(rows, charWidth) {
        charWidth * (rows.maxOfOrNull { it.text.length } ?: 0) + 24.dp
    }
    val horizontalScroll = rememberScrollState()

    LazyColumn(state = listState, modifier = modifier) {
        items(rows.size) { index ->
            val row = rows[index]
            Column {
                DiffLineRow(
                    row = row,
                    codeStyle = codeStyle,
                    colors = colors,
                    gutterWidth = gutterWidth,
                    codeWidth = codeWidth,
                    wrap = wrap,
                    horizontalScroll = horizontalScroll,
                    onLineLongPress = onLineLongPress,
                )
                lineContent(row)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiffLineRow(
    row: DiffRow,
    codeStyle: TextStyle,
    colors: DiffColors,
    gutterWidth: Dp,
    codeWidth: Dp,
    wrap: Boolean,
    horizontalScroll: androidx.compose.foundation.ScrollState,
    onLineLongPress: ((DiffRow) -> Unit)?,
) {
    if (row.type == DiffRowType.HUNK) {
        JengaText(
            text = row.text,
            style = codeStyle,
            color = colors.hunkText,
            softWrap = false,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.hunkBackground)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        return
    }
    val tint = when (row.type) {
        DiffRowType.ADD -> colors.addBackground
        DiffRowType.DELETE -> colors.deleteBackground
        else -> Color.Transparent
    }
    val sign = when (row.type) {
        DiffRowType.ADD -> "+"
        DiffRowType.DELETE -> "-"
        else -> " "
    }
    val signColor = when (row.type) {
        DiffRowType.ADD -> colors.addSign
        DiffRowType.DELETE -> colors.deleteSign
        else -> colors.contextSign
    }
    val clickable = if (onLineLongPress != null) {
        Modifier.combinedClickable(onClick = {}, onLongClick = { onLineLongPress(row) })
    } else {
        Modifier
    }
    if (wrap) {
        Row(modifier = Modifier.fillMaxWidth().background(tint).then(clickable)) {
            Gutter(row.oldLine, gutterWidth, codeStyle, colors.gutterText)
            Gutter(row.newLine, gutterWidth, codeStyle, colors.gutterText)
            JengaText(sign, style = codeStyle, color = signColor, modifier = Modifier.width(16.dp))
            JengaText(
                text = row.text.ifEmpty { " " },
                style = codeStyle,
                color = colors.codeText,
                softWrap = true,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(horizontalScroll).then(clickable)) {
            Row(modifier = Modifier.background(tint)) {
                Gutter(row.oldLine, gutterWidth, codeStyle, colors.gutterText)
                Gutter(row.newLine, gutterWidth, codeStyle, colors.gutterText)
                JengaText(sign, style = codeStyle, color = signColor, modifier = Modifier.width(16.dp))
                JengaText(
                    text = row.text.ifEmpty { " " },
                    style = codeStyle,
                    color = colors.codeText,
                    softWrap = false,
                    maxLines = 1,
                    modifier = Modifier.width(codeWidth),
                )
            }
        }
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
