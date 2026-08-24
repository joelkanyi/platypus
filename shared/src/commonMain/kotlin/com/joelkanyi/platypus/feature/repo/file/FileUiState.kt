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

import androidx.compose.runtime.Immutable
import com.joelkanyi.platypus.domain.model.RepoFile

@Immutable
data class FileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val file: RepoFile? = null,
    val findActive: Boolean = false,
    val findQuery: String = "",
    val matches: List<Int> = emptyList(),
    val matchIndex: Int = 0,
    val outlineOpen: Boolean = false,
    val isMarkdown: Boolean = false,
    val preview: Boolean = false,
    val defaultBranch: String? = null,
) {
    val currentMatchLine: Int? get() = matches.getOrNull(matchIndex)
}
