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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlin.math.max

/**
 * Renders code as one lazy item per line, so long files virtualize. When [wrap] is true the code
 * soft-wraps to the screen width; when false it scrolls horizontally as a single unit (the whole block
 * moves together, gutter included), sized to the widest line via the monospace character advance.
 */
@Composable
fun PlatypusCodeView(
    lines: List<AnnotatedString>,
    wrap: Boolean,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    fontSize: TextUnit = 13.sp,
    gutterColor: Color = JengaTheme.colors.textMuted,
    highlightedLine: Int? = null,
    lineHighlightColor: Color = JengaTheme.colors.brand.copy(alpha = 0.14f),
) {
    val mono = rememberCodeFontFamily()
    val codeStyle = remember(mono, fontSize) {
        TextStyle(fontFamily = mono, fontSize = fontSize, lineHeight = fontSize * CodeMetrics.LINE_HEIGHT_RATIO)
    }
    val digits = max(2, lines.size.toString().length)
    val gutterWidth = (digits * 9 + 20).dp

    if (wrap) {
        LazyColumn(state = listState, modifier = modifier) {
            codeLines(lines, codeStyle, gutterWidth, gutterColor, highlightedLine, lineHighlightColor, wrap = true)
        }
    } else {
        val density = LocalDensity.current
        val charWidth = with(density) { (fontSize.toPx() * CodeMetrics.CHAR_ADVANCE_RATIO).toDp() }
        val maxChars = remember(lines) { lines.maxOfOrNull { it.length } ?: 0 }
        val contentWidth = gutterWidth + charWidth * maxChars + 24.dp

        Box(modifier = modifier.horizontalScroll(rememberScrollState())) {
            LazyColumn(state = listState, modifier = Modifier.width(contentWidth).fillMaxHeight()) {
                codeLines(lines, codeStyle, gutterWidth, gutterColor, highlightedLine, lineHighlightColor, wrap = false)
            }
        }
    }
}

private fun LazyListScope.codeLines(
    lines: List<AnnotatedString>,
    codeStyle: TextStyle,
    gutterWidth: Dp,
    gutterColor: Color,
    highlightedLine: Int?,
    lineHighlightColor: Color,
    wrap: Boolean,
) {
    itemsIndexed(lines) { index, line ->
        val rowModifier = if (index == highlightedLine) {
            Modifier.fillMaxWidth().background(lineHighlightColor)
        } else {
            Modifier.fillMaxWidth()
        }
        Row(modifier = rowModifier) {
            JengaText(
                text = (index + 1).toString(),
                style = codeStyle,
                color = gutterColor,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(gutterWidth).padding(end = 12.dp),
            )
            JengaText(
                text = line,
                style = codeStyle,
                color = JengaTheme.colors.textPrimary,
                softWrap = wrap,
                maxLines = if (wrap) Int.MAX_VALUE else 1,
                modifier = if (wrap) Modifier.weight(1f) else Modifier,
            )
        }
    }
}
