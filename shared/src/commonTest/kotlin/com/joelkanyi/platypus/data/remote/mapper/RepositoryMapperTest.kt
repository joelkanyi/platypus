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

import com.joelkanyi.platypus.data.remote.dto.LinkDto
import com.joelkanyi.platypus.data.remote.dto.LinksDto
import com.joelkanyi.platypus.data.remote.dto.RepositoryDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RepositoryMapperTest {

    @Test
    fun mapsAllFieldsAndInjectsWorkspaceSlug() {
        val dto = RepositoryDto(
            uuid = "{r}",
            name = "API Gateway",
            slug = "api-gateway",
            fullName = "acme/api-gateway",
            isPrivate = true,
            description = "Edge",
            links = LinksDto(avatar = LinkDto(href = "https://img/r.png")),
        )

        val repo = dto.toDomain(workspaceSlug = "acme")

        assertEquals("{r}", repo.uuid)
        assertEquals("acme", repo.workspaceSlug)
        assertEquals("api-gateway", repo.slug)
        assertEquals("API Gateway", repo.name)
        assertEquals("acme/api-gateway", repo.fullName)
        assertEquals(true, repo.isPrivate)
        assertEquals("https://img/r.png", repo.avatarUrl)
    }

    @Test
    fun fallsBackToSlugWhenNameBlankAndNullAvatar() {
        val repo = RepositoryDto(uuid = "{r}", name = "", slug = "infra").toDomain(workspaceSlug = "acme")

        assertEquals("infra", repo.name)
        assertNull(repo.avatarUrl)
    }
}
