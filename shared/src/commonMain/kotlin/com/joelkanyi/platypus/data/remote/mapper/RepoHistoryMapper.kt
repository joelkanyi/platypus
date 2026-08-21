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

import com.joelkanyi.platypus.data.remote.dto.BranchDto
import com.joelkanyi.platypus.data.remote.dto.CommitDto
import com.joelkanyi.platypus.domain.model.Branch
import com.joelkanyi.platypus.domain.model.Commit

fun BranchDto.toDomain(): Branch = Branch(name = name, targetHash = target?.hash.orEmpty())

fun CommitDto.toDomain(): Commit = Commit(
    hash = hash,
    message = message.trim(),
    authorName = author?.user?.displayName?.takeIf { it.isNotBlank() }
        ?: author?.raw?.substringBefore('<')?.trim().orEmpty(),
    date = date,
)
