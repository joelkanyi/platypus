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
import com.joelkanyi.platypus.data.remote.dto.UserDto
import com.joelkanyi.platypus.data.remote.dto.WorkspaceDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserMapperTest {

    @Test
    fun userMapsAllFields() {
        val dto = UserDto(
            uuid = "{uuid}",
            accountId = "acc-1",
            nickname = "joel",
            displayName = "Joel Kanyi",
            links = LinksDto(avatar = LinkDto(href = "https://img/avatar.png")),
        )
        val user = dto.toDomain()
        assertEquals("{uuid}", user.uuid)
        assertEquals("acc-1", user.accountId)
        assertEquals("joel", user.nickname)
        assertEquals("Joel Kanyi", user.displayName)
        assertEquals("https://img/avatar.png", user.avatarUrl)
    }

    @Test
    fun userDisplayNameFallsBackToNickname() {
        val dto = UserDto(uuid = "{uuid}", nickname = "joel", displayName = "")
        assertEquals("joel", dto.toDomain().displayName)
    }

    @Test
    fun userAvatarIsNullWhenLinksMissing() {
        val dto = UserDto(uuid = "{uuid}", nickname = "joel", displayName = "Joel")
        assertNull(dto.toDomain().avatarUrl)
    }

    @Test
    fun workspaceNameFallsBackToSlug() {
        val dto = WorkspaceDto(uuid = "{ws}", slug = "acme", name = "")
        val workspace = dto.toDomain()
        assertEquals("acme", workspace.name)
        assertEquals("acme", workspace.slug)
    }
}
