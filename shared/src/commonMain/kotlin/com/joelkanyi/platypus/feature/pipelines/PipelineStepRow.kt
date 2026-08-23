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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.joelkanyi.platypus.designsystem.formatDuration
import com.joelkanyi.platypus.domain.model.PipelineStep
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun PipelineStepRow(
    step: PipelineStep,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    val connector = JengaTheme.colors.border
    val surface = JengaTheme.colors.background
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
                .drawBehind {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val strokePx = 2.dp.toPx()
                    val nodeRadius = 12.dp.toPx()
                    if (!isFirst) {
                        drawLine(
                            color = connector,
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, centerY - nodeRadius),
                            strokeWidth = strokePx,
                        )
                    }
                    if (!isLast) {
                        drawLine(
                            color = connector,
                            start = Offset(centerX, centerY + nodeRadius),
                            end = Offset(centerX, size.height),
                            strokeWidth = strokePx,
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier.size(22.dp).clip(CircleShape).background(surface),
                contentAlignment = Alignment.Center,
            ) {
                PipelineStatusIcon(step.status, size = 16.dp)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = spacing.md, top = spacing.md, bottom = spacing.md),
        ) {
            JengaText(
                text = step.name,
                style = JengaTheme.typography.bodyMedium,
                color = JengaTheme.colors.textPrimary,
                maxLines = 1,
            )
            step.testSummary?.let { summary ->
                JengaText(
                    text = buildString {
                        append("${summary.passed} passed")
                        if (summary.failed > 0) append(" · ${summary.failed} failed")
                        if (summary.skipped > 0) append(" · ${summary.skipped} skipped")
                    },
                    style = JengaTheme.typography.caption,
                    color = if (summary.failed > 0) JengaTheme.colors.error else JengaTheme.colors.textMuted,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (step.status.visual() == PipelineVisualStatus.Running) {
            LiveElapsedText(
                startIso = step.startedOn.orEmpty(),
                color = JengaTheme.colors.info,
                modifier = Modifier.padding(start = spacing.sm),
            )
        } else if (step.durationSeconds > 0) {
            JengaText(
                text = formatDuration(step.durationSeconds),
                style = JengaTheme.typography.caption.copy(fontFeatureSettings = "tnum"),
                color = JengaTheme.colors.textMuted,
                maxLines = 1,
                modifier = Modifier.padding(start = spacing.sm),
            )
        }
        JengaIcon(
            JengaIcons.ChevronRight,
            contentDescription = null,
            tint = JengaTheme.colors.textMuted,
            size = 16.dp,
            modifier = Modifier.padding(start = spacing.xs),
        )
    }
}
