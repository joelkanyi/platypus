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
package com.joelkanyi.platypus

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.joelkanyi.platypus.app.PlatypusApp
import com.joelkanyi.platypus.data.repository.PlatypusActivityHolder
import com.joelkanyi.platypus.di.AndroidDependencies

class MainActivity : FragmentActivity() {

    private lateinit var dependencies: AndroidDependencies

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        PlatypusActivityHolder.activity = this
        val app = application as PlatypusApplication
        dependencies = AndroidDependencies(graph = app.appGraph, appContext = applicationContext)
        handleDeepLink(intent)

        setContent {
            PlatypusApp(dependencies)
        }
    }

    override fun onDestroy() {
        if (PlatypusActivityHolder.activity === this) PlatypusActivityHolder.activity = null
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == OAUTH_SCHEME) {
            data.getQueryParameter("code")?.let { dependencies.oauthDeepLinks.submit(it) }
        }
    }

    private companion object {
        const val OAUTH_SCHEME = "platypus"
    }
}
