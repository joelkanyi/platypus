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

import com.joelkanyi.platypus.domain.repository.Biometrics
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosBiometrics : Biometrics {

    override suspend fun isAvailable(): Boolean = LAContext().canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, null)

    override suspend fun authenticate(reason: String): Boolean = suspendCancellableCoroutine { continuation ->
        LAContext().evaluatePolicy(LAPolicyDeviceOwnerAuthentication, reason) { success, _ ->
            if (continuation.isActive) continuation.resume(success)
        }
    }
}
