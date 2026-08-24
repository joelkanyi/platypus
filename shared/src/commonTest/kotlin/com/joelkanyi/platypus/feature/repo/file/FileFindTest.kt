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
package com.joelkanyi.platypus.feature.repo.file

import kotlin.test.Test
import kotlin.test.assertEquals

class FileFindTest {

    private val lines = listOf("fun main() {", "    println(\"hi\")", "    return", "}")

    @Test
    fun blankQueryMatchesNothing() {
        assertEquals(emptyList(), FileFind.matchingLines("", lines))
    }

    @Test
    fun textQueryMatchesEveryContainingLineCaseInsensitively() {
        assertEquals(listOf(0), FileFind.matchingLines("MAIN", lines))
        assertEquals(listOf(1, 2), FileFind.matchingLines("r", lines))
    }

    @Test
    fun colonQueryJumpsToAOneBasedLineClampedToRange() {
        assertEquals(listOf(1), FileFind.matchingLines(":2", lines))
        assertEquals(listOf(3), FileFind.matchingLines(":99", lines))
        assertEquals(listOf(0), FileFind.matchingLines(":0", lines))
    }

    @Test
    fun matchCyclingWrapsAtBothEnds() {
        assertEquals(1, FileFind.nextIndex(current = 0, matchCount = 3))
        assertEquals(0, FileFind.nextIndex(current = 2, matchCount = 3))
        assertEquals(2, FileFind.previousIndex(current = 0, matchCount = 3))
        assertEquals(0, FileFind.previousIndex(current = 0, matchCount = 0))
    }
}
