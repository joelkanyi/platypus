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
package com.joelkanyi.platypus.core.search

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FuzzyMatchTest {

    private val paths = listOf(
        "app/src/main/kotlin/MainActivity.kt",
        "app/src/main/kotlin/Repository.kt",
        "build.gradle.kts",
        "docs/README.md",
    )

    @Test
    fun matchesSubsequenceAcrossPath() {
        val results = fuzzyFilter("mainact", paths)
        assertTrue(results.first().endsWith("MainActivity.kt"))
    }

    @Test
    fun ranksContiguousFileNameMatchFirst() {
        val results = fuzzyFilter("repository", paths)
        assertEquals("app/src/main/kotlin/Repository.kt", results.first())
    }

    @Test
    fun returnsNullWhenNotASubsequence() {
        assertNull(fuzzyScore("zzz", "build.gradle.kts"))
    }

    @Test
    fun blankQueryYieldsNoResults() {
        assertTrue(fuzzyFilter("", paths).isEmpty())
    }
}
