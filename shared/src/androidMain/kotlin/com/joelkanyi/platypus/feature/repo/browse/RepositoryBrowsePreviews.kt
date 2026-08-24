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
package com.joelkanyi.platypus.feature.repo.browse

import androidx.compose.runtime.Composable
import com.joelkanyi.platypus.domain.model.SrcEntry
import com.joelkanyi.platypus.domain.model.SrcEntryType
import com.joelkanyi.platypus.preview.PlatypusPreview
import com.joelkanyi.platypus.preview.PlatypusThemePreviews
import kotlinx.collections.immutable.persistentListOf

private val sampleEntries = persistentListOf(
    SrcEntry("commission/src", SrcEntryType.DIRECTORY, 0),
    SrcEntry("commission/build.gradle.kts", SrcEntryType.FILE, 3_400),
    SrcEntry("commission/.gitignore", SrcEntryType.FILE, 6),
    SrcEntry("commission/proguard-rules.pro", SrcEntryType.FILE, 750),
)

@PlatypusThemePreviews
@Composable
private fun BrowseContentPreview() {
    PlatypusPreview {
        BrowseContent(
            repoLabel = "d.light-atlas-android-app",
            state = BrowseUiState(ref = "master", path = "commission", isLoading = false, entries = sampleEntries),
            onBack = {},
            onRetry = {},
            onQueryChanged = {},
            onNavigateToPath = {},
            onBranchClick = {},
            onOpenFile = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun BrowseRootPreview() {
    PlatypusPreview {
        BrowseContent(
            repoLabel = "platypus",
            state = BrowseUiState(ref = "main", path = "", isLoading = false, entries = sampleEntries),
            onBack = {},
            onRetry = {},
            onQueryChanged = {},
            onNavigateToPath = {},
            onBranchClick = {},
            onOpenFile = {},
        )
    }
}
