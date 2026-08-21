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

import com.joelkanyi.platypus.core.concurrency.DispatcherProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.withContext
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosSecureStore(private val dispatchers: DispatcherProvider) : SecureStore {

    override suspend fun get(key: String): String? = withContext(dispatchers.io) {
        readRaw(key)
    }

    override suspend fun set(key: String, value: String): Unit = withContext(dispatchers.io) {
        writeRaw(key, value)
    }

    override suspend fun remove(key: String): Unit = withContext(dispatchers.io) {
        deleteRaw(key)
    }

    private fun writeRaw(account: String, value: String) {
        deleteRaw(account)
        val service = cfString(SERVICE)
        val accountRef = cfString(account)
        val data = cfData(value.encodeToByteArray())
        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, service)
        CFDictionaryAddValue(query, kSecAttrAccount, accountRef)
        CFDictionaryAddValue(query, kSecValueData, data)
        CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        SecItemAdd(query, null)
        release(query)
        release(service)
        release(accountRef)
        release(data)
    }

    private fun readRaw(account: String): String? = memScoped {
        val service = cfString(SERVICE)
        val accountRef = cfString(account)
        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, service)
        CFDictionaryAddValue(query, kSecAttrAccount, accountRef)
        CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        release(query)
        release(service)
        release(accountRef)
        if (status != errSecSuccess) {
            return@memScoped null
        }
        val dataRef = result.value
        val out = cfDataToString(dataRef?.reinterpret())
        dataRef?.let { CFRelease(it) }
        out
    }

    private fun deleteRaw(account: String) {
        val service = cfString(SERVICE)
        val accountRef = cfString(account)
        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionaryAddValue(query, kSecAttrService, service)
        CFDictionaryAddValue(query, kSecAttrAccount, accountRef)
        SecItemDelete(query)
        release(query)
        release(service)
        release(accountRef)
    }

    private fun cfString(value: String): CFStringRef? =
        CFStringCreateWithCString(kCFAllocatorDefault, value, kCFStringEncodingUTF8)

    private fun cfData(bytes: ByteArray): CFDataRef? = if (bytes.isEmpty()) {
        CFDataCreate(kCFAllocatorDefault, null, 0)
    } else {
        bytes.usePinned { pinned ->
            CFDataCreate(kCFAllocatorDefault, pinned.addressOf(0).reinterpret(), bytes.size.toLong())
        }
    }

    private fun cfDataToString(data: CFDataRef?): String? {
        val length = CFDataGetLength(data).toInt()
        if (length <= 0) return ""
        val bytePtr = CFDataGetBytePtr(data) ?: return null
        val bytes = ByteArray(length)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytePtr, length.toULong())
        }
        return bytes.decodeToString()
    }

    private fun release(ref: CFTypeRef?) {
        if (ref != null) CFRelease(ref)
    }

    private companion object {
        const val SERVICE = "com.joelkanyi.platypus.auth"
    }
}
