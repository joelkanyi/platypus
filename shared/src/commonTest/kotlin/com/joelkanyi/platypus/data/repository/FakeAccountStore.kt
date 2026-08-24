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

class FakeAccountStore(initial: List<StoredAccount> = emptyList()) : AccountStore {

    private val accounts = initial.toMutableList()

    val stored: List<StoredAccount> get() = accounts.toList()

    override suspend fun read(): List<StoredAccount> = accounts.toList()

    override suspend fun upsert(account: StoredAccount) {
        accounts.removeAll { it.id == account.id }
        accounts.add(account)
    }

    override suspend fun remove(id: String) {
        accounts.removeAll { it.id == id }
    }

    override suspend fun clear() {
        accounts.clear()
    }
}
