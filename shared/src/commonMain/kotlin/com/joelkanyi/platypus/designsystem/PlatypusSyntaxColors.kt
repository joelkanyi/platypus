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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.joelkanyi.platypus.syntax.LineHighlighter
import com.joelkanyi.platypus.syntax.SyntaxPalette
import com.joelkanyi.platypus.syntax.TokenRole
import io.github.joelkanyi.jenga.theme.JengaTheme

@Immutable
data class PlatypusSyntaxColors(
    val ground: Color,
    val foreground: Color,
    val gutter: Color,
    val roles: Map<TokenRole, Color>,
)

@Composable
fun rememberSyntaxColors(): PlatypusSyntaxColors {
    val dark = JengaTheme.colors.background.luminance() < 0.5f
    return remember(dark) {
        if (dark) {
            PlatypusSyntaxColors(
                ground = Color(SyntaxPalette.DARK_GROUND),
                foreground = Color(SyntaxPalette.DARK_FOREGROUND),
                gutter = Color(SyntaxPalette.DARK_GUTTER),
                roles = SyntaxPalette.dark.mapValues { Color(it.value) },
            )
        } else {
            PlatypusSyntaxColors(
                ground = Color(SyntaxPalette.LIGHT_GROUND),
                foreground = Color(SyntaxPalette.LIGHT_FOREGROUND),
                gutter = Color(SyntaxPalette.LIGHT_GUTTER),
                roles = SyntaxPalette.light.mapValues { Color(it.value) },
            )
        }
    }
}

fun highlightLine(line: String, highlighter: LineHighlighter, colors: PlatypusSyntaxColors): AnnotatedString =
    buildAnnotatedString {
        append(line)
        addStyle(SpanStyle(color = colors.foreground), 0, line.length)
        highlighter.tokenize(line).forEach { token ->
            val color = colors.roles[token.role] ?: colors.foreground
            addStyle(SpanStyle(color = color), token.start, token.end.coerceAtMost(line.length))
        }
    }
