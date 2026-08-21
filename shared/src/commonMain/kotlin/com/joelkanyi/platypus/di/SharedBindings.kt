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

import com.joelkanyi.platypus.data.auth.AccountStore
import com.joelkanyi.platypus.data.auth.AuthConfig
import com.joelkanyi.platypus.data.auth.Biometrics
import com.joelkanyi.platypus.data.auth.NoopBiometrics
import com.joelkanyi.platypus.data.auth.PlatypusConfig
import com.joelkanyi.platypus.data.auth.createAuthRepository
import com.joelkanyi.platypus.domain.repository.AuthRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@BindingContainer
@ContributesTo(AppScope::class)
object SharedBindings {

    @Provides fun authConfig(): AuthConfig = PlatypusConfig.auth

    @Provides fun biometrics(): Biometrics = NoopBiometrics

    @Provides
    @SingleIn(AppScope::class)
    fun authRepository(config: AuthConfig, accountStore: AccountStore): AuthRepository =
        createAuthRepository(config, accountStore)
}
