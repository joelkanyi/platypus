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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme

@Immutable
data class Crumb(val label: String, val path: String)

/** Builds a repo/dir/subdir crumb trail from a slash-separated [path] rooted at [rootLabel]. */
fun crumbsFor(rootLabel: String, path: String): List<Crumb> {
    val crumbs = mutableListOf(Crumb(rootLabel, ""))
    val segments = path.trim('/').split('/').filter { it.isNotBlank() }
    var running = ""
    for (segment in segments) {
        running = if (running.isEmpty()) segment else "$running/$segment"
        crumbs += Crumb(segment, running)
    }
    return crumbs
}

@Composable
fun PlatypusBreadcrumb(crumbs: List<Crumb>, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = JengaTheme.colors
    val style = JengaTheme.typography.bodySmall
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        crumbs.forEachIndexed { index, crumb ->
            val isLast = index == crumbs.lastIndex
            JengaText(
                text = crumb.label,
                style = if (isLast) style.copy(fontWeight = FontWeight.SemiBold) else style,
                color = if (isLast) colors.textPrimary else colors.brand,
                maxLines = 1,
                modifier = if (isLast) Modifier else Modifier.clickable { onNavigate(crumb.path) },
            )
            if (!isLast) {
                JengaText(text = "  /  ", style = style, color = colors.textMuted, maxLines = 1)
            }
        }
    }
}
