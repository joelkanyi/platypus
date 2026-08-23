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
package com.joelkanyi.platypus.data.remote.mapper

import com.joelkanyi.platypus.domain.model.DiffFileStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class PrDiffParserTest {

    private val raw = """
        diff --git a/src/Main.kt b/src/Main.kt
        index 111..222 100644
        --- a/src/Main.kt
        +++ b/src/Main.kt
        @@ -1,3 +1,4 @@
         fun main() {
        -    println("old")
        +    println("new")
        +    println("added")
         }
        diff --git a/README.md b/README.md
        new file mode 100644
        --- /dev/null
        +++ b/README.md
        @@ -0,0 +1,2 @@
        +# Title
        +body
    """.trimIndent()

    @Test
    fun splitsFilesAndCountsChanges() {
        val diff = parsePrDiff(raw)
        assertEquals(2, diff.files.size)

        val main = diff.files.first { it.path == "src/Main.kt" }
        assertEquals(DiffFileStatus.MODIFIED, main.status)
        assertEquals(2, main.added)
        assertEquals(1, main.removed)

        val readme = diff.files.first { it.path == "README.md" }
        assertEquals(DiffFileStatus.ADDED, readme.status)
        assertEquals(2, readme.added)
        assertEquals(0, readme.removed)

        assertEquals(4, diff.totalAdded)
        assertEquals(1, diff.totalRemoved)
    }

    @Test
    fun emptyDiffYieldsNoFiles() {
        assertEquals(0, parsePrDiff("").files.size)
    }
}
