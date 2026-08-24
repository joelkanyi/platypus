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

import com.joelkanyi.platypus.data.remote.dto.PageDto
import com.joelkanyi.platypus.data.remote.dto.UserDto
import com.joelkanyi.platypus.data.remote.dto.WorkspaceMembershipDto
import com.joelkanyi.platypus.data.remote.network.BITBUCKET_API_BASE
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class UserApi(private val client: HttpClient) {

    suspend fun getCurrentUser(): UserDto = client.get("$BITBUCKET_API_BASE/user").body()

    suspend fun getWorkspaces(): PageDto<WorkspaceMembershipDto> =
        client.get("$BITBUCKET_API_BASE/user/workspaces?pagelen=100").body()
}
