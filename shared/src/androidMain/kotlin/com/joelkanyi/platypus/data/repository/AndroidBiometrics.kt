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

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.joelkanyi.platypus.domain.repository.Biometrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class AndroidBiometrics(context: Context) : Biometrics {

    private val appContext = context.applicationContext

    override suspend fun isAvailable(): Boolean =
        BiometricManager.from(appContext).canAuthenticate(ALLOWED) == BiometricManager.BIOMETRIC_SUCCESS

    override suspend fun authenticate(reason: String): Boolean {
        val activity = PlatypusActivityHolder.activity ?: return false
        return withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine { continuation ->
                val prompt = BiometricPrompt(
                    activity,
                    ContextCompat.getMainExecutor(activity),
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            if (continuation.isActive) continuation.resume(true)
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            if (continuation.isActive) continuation.resume(false)
                        }
                    },
                )
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Platypus")
                    .setSubtitle(reason)
                    .setAllowedAuthenticators(ALLOWED)
                    .setNegativeButtonText("Cancel")
                    .build()
                continuation.invokeOnCancellation { prompt.cancelAuthentication() }
                prompt.authenticate(info)
            }
        }
    }

    private companion object {
        const val ALLOWED = BIOMETRIC_STRONG or BIOMETRIC_WEAK
    }
}
