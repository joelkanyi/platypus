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
package com.joelkanyi.platypus.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface PlatypusKey : NavKey

@Serializable data object InboxKey : PlatypusKey

@Serializable data object RepositoriesKey : PlatypusKey

@Serializable data object ProfileKey : PlatypusKey

@Serializable data object SettingsKey : PlatypusKey

@Serializable
data class PullRequestKey(
    val accountId: String,
    val workspace: String,
    val repoSlug: String,
    val prId: Long,
    val repoName: String = "",
) : PlatypusKey

@Serializable
data class PullRequestDiffKey(val workspace: String, val repoSlug: String, val prId: Long) : PlatypusKey

@Serializable
data class FilesChangedKey(
    val accountId: String,
    val workspace: String,
    val repoSlug: String,
    val prId: Long,
    val repoName: String = "",
) : PlatypusKey

@Serializable
data class PrFileDiffKey(
    val accountId: String,
    val workspace: String,
    val repoSlug: String,
    val prId: Long,
    val path: String,
) : PlatypusKey

@Serializable
data class PrCommitsKey(
    val accountId: String,
    val workspace: String,
    val repoSlug: String,
    val prId: Long,
    val repoName: String = "",
) : PlatypusKey

@Serializable
data class RepositoryOverviewKey(
    val accountId: String,
    val workspace: String,
    val repoSlug: String,
    val repoName: String,
) : PlatypusKey

@Serializable
data class RepoPullRequestsKey(
    val accountId: String,
    val workspace: String,
    val repoSlug: String,
    val repoName: String,
) : PlatypusKey

@Serializable
data class RepositoryBrowseKey(
    val accountId: String,
    val workspace: String,
    val repoSlug: String,
    val ref: String,
    val path: String = "",
) : PlatypusKey

@Serializable
data class FileViewerKey(
    val accountId: String,
    val workspace: String,
    val repoSlug: String,
    val ref: String,
    val path: String,
) : PlatypusKey

@Serializable
data class CommitsKey(val accountId: String, val workspace: String, val repoSlug: String, val ref: String) :
    PlatypusKey

@Serializable
data class CommitDetailKey(val accountId: String, val workspace: String, val repoSlug: String, val hash: String) :
    PlatypusKey

@Serializable
data class PipelineKey(val workspace: String, val repoSlug: String, val pipelineUuid: String) : PlatypusKey

enum class TopLevelDestination(val root: PlatypusKey) {
    REPOSITORIES(RepositoriesKey),
    INBOX(InboxKey),
    PROFILE(ProfileKey),
}
