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
package com.joelkanyi.platypus.data.repository

import com.joelkanyi.platypus.data.remote.PlatypusJson
import com.joelkanyi.platypus.domain.model.PrRelationship
import com.joelkanyi.platypus.domain.model.PullRequest
import com.joelkanyi.platypus.domain.repository.CachedInbox
import com.joelkanyi.platypus.domain.repository.InboxCache
import com.joelkanyi.platypus.domain.repository.SecureStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultInboxCache(private val secureStore: SecureStore) : InboxCache {

    override suspend fun load(): CachedInbox? {
        val raw = secureStore.get(KEY) ?: return null
        return runCatching {
            val envelope = PlatypusJson.decodeFromString<Envelope>(raw)
            CachedInbox(envelope.prs.map { it.toDomain() }, envelope.updatedAt)
        }.getOrNull()
    }

    override suspend fun save(pullRequests: List<PullRequest>, updatedAtEpochMs: Long) {
        val envelope = Envelope(updatedAtEpochMs, pullRequests.map { it.toDto() })
        secureStore.set(KEY, PlatypusJson.encodeToString(envelope))
    }

    override suspend fun clear() {
        secureStore.remove(KEY)
    }

    private companion object {
        const val KEY = "inbox_cache_v1"
    }
}

@Serializable
private data class Envelope(val updatedAt: Long, val prs: List<CachedPr>)

@Serializable
private data class CachedPr(
    val id: Long,
    val title: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val sourceBranch: String,
    val destinationBranch: String,
    val commentCount: Int,
    val updatedOn: String,
    val webUrl: String?,
    val relationship: String,
    val accountId: String,
    val accountLabel: String,
    val workspaceSlug: String,
    val repoSlug: String,
    val repoName: String,
)

private fun PullRequest.toDto() = CachedPr(
    id = id,
    title = title,
    authorName = authorName,
    authorAvatarUrl = authorAvatarUrl,
    sourceBranch = sourceBranch,
    destinationBranch = destinationBranch,
    commentCount = commentCount,
    updatedOn = updatedOn,
    webUrl = webUrl,
    relationship = relationship.name,
    accountId = accountId,
    accountLabel = accountLabel,
    workspaceSlug = workspaceSlug,
    repoSlug = repoSlug,
    repoName = repoName,
)

private fun CachedPr.toDomain() = PullRequest(
    id = id,
    title = title,
    authorName = authorName,
    authorAvatarUrl = authorAvatarUrl,
    sourceBranch = sourceBranch,
    destinationBranch = destinationBranch,
    commentCount = commentCount,
    updatedOn = updatedOn,
    webUrl = webUrl,
    relationship = runCatching { PrRelationship.valueOf(relationship) }.getOrDefault(PrRelationship.OTHER),
    accountId = accountId,
    accountLabel = accountLabel,
    workspaceSlug = workspaceSlug,
    repoSlug = repoSlug,
    repoName = repoName,
)
