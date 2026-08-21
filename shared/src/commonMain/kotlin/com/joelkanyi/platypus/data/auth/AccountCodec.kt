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
package com.joelkanyi.platypus.data.auth

import com.joelkanyi.platypus.data.remote.PlatypusJson
import com.joelkanyi.platypus.domain.model.BitbucketUser
import com.joelkanyi.platypus.domain.model.Credential
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

object AccountCodec {
    fun encode(accounts: List<StoredAccount>): String = PlatypusJson.encodeToString(accounts.map { it.toDto() })

    fun decode(raw: String): List<StoredAccount> = runCatching {
        PlatypusJson.decodeFromString<List<StoredAccountDto>>(raw).mapNotNull { it.toDomain() }
    }.getOrElse { emptyList() }

    @Serializable
    private data class StoredAccountDto(
        val id: String,
        val uuid: String,
        val accountId: String? = null,
        val nickname: String = "",
        val displayName: String = "",
        val avatarUrl: String? = null,
        val credType: String,
        val email: String? = null,
        val token: String? = null,
        val refreshToken: String? = null,
    )

    private fun StoredAccount.toDto(): StoredAccountDto {
        val base = StoredAccountDto(
            id = id,
            uuid = user.uuid,
            accountId = user.accountId,
            nickname = user.nickname,
            displayName = user.displayName,
            avatarUrl = user.avatarUrl,
            credType = "",
        )
        return when (val credential = credential) {
            is Credential.ApiToken -> base.copy(
                credType = "api_token",
                email = credential.email,
                token = credential.token,
            )
            is Credential.OAuth -> base.copy(credType = "oauth", refreshToken = credential.refreshToken)
        }
    }

    private fun StoredAccountDto.toDomain(): StoredAccount? {
        val credential = when (credType) {
            "api_token" -> if (email != null && token != null) Credential.ApiToken(email, token) else return null
            "oauth" -> refreshToken?.let(Credential::OAuth) ?: return null
            else -> return null
        }
        return StoredAccount(
            id = id,
            user = BitbucketUser(uuid, accountId, nickname, displayName, avatarUrl),
            credential = credential,
        )
    }
}
