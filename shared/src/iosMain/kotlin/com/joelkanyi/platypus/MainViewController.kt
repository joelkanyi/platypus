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

import androidx.compose.ui.window.ComposeUIViewController
import com.joelkanyi.platypus.app.PlatypusApp
import com.joelkanyi.platypus.app.PlatypusDependencies
import com.joelkanyi.platypus.di.IosAppGraph
import dev.zacsweers.metro.createGraph

@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController {
    PlatypusApp(IosApp.dependencies)
}

private object IosApp {
    val graph: IosAppGraph = createGraph<IosAppGraph>()
    val dependencies: PlatypusDependencies by lazy { IosDependencies(graph) }
}

class IosDependencies(private val graph: IosAppGraph) : PlatypusDependencies {

    override val hasGraph: Boolean = true
}
