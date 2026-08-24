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

const val DEFAULT_MAX_PAGES = 10

// Follows each page's `next` link: Bitbucket page counts are unreliable, so never trust `size`.
internal suspend fun <T> collectPaged(
    maxPages: Int = DEFAULT_MAX_PAGES,
    firstPage: suspend () -> PageDto<T>,
    nextPage: suspend (String) -> PageDto<T>,
): List<T> {
    val out = mutableListOf<T>()
    var page = firstPage()
    var guard = 0
    while (true) {
        out += page.values
        val next = page.next
        if (next == null || ++guard >= maxPages) break
        page = nextPage(next)
    }
    return out
}
