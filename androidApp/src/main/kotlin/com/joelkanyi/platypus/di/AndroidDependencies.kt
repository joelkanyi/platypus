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
package com.joelkanyi.platypus.di

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.joelkanyi.platypus.app.OAuthDeepLinks
import com.joelkanyi.platypus.app.PlatypusDependencies
import com.joelkanyi.platypus.data.auth.Biometrics
import com.joelkanyi.platypus.domain.repository.AuthRepository
import com.joelkanyi.platypus.domain.repository.InboxCache
import com.joelkanyi.platypus.domain.repository.PipelineRepository
import com.joelkanyi.platypus.domain.repository.PullRequestRepository
import com.joelkanyi.platypus.domain.repository.RepoContentRepository
import com.joelkanyi.platypus.domain.repository.SearchRepository
import com.joelkanyi.platypus.domain.repository.SettingsStore
import com.joelkanyi.platypus.domain.repository.WatchlistRepository

class AndroidDependencies(graph: AppGraph, private val appContext: Context) : PlatypusDependencies {

    override val authRepository: AuthRepository = graph.authRepository
    override val watchlistRepository: WatchlistRepository = graph.watchlistRepository
    override val repoContentRepository: RepoContentRepository = graph.repoContentRepository
    override val pullRequestRepository: PullRequestRepository = graph.pullRequestRepository
    override val pipelineRepository: PipelineRepository = graph.pipelineRepository
    override val searchRepository: SearchRepository = graph.searchRepository
    override val inboxCache: InboxCache = graph.inboxCache
    override val settingsStore: SettingsStore = graph.settingsStore
    override val biometrics: Biometrics = graph.biometrics
    override val oauthDeepLinks: OAuthDeepLinks = graph.oauthDeepLinks

    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }
}
