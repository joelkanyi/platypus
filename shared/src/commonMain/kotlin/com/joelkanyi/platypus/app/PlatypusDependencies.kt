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
package com.joelkanyi.platypus.app

import androidx.compose.runtime.staticCompositionLocalOf
import com.joelkanyi.platypus.domain.repository.AuthRepository
import com.joelkanyi.platypus.domain.repository.Biometrics
import com.joelkanyi.platypus.domain.repository.InboxCache
import com.joelkanyi.platypus.domain.repository.PipelineRepository
import com.joelkanyi.platypus.domain.repository.PullRequestRepository
import com.joelkanyi.platypus.domain.repository.RepoContentRepository
import com.joelkanyi.platypus.domain.repository.SearchRepository
import com.joelkanyi.platypus.domain.repository.SettingsStore
import com.joelkanyi.platypus.domain.repository.WatchlistRepository

interface PlatypusDependencies {
    val authRepository: AuthRepository
    val watchlistRepository: WatchlistRepository
    val repoContentRepository: RepoContentRepository
    val pullRequestRepository: PullRequestRepository
    val pipelineRepository: PipelineRepository
    val searchRepository: SearchRepository
    val inboxCache: InboxCache
    val settingsStore: SettingsStore
    val biometrics: Biometrics
    val oauthDeepLinks: OAuthDeepLinks

    fun openUrl(url: String)
}

val LocalPlatypusDependencies = staticCompositionLocalOf<PlatypusDependencies> {
    error("PlatypusDependencies was not provided. Render the app through PlatypusApp(dependencies).")
}

/**
 * Drops transient, session-derived data on sign-out: the persisted inbox snapshot and the in-memory
 * content/diff caches. The watchlist is intentionally left (it is per-account and survives sign-out).
 */
suspend fun PlatypusDependencies.purgeSessionCaches() {
    inboxCache.clear()
    repoContentRepository.clearCache()
    pullRequestRepository.clearCache()
}
