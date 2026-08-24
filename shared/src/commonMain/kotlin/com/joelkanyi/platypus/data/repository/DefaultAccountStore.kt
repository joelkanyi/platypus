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

import com.joelkanyi.platypus.domain.model.StoredAccount
import com.joelkanyi.platypus.domain.repository.AccountStore
import com.joelkanyi.platypus.domain.repository.SecureStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultAccountStore(private val secureStore: SecureStore) : AccountStore {

    override suspend fun read(): List<StoredAccount> = secureStore.get(KEY)?.let(AccountCodec::decode) ?: emptyList()

    override suspend fun upsert(account: StoredAccount) {
        val next = read().filterNot { it.id == account.id } + account
        secureStore.set(KEY, AccountCodec.encode(next))
    }

    override suspend fun remove(id: String) {
        val next = read().filterNot { it.id == id }
        if (next.isEmpty()) secureStore.remove(KEY) else secureStore.set(KEY, AccountCodec.encode(next))
    }

    override suspend fun clear() {
        secureStore.remove(KEY)
    }

    private companion object {
        const val KEY = "accounts"
    }
}
