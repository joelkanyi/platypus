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

import kotlinx.serialization.Serializable

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class CodeFontSize { SMALL, MEDIUM, LARGE }

@Serializable
enum class InboxFilter { TO_REVIEW, MINE, ALL }

@Serializable
enum class RepoTab { WATCHING, BROWSE }

@Serializable
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val codeFontSize: CodeFontSize = CodeFontSize.MEDIUM,
    val wrapCode: Boolean = false,
    val renderMarkdownByDefault: Boolean = true,
    val defaultInboxFilter: InboxFilter = InboxFilter.TO_REVIEW,
    val defaultReposTab: RepoTab = RepoTab.WATCHING,
    val defaultMergeStrategy: MergeStrategy = MergeStrategy.MERGE_COMMIT,
    val closeSourceBranchOnMerge: Boolean = false,
    val appLockEnabled: Boolean = false,
)
