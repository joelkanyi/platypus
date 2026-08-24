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
package com.joelkanyi.platypus.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import kotlin.test.Test

/**
 * Guards the module's dependency direction so it stays splittable into Gradle modules. Each layer may
 * only depend downward. A new import that reverses an edge fails the build here instead of silently
 * making the module un-modularizable.
 */
class DependencyDirectionTest {

    private val root = "com.joelkanyi.platypus"
    private val files = Konsist.scopeFromProduction().files

    private fun inLayer(layer: String) = files.filter {
        val name = it.packagee?.name.orEmpty()
        name == "$root.$layer" || name.startsWith("$root.$layer.")
    }

    private fun importsAnyOf(
        vararg layers: String,
    ): (com.lemonappdev.konsist.api.declaration.KoFileDeclaration) -> Boolean = { file ->
        file.imports.any { import ->
            layers.any { import.name == "$root.$it" || import.name.startsWith("$root.$it.") }
        }
    }

    @Test
    fun `domain depends on nothing but core`() {
        inLayer("domain").assertFalse(
            testName = "domain must not import data, feature, ui, designsystem, navigation or app",
            function = importsAnyOf("data", "feature", "ui", "designsystem", "navigation", "app"),
        )
    }

    @Test
    fun `design system is a domain-blind leaf`() {
        inLayer("designsystem").assertFalse(
            testName = "designsystem must not import domain, data, feature, ui, navigation or app",
            function = importsAnyOf("domain", "data", "feature", "ui", "navigation", "app"),
        )
    }

    @Test
    fun `features never reach into the data layer`() {
        inLayer("feature").assertFalse(
            testName = "features must talk to domain interfaces, never data",
            function = importsAnyOf("data"),
        )
    }

    @Test
    fun `features never depend on one another`() {
        inLayer("feature").assertFalse(testName = "one feature must not import another feature") { file ->
            val ownFeature = file.packagee?.name.orEmpty()
                .removePrefix("$root.feature.")
                .substringBefore(".")
            file.imports.any { import ->
                import.name.startsWith("$root.feature.") &&
                    !import.name.removePrefix("$root.feature.").startsWith(ownFeature)
            }
        }
    }

    @Test
    fun `the navigation shell assembles feature entries and never imports feature screens`() {
        inLayer("navigation").assertFalse(
            testName = "navigation must call each feature's entry provider, not import its screens",
        ) { file ->
            file.imports.any { import ->
                import.name.startsWith("$root.feature.") && import.name.substringAfterLast('.').endsWith("Screen")
            }
        }
    }

    @Test
    fun `data-layer wire types never leak outside data`() {
        val nonData = files.filterNot {
            val name = it.packagee?.name.orEmpty()
            name == "$root.data" || name.startsWith("$root.data.")
        }
        nonData.assertFalse(
            testName = "DTOs and API clients are data internals; only repositories may use them",
        ) { file ->
            file.imports.any { import ->
                import.name.startsWith("$root.data.remote.dto") || import.name.startsWith("$root.data.remote.api")
            }
        }
    }
}
