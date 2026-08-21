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

import com.joelkanyi.platypus.data.remote.dto.RepositoryDto
import com.joelkanyi.platypus.domain.model.Repository

fun RepositoryDto.toDomain(workspaceSlug: String): Repository = Repository(
    uuid = uuid,
    workspaceSlug = workspaceSlug,
    slug = slug,
    name = name.ifBlank { slug },
    fullName = fullName,
    description = description,
    isPrivate = isPrivate,
    avatarUrl = links?.avatar?.href,
)
