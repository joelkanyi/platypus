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

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.joelkanyi.platypus.designsystem.formatDuration
import com.joelkanyi.platypus.designsystem.relativeTime
import com.joelkanyi.platypus.domain.model.Pipeline
import com.joelkanyi.platypus.domain.model.PipelineStatus
import io.github.joelkanyi.jenga.component.badge.JengaBadgeTone
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.progress.JengaCircularProgressIndeterminate
import io.github.joelkanyi.jenga.component.status.JengaStatusPill
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

enum class PipelineVisualStatus { Running, Success, Failed, Paused, Neutral }

fun PipelineStatus.visual(): PipelineVisualStatus = when (this) {
    PipelineStatus.IN_PROGRESS -> PipelineVisualStatus.Running
    PipelineStatus.PENDING -> PipelineVisualStatus.Neutral
    PipelineStatus.SUCCESSFUL -> PipelineVisualStatus.Success
    PipelineStatus.FAILED, PipelineStatus.ERROR -> PipelineVisualStatus.Failed
    PipelineStatus.PAUSED -> PipelineVisualStatus.Paused
    PipelineStatus.STOPPED, PipelineStatus.SKIPPED, PipelineStatus.UNKNOWN -> PipelineVisualStatus.Neutral
}

fun PipelineStatus.label(): String = when (this) {
    PipelineStatus.PENDING -> "Queued"
    PipelineStatus.IN_PROGRESS -> "Running"
    PipelineStatus.PAUSED -> "Paused"
    PipelineStatus.SUCCESSFUL -> "Success"
    PipelineStatus.FAILED -> "Failed"
    PipelineStatus.ERROR -> "Error"
    PipelineStatus.STOPPED -> "Stopped"
    PipelineStatus.SKIPPED -> "Skipped"
    PipelineStatus.UNKNOWN -> "Unknown"
}

fun PipelineVisualStatus.tone(): JengaBadgeTone = when (this) {
    PipelineVisualStatus.Running -> JengaBadgeTone.Info
    PipelineVisualStatus.Success -> JengaBadgeTone.Success
    PipelineVisualStatus.Failed -> JengaBadgeTone.Error
    PipelineVisualStatus.Paused -> JengaBadgeTone.Warning
    PipelineVisualStatus.Neutral -> JengaBadgeTone.Neutral
}

@Composable
fun PipelineVisualStatus.color(): Color = when (this) {
    PipelineVisualStatus.Running -> JengaTheme.colors.info
    PipelineVisualStatus.Success -> JengaTheme.colors.success
    PipelineVisualStatus.Failed -> JengaTheme.colors.error
    PipelineVisualStatus.Paused -> JengaTheme.colors.warning
    PipelineVisualStatus.Neutral -> JengaTheme.colors.textMuted
}

@Composable
fun PipelineStatusIcon(status: PipelineStatus, size: Dp = 16.dp) {
    val visual = status.visual()
    val color = visual.color()
    when (visual) {
        PipelineVisualStatus.Running -> JengaCircularProgressIndeterminate(
            size = size,
            strokeWidth = 2.dp,
            color = color,
        )
        PipelineVisualStatus.Success ->
            JengaIcon(JengaIcons.CheckCircle, contentDescription = "Success", tint = color, size = size)
        PipelineVisualStatus.Failed -> Box(
            modifier = Modifier.size(size).border(1.5.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            JengaIcon(JengaIcons.Close, contentDescription = "Failed", tint = color, size = size - 5.dp)
        }
        PipelineVisualStatus.Paused ->
            JengaIcon(JengaIcons.Clock, contentDescription = "Paused", tint = color, size = size)
        PipelineVisualStatus.Neutral ->
            JengaIcon(JengaIcons.Clock, contentDescription = status.label(), tint = color, size = size)
    }
}

@Composable
fun PipelineStatusPill(status: PipelineStatus, label: String = status.label()) {
    JengaStatusPill(
        label = label,
        tone = status.visual().tone(),
        loading =
        status.visual() == PipelineVisualStatus.Running,
    )
}

fun triggerLabel(pipeline: Pipeline): String = when {
    pipeline.pullRequestId != null -> "PR #${pipeline.pullRequestId}"
    pipeline.triggerName.equals("PUSH", ignoreCase = true) -> "Push"
    pipeline.triggerName.equals("MANUAL", ignoreCase = true) -> "Manual"
    pipeline.triggerName.equals("SCHEDULE", ignoreCase = true) -> "Scheduled"
    pipeline.triggerName.isNotBlank() -> pipeline.triggerName
    else -> "Pipeline"
}

fun pipelineTitle(pipeline: Pipeline): String {
    val message = pipeline.commitMessage?.substringBefore('\n')?.takeIf { it.isNotBlank() }
    if (message != null) return message
    val ref = pipeline.refName?.takeIf { it.isNotBlank() }
    val short = pipeline.commitHash?.take(7)
    return when {
        ref != null && short != null -> "$ref@$short"
        ref != null -> "${triggerLabel(pipeline)} on $ref"
        else -> triggerLabel(pipeline)
    }
}

fun pipelineMetaLine(pipeline: Pipeline): String = buildList {
    add("#${pipeline.buildNumber}")
    pipeline.refName?.takeIf { it.isNotBlank() }?.let { add(it) }
    pipeline.commitHash?.takeIf { it.isNotBlank() }?.let { add(it.take(7)) }
    pipeline.creatorName.takeIf { it.isNotBlank() }?.let { add(it) }
    if (pipeline.durationSeconds > 0) add(formatDuration(pipeline.durationSeconds))
}.joinToString("  ·  ")

@Composable
private fun tabularCaption() = JengaTheme.typography.caption.copy(fontFeatureSettings = "tnum")

@Composable
fun PipelineRow(pipeline: Pipeline, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = JengaTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            PipelineStatusIcon(pipeline.status, size = 16.dp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = spacing.md)) {
            JengaText(
                text = pipelineTitle(pipeline),
                style = JengaTheme.typography.titleSmall,
                color = JengaTheme.colors.textPrimary,
                maxLines = 1,
            )
            JengaText(
                text = pipelineMetaLine(pipeline),
                style = tabularCaption(),
                color = JengaTheme.colors.textSecondary,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (pipeline.status.visual() == PipelineVisualStatus.Running) {
            LiveElapsedText(
                startIso = pipeline.createdOn,
                color = JengaTheme.colors.info,
                modifier = Modifier.padding(start = spacing.sm),
            )
        } else {
            JengaText(
                text = relativeTime(pipeline.createdOn),
                style = tabularCaption(),
                color = JengaTheme.colors.textMuted,
                maxLines = 1,
                modifier = Modifier.padding(start = spacing.sm),
            )
        }
    }
}

@Composable
fun PipelineMetaRow(label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val spacing = JengaTheme.spacing
    val rowModifier = if (onClick !=
        null
    ) {
        modifier.fillMaxWidth().clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }
    Row(
        modifier = rowModifier.padding(vertical = spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JengaText(
            text = label,
            style = JengaTheme.typography.caption,
            color = JengaTheme.colors.textMuted,
            modifier = Modifier.weight(0.4f),
        )
        JengaText(
            text = value,
            style = JengaTheme.typography.bodySmall,
            color = if (onClick != null) JengaTheme.colors.brand else JengaTheme.colors.textPrimary,
            maxLines = 2,
            modifier = Modifier.weight(0.6f),
        )
        if (onClick != null) {
            JengaIcon(
                JengaIcons.ChevronRight,
                contentDescription = null,
                tint = JengaTheme.colors.textMuted,
                size = 16.dp,
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun LiveElapsedText(startIso: String, color: Color, modifier: Modifier = Modifier) {
    val startMs = remember(startIso) {
        runCatching { Instant.parse(startIso).toEpochMilliseconds() }.getOrNull()
    }
    val elapsed by produceState(initialValue = 0L, startMs) {
        if (startMs == null) return@produceState
        while (true) {
            value = (Clock.System.now().toEpochMilliseconds() - startMs).coerceAtLeast(0L) / 1000L
            delay(1000L)
        }
    }
    JengaText(
        text = if (startMs == null) "" else formatDuration(elapsed),
        style = JengaTheme.typography.caption.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Medium),
        color = color,
        maxLines = 1,
        modifier = modifier,
    )
}
