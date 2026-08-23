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
import com.joelkanyi.platypus.designsystem.relativeTime
import com.joelkanyi.platypus.domain.model.PipelineStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class PipelineUiTest {

    private val red = Color(0xFFFF0000)
    private val amber = Color(0xFFFFAA00)

    @Test
    fun visual_mapping_collapses_states() {
        assertEquals(PipelineVisualStatus.Running, PipelineStatus.IN_PROGRESS.visual())
        assertEquals(PipelineVisualStatus.Success, PipelineStatus.SUCCESSFUL.visual())
        assertEquals(PipelineVisualStatus.Failed, PipelineStatus.FAILED.visual())
        assertEquals(PipelineVisualStatus.Failed, PipelineStatus.ERROR.visual())
        assertEquals(PipelineVisualStatus.Paused, PipelineStatus.PAUSED.visual())
        assertEquals(PipelineVisualStatus.Neutral, PipelineStatus.PENDING.visual())
        assertEquals(PipelineVisualStatus.Neutral, PipelineStatus.STOPPED.visual())
    }

    @Test
    fun classify_detects_error_and_warn() {
        assertEquals(LogLevel.ERROR, classifyLogLine("Task failed with exception"))
        assertEquals(LogLevel.WARN, classifyLogLine("warning: deprecated API"))
        assertEquals(LogLevel.NORMAL, classifyLogLine("Downloading dependencies"))
    }

    @Test
    fun annotate_strips_ansi_and_keeps_text() {
        val esc = Char(27)
        val raw = esc + "[31mBUILD FAILED" + esc + "[0m"
        val view = annotateLog(raw, red, amber, maxLines = 100)
        assertEquals("BUILD FAILED", view.lines.single().text)
        assertEquals(listOf(0), view.errorLines)
    }

    @Test
    fun annotate_does_not_eat_bracket_text() {
        val view = annotateLog("[INFO] building module", red, amber, maxLines = 100)
        assertEquals("[INFO] building module", view.lines.single().text)
    }

    @Test
    fun annotate_truncates_to_last_lines() {
        val raw = (1..10).joinToString("\n") { "line $it" }
        val view = annotateLog(raw, red, amber, maxLines = 3)
        assertTrue(view.truncated)
        assertEquals(3, view.lines.size)
        assertEquals("line 10", view.lines.last().text)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun relative_time_buckets() {
        val then = Instant.parse("2026-08-23T09:00:00Z").toEpochMilliseconds()
        assertEquals("just now", relativeTime("2026-08-23T09:00:00Z", then + 10_000L))
        assertEquals("5m ago", relativeTime("2026-08-23T09:00:00Z", then + 5 * 60_000L))
        assertEquals("2h ago", relativeTime("2026-08-23T09:00:00Z", then + 2 * 3_600_000L))
        assertEquals("3d ago", relativeTime("2026-08-23T09:00:00Z", then + 3 * 86_400_000L))
        assertEquals("", relativeTime("", then))
    }
}
