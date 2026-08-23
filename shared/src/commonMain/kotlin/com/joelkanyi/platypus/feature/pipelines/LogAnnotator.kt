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
package com.joelkanyi.platypus.feature.pipelines

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

enum class LogLevel { ERROR, WARN, NORMAL }

data class LogView(val lines: List<AnnotatedString>, val errorLines: List<Int>, val truncated: Boolean)

private val ANSI = Regex(Char(27) + "\\[[0-9;?]*[ -/]*[@-~]")
private val ERROR_HINT = Regex("""\b(error|errors|failed|failure|exception|fatal|err!)\b""", RegexOption.IGNORE_CASE)
private val WARN_HINT = Regex("""\b(warn|warning|deprecated)\b""", RegexOption.IGNORE_CASE)

fun classifyLogLine(line: String): LogLevel = when {
    ERROR_HINT.containsMatchIn(line) -> LogLevel.ERROR
    WARN_HINT.containsMatchIn(line) -> LogLevel.WARN
    else -> LogLevel.NORMAL
}

fun annotateLog(raw: String, errorColor: Color, warnColor: Color, maxLines: Int): LogView {
    val all = raw.split('\n')
    val truncated = all.size > maxLines
    val kept = if (truncated) all.subList(all.size - maxLines, all.size) else all
    val lines = ArrayList<AnnotatedString>(kept.size)
    val errorLines = ArrayList<Int>()
    kept.forEachIndexed { index, rawLine ->
        val clean = ANSI.replace(rawLine, "")
        when (classifyLogLine(clean)) {
            LogLevel.ERROR -> {
                errorLines += index
                lines += buildAnnotatedString { withStyle(SpanStyle(color = errorColor)) { append(clean) } }
            }
            LogLevel.WARN -> lines += buildAnnotatedString { withStyle(SpanStyle(color = warnColor)) { append(clean) } }
            LogLevel.NORMAL -> lines += AnnotatedString(clean)
        }
    }
    return LogView(lines = lines, errorLines = errorLines, truncated = truncated)
}
