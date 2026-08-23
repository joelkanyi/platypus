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

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun shortDate(isoTimestamp: String): String = isoTimestamp.substringBefore('T')

@OptIn(ExperimentalTime::class)
fun relativeTime(isoTimestamp: String, nowEpochMs: Long = Clock.System.now().toEpochMilliseconds()): String {
    if (isoTimestamp.isBlank()) return ""
    val thenMs = runCatching { Instant.parse(isoTimestamp).toEpochMilliseconds() }.getOrNull()
        ?: return shortDate(isoTimestamp)
    val diff = nowEpochMs - thenMs
    return when {
        diff < 0 -> "just now"
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000L}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
        diff < 604_800_000L -> "${diff / 86_400_000L}d ago"
        else -> shortDate(isoTimestamp)
    }
}

fun formatDuration(seconds: Long): String {
    if (seconds <= 0) return "—"
    val minutes = seconds / 60
    val secs = seconds % 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${secs}s"
        else -> "${secs}s"
    }
}

fun formatByteSize(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    bytes < 1_024 * 1_024 -> "${bytes / 1_024} KB"
    else -> {
        val mb = bytes.toDouble() / (1_024 * 1_024)
        val rounded = (mb * 10).toLong() / 10.0
        "$rounded MB"
    }
}
