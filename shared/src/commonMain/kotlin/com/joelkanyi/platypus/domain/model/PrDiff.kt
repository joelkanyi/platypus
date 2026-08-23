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
package com.joelkanyi.platypus.domain.model

enum class DiffFileStatus { ADDED, REMOVED, MODIFIED, RENAMED }

data class PrDiffFile(
    val path: String,
    val status: DiffFileStatus,
    val added: Int,
    val removed: Int,
    val lines: List<String>,
) {
    val id: String get() = path
}

data class PrDiff(val files: List<PrDiffFile>) {
    val totalAdded: Int get() = files.sumOf { it.added }

    val totalRemoved: Int get() = files.sumOf { it.removed }
}
