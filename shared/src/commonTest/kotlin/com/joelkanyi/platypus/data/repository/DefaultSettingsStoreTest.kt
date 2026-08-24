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
import com.joelkanyi.platypus.domain.model.AppSettings
import com.joelkanyi.platypus.domain.model.ThemeMode
import com.joelkanyi.platypus.domain.repository.SecureStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class MapSecureStore(private val map: MutableMap<String, String> = mutableMapOf()) : SecureStore {
    override suspend fun get(key: String): String? = map[key]

    override suspend fun set(key: String, value: String) {
        map[key] = value
    }

    override suspend fun remove(key: String) {
        map.remove(key)
    }
}

private class TestDispatchers(private val dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher get() = dispatcher
    override val io: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher
}

class DefaultSettingsStoreTest {

    @Test
    fun defaultsWhenNothingPersisted() = runTest {
        val store = DefaultSettingsStore(MapSecureStore(), TestDispatchers(UnconfinedTestDispatcher(testScheduler)))
        assertEquals(AppSettings(), store.settings.value)
    }

    @Test
    fun updatePersistsAndReloads() = runTest {
        val backing = MapSecureStore()
        val dispatchers = TestDispatchers(UnconfinedTestDispatcher(testScheduler))
        val store = DefaultSettingsStore(backing, dispatchers)

        store.update(AppSettings(theme = ThemeMode.DARK, wrapCode = true, appLockEnabled = true))
        assertEquals(ThemeMode.DARK, store.settings.value.theme)

        val reloaded = DefaultSettingsStore(backing, dispatchers)
        testScheduler.advanceUntilIdle()
        assertEquals(ThemeMode.DARK, reloaded.settings.value.theme)
        assertEquals(true, reloaded.settings.value.wrapCode)
        assertEquals(true, reloaded.settings.value.appLockEnabled)
    }
}
