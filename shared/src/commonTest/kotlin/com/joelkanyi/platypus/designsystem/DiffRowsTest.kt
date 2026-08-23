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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffRowsTest {

    @Test
    fun accountsForLineNumbersAcrossAHunk() {
        val rows = parseDiffRows(
            listOf(
                "@@ -1,3 +1,4 @@",
                " context",
                "-removed",
                "+added",
                " tail",
            ),
        )
        val hunk = rows[0]
        assertEquals(DiffRowType.HUNK, hunk.type)

        val context = rows[1]
        assertEquals(DiffRowType.CONTEXT, context.type)
        assertEquals(1, context.oldLine)
        assertEquals(1, context.newLine)

        val removed = rows[2]
        assertEquals(DiffRowType.DELETE, removed.type)
        assertEquals(2, removed.oldLine)
        assertEquals(null, removed.newLine)

        val added = rows[3]
        assertEquals(DiffRowType.ADD, added.type)
        assertEquals(null, added.oldLine)
        assertEquals(2, added.newLine)

        val tail = rows[4]
        assertEquals(3, tail.oldLine)
        assertEquals(3, tail.newLine)
    }

    @Test
    fun dropsGitMetadataLines() {
        val rows = parseDiffRows(
            listOf(
                "diff --git a/File.kt b/File.kt",
                "index abc..def 100644",
                "--- a/File.kt",
                "+++ b/File.kt",
                "@@ -1 +1 @@",
                "+only line",
            ),
        )
        assertTrue(rows.none { it.text.startsWith("diff ") || it.text.startsWith("index ") })
        assertEquals(DiffRowType.HUNK, rows.first().type)
        assertEquals(DiffRowType.ADD, rows.last().type)
    }

    @Test
    fun resetsCountersOnEachHunk() {
        val rows = parseDiffRows(
            listOf(
                "@@ -10,1 +10,1 @@",
                " a",
                "@@ -50,1 +60,1 @@",
                " b",
            ),
        )
        assertEquals(10, rows[1].newLine)
        assertEquals(50, rows[3].oldLine)
        assertEquals(60, rows[3].newLine)
    }

    @Test
    fun preservesEmptyContextLine() {
        val rows = parseDiffRows(listOf("@@ -1,1 +1,1 @@", " "))
        assertEquals(DiffRowType.CONTEXT, rows[1].type)
        assertEquals("", rows[1].text)
    }
}
