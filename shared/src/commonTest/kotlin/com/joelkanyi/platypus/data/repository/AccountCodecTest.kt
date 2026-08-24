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

import com.joelkanyi.platypus.domain.model.BitbucketUser
import com.joelkanyi.platypus.domain.model.Credential
import com.joelkanyi.platypus.domain.model.StoredAccount
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccountCodecTest {

    private fun user(uuid: String) = BitbucketUser(uuid, "acc-$uuid", "nick$uuid", "Name $uuid", null)

    @Test
    fun roundTripsMixedAccounts() {
        val accounts = listOf(
            StoredAccount("a", user("a"), Credential.ApiToken("joel@example.com", "atk")),
            StoredAccount("b", user("b"), Credential.OAuth("refresh-b")),
        )
        val decoded = AccountCodec.decode(AccountCodec.encode(accounts))
        assertEquals(accounts, decoded)
    }

    @Test
    fun emptyListRoundTrips() {
        assertEquals(emptyList(), AccountCodec.decode(AccountCodec.encode(emptyList())))
    }

    @Test
    fun decodeReturnsEmptyForGarbage() {
        assertTrue(AccountCodec.decode("not json").isEmpty())
    }

    @Test
    fun decodeDropsAccountsWithUnknownCredType() {
        val raw = """[{"id":"a","uuid":"a","credType":"totally_unknown"}]"""
        assertTrue(AccountCodec.decode(raw).isEmpty())
    }
}
