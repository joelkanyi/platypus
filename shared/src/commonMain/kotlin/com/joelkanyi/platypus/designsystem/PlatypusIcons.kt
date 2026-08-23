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
package com.joelkanyi.platypus.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Icons the Jenga set does not ship (folder, file, up-a-level, git branch), built from SVG path data. */
object PlatypusIcons {

    val Folder: ImageVector by lazy {
        icon("M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z")
    }

    val File: ImageVector by lazy {
        icon("M6 2c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6H6zm7 7V3.5L18.5 9H13z")
    }

    val LevelUp: ImageVector by lazy {
        icon("M9 5v4h6a4 4 0 0 1 4 4v6h-2v-6a2 2 0 0 0-2-2H9v4l-5-5 5-5z")
    }

    val GitBranch: ImageVector by lazy {
        icon(
            "M6 4a2 2 0 0 1 1 3.82v.68a3 3 0 0 0 3 3h1a5 5 0 0 0 4.9-4.02A2 2 0 1 1 " +
                "18 7.5a3 3 0 0 1-.13.4A7 7 0 0 1 11 13.5h-1a5 5 0 0 1-3-1v3.68a2 2 0 1 " +
                "1-2 0V7.82A2 2 0 0 1 6 4zm0 13a1 1 0 1 0 0 2 1 1 0 0 0 0-2zm0-11a1 1 0 " +
                "1 0 0 2 1 1 0 0 0 0-2zm10 0a1 1 0 1 0 0 2 1 1 0 0 0 0-2z",
        )
    }

    val MoreVertical: ImageVector by lazy {
        icon(
            "M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 " +
                "2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z",
        )
    }

    val Send: ImageVector by lazy {
        icon("M2.01 21L23 12 2.01 3 2 10l15 2-15 2z")
    }

    private fun icon(pathData: String): ImageVector = ImageVector.Builder(
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        addPath(PathParser().parsePathString(pathData).toNodes(), fill = SolidColor(Color.Black))
    }.build()
}
