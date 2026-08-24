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
package com.joelkanyi.platypus.data.remote.network

import com.joelkanyi.platypus.data.remote.dto.PageDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PagingTest {

    @Test
    fun followsNextLinksAcrossPages() = runTest {
        val result = collectPaged(
            firstPage = { PageDto(values = listOf(1, 2), next = "page-2") },
            nextPage = { PageDto(values = listOf(3, 4), next = null) },
        )

        assertEquals(listOf(1, 2, 3, 4), result)
    }

    @Test
    fun stopsAtMaxPages() = runTest {
        var fetched = 0
        val result = collectPaged(
            maxPages = 2,
            firstPage = {
                fetched++
                PageDto(values = listOf(1), next = "more")
            },
            nextPage = {
                fetched++
                PageDto(values = listOf(1), next = "more")
            },
        )

        assertEquals(2, fetched)
        assertEquals(2, result.size)
    }
}
