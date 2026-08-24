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
package com.joelkanyi.platypus

import androidx.compose.ui.window.ComposeUIViewController
import com.joelkanyi.platypus.app.OAuthDeepLinks
import com.joelkanyi.platypus.app.PlatypusApp
import com.joelkanyi.platypus.app.PlatypusDependencies
import com.joelkanyi.platypus.data.auth.Biometrics
import com.joelkanyi.platypus.di.IosAppGraph
import com.joelkanyi.platypus.domain.repository.AuthRepository
import com.joelkanyi.platypus.domain.repository.InboxCache
import com.joelkanyi.platypus.domain.repository.PipelineRepository
import com.joelkanyi.platypus.domain.repository.PullRequestRepository
import com.joelkanyi.platypus.domain.repository.RepoContentRepository
import com.joelkanyi.platypus.domain.repository.SearchRepository
import com.joelkanyi.platypus.domain.repository.SettingsStore
import com.joelkanyi.platypus.domain.repository.WatchlistRepository
import dev.zacsweers.metro.createGraph
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.UIKit.UIApplication

@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController {
    PlatypusApp(IosApp.dependencies)
}

fun handleOAuthDeepLink(url: String) {
    val code = extractCode(url) ?: return
    IosApp.deepLinks.submit(code)
}

private fun extractCode(url: String): String? {
    val components = NSURLComponents(string = url)
    val items = components.queryItems ?: return null
    return items.filterIsInstance<NSURLQueryItem>().firstOrNull { it.name == "code" }?.value
}

private object IosApp {
    val graph: IosAppGraph = createGraph<IosAppGraph>()
    val deepLinks: OAuthDeepLinks get() = graph.oauthDeepLinks
    val dependencies: PlatypusDependencies by lazy { IosDependencies(graph) }
}

class IosDependencies(graph: IosAppGraph) : PlatypusDependencies {

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
        val nsUrl = NSURL(string = url)
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}
