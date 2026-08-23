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

import com.joelkanyi.platypus.core.concurrency.DispatcherProvider
import com.joelkanyi.platypus.data.auth.SecureStore
import com.joelkanyi.platypus.data.remote.PlatypusJson
import com.joelkanyi.platypus.domain.model.AppSettings
import com.joelkanyi.platypus.domain.repository.SettingsStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultSettingsStore(private val secureStore: SecureStore, dispatchers: DispatcherProvider) : SettingsStore {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io)
    private val _settings = MutableStateFlow(AppSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        scope.launch {
            secureStore.get(KEY)?.let { raw ->
                runCatching {
                    PlatypusJson.decodeFromString<AppSettings>(raw)
                }.getOrNull()?.let { _settings.value = it }
            }
        }
    }

    override fun update(settings: AppSettings) {
        _settings.value = settings
        scope.launch { secureStore.set(KEY, PlatypusJson.encodeToString(settings)) }
    }

    private companion object {
        const val KEY = "app_settings_v1"
    }
}
