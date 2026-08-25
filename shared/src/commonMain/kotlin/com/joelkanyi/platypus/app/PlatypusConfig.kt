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
package com.joelkanyi.platypus.app

import com.joelkanyi.platypus.domain.model.AuthConfig

object PlatypusConfig {
    // Keep in sync with androidApp versionName and the iOS marketing version.
    const val VERSION = "0.1.0"

    // The custom scheme the app catches; the OAuth Worker's /callback bounces to it.
    const val APP_REDIRECT = "platypus://oauth/callback"

    // Fill these in after deploying the OAuth Worker and registering the Bitbucket
    // consumer. Both are public (not secrets). Leaving them blank disables the
    // "Sign in with Bitbucket" button; API-token sign-in works regardless.
    private const val BACKEND_BASE_URL = "https://platypus-oauth.joelkanyi98.workers.dev"
    private const val OAUTH_CLIENT_ID = "I2AhqUuSAM1IEHtP4KNk8KLajJmmEE0O"

    val auth: AuthConfig = AuthConfig(
        backendBaseUrl = BACKEND_BASE_URL,
        oauthClientId = OAUTH_CLIENT_ID,
        // Bitbucket redirects to the Worker's https /callback, which then bounces to
        // APP_REDIRECT. Authorize and token-exchange must send the same redirect_uri.
        redirectUri = if (BACKEND_BASE_URL.isBlank()) "" else "$BACKEND_BASE_URL/callback",
    )
}
