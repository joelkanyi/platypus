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
package com.joelkanyi.platypus.data.remote.api

import com.joelkanyi.platypus.data.remote.BITBUCKET_API_BASE
import com.joelkanyi.platypus.data.remote.dto.CodeSearchResultDto
import com.joelkanyi.platypus.data.remote.dto.PageDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.encodeURLPathPart

class SearchApi(private val client: HttpClient) {

    suspend fun code(workspaceSlug: String, query: String): PageDto<CodeSearchResultDto> =
        client.get("$BITBUCKET_API_BASE/workspaces/${workspaceSlug.encodeURLPathPart()}/search/code") {
            parameter("search_query", query)
            parameter("pagelen", PAGE_LEN)
            parameter("fields", CODE_FIELDS)
        }.body()

    suspend fun codePage(url: String): PageDto<CodeSearchResultDto> = client.get(url).body()

    private companion object {
        const val PAGE_LEN = 25
        const val CODE_FIELDS = "+values.file.commit,+values.file.commit.repository"
    }
}
