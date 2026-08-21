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
package com.joelkanyi.platypus.feature.repo.file

import androidx.compose.runtime.Composable
import com.joelkanyi.platypus.domain.model.RepoFile
import com.joelkanyi.platypus.preview.PlatypusPreview
import com.joelkanyi.platypus.preview.PlatypusThemePreviews

private val sampleFile = RepoFile(
    path = "shared/src/Main.kt",
    lines = listOf(
        "fun main() {",
        "    val greeting = \"Hello, Platypus\"",
        "    println(greeting)",
        "}",
    ),
    truncatedAtLine = null,
    renderable = true,
    webUrl = "https://bitbucket.org/acme/api-gateway/src/main/shared/src/Main.kt",
)

@PlatypusThemePreviews
@Composable
private fun FileViewerContentPreview() {
    PlatypusPreview {
        FileViewerContent(
            fileName = "Main.kt",
            repoLabel = "platypus",
            path = "shared/src/Main.kt",
            onNavigateToPath = {},
            onBack = {},
            state = FileUiState(isLoading = false, file = sampleFile, wrap = true),
            onRetry = {},
            onToggleWrap = {},
            onToggleFind = {},
            onFindQuery = {},
            onNextMatch = {},
            onPreviousMatch = {},
            onToggleOutline = {},
            onJumpTo = {},
            onOpenUrl = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun FileViewerBinaryPreview() {
    PlatypusPreview {
        FileViewerContent(
            fileName = "logo.png",
            repoLabel = "platypus",
            path = "assets/logo.png",
            onNavigateToPath = {},
            onBack = {},
            state = FileUiState(
                isLoading = false,
                file = sampleFile.copy(path = "logo.png", lines = emptyList(), renderable = false),
                wrap = true,
            ),
            onRetry = {},
            onToggleWrap = {},
            onToggleFind = {},
            onFindQuery = {},
            onNextMatch = {},
            onPreviousMatch = {},
            onToggleOutline = {},
            onJumpTo = {},
            onOpenUrl = {},
        )
    }
}
