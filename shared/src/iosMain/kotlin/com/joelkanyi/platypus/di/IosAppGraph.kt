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

import com.joelkanyi.platypus.data.auth.Biometrics
import com.joelkanyi.platypus.data.auth.OAuthDeepLinks
import com.joelkanyi.platypus.data.local.PlatypusDatabase
import com.joelkanyi.platypus.data.local.createPlatypusDatabase
import com.joelkanyi.platypus.data.local.platypusDatabaseBuilder
import com.joelkanyi.platypus.domain.repository.AuthRepository
import com.joelkanyi.platypus.domain.repository.RepoContentRepository
import com.joelkanyi.platypus.domain.repository.WatchlistRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@DependencyGraph(AppScope::class)
interface IosAppGraph {

    val authRepository: AuthRepository
    val watchlistRepository: WatchlistRepository
    val repoContentRepository: RepoContentRepository
    val biometrics: Biometrics
    val oauthDeepLinks: OAuthDeepLinks

    @Provides
    @SingleIn(AppScope::class)
    fun database(): PlatypusDatabase = createPlatypusDatabase(platypusDatabaseBuilder())
}
