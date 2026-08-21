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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun PlatypusDiffView(
    lines: List<String>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val mono = rememberCodeFontFamily()
    val colors = JengaTheme.colors
    val style = remember(mono) { TextStyle(fontFamily = mono, fontSize = 12.sp, lineHeight = 18.sp) }

    LazyColumn(state = listState, modifier = modifier) {
        items(lines.size) { index ->
            val line = lines[index]
            val background: Color
            val foreground: Color
            when {
                line.startsWith("@@") -> {
                    background = colors.infoContainer
                    foreground = colors.onInfoContainer
                }
                line.startsWith("+++") ||
                    line.startsWith("---") ||
                    line.startsWith("diff ") ||
                    line.startsWith("index ") -> {
                    background = colors.surfaceVariant
                    foreground = colors.textMuted
                }
                line.startsWith("+") -> {
                    background = colors.successContainer
                    foreground = colors.onSuccessContainer
                }
                line.startsWith("-") -> {
                    background = colors.errorContainer
                    foreground = colors.onErrorContainer
                }
                else -> {
                    background = Color.Transparent
                    foreground = colors.textSecondary
                }
            }
            JengaText(
                text = line.ifEmpty { " " },
                style = style,
                color = foreground,
                softWrap = false,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 1.dp),
            )
        }
    }
}
