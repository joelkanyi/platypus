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
package com.joelkanyi.platypus.feature.pr.files

import androidx.compose.runtime.Composable
import com.joelkanyi.platypus.domain.model.DiffFileStatus
import com.joelkanyi.platypus.domain.model.PrDiff
import com.joelkanyi.platypus.domain.model.PrDiffFile
import com.joelkanyi.platypus.preview.PlatypusPreview
import com.joelkanyi.platypus.preview.PlatypusThemePreviews

private val sampleDiff = PrDiff(
    files = listOf(
        PrDiffFile(
            path = "app/data/mappers/DtoToDomain.kt",
            status = DiffFileStatus.MODIFIED,
            added = 59,
            removed = 1,
            lines = emptyList(),
        ),
        PrDiffFile(
            path = "app/components/SkuSpecificationRow.kt",
            status = DiffFileStatus.MODIFIED,
            added = 1,
            removed = 1,
            lines = emptyList(),
        ),
        PrDiffFile(
            path = "app/test/DtoToDomainTest.kt",
            status = DiffFileStatus.ADDED,
            added = 167,
            removed = 0,
            lines = emptyList(),
        ),
    ),
)

private val sampleFile = PrDiffFile(
    path = "app/data/mappers/DtoToDomain.kt",
    status = DiffFileStatus.MODIFIED,
    added = 2,
    removed = 1,
    lines = listOf(
        "diff --git a/DtoToDomain.kt b/DtoToDomain.kt",
        "@@ -30,7 +43,7 @@ object DtoToDomain {",
        "     serialNumber = serialNumber.orEmpty(),",
        "-    manufactureDate = manufacturerDate.orEmpty(),",
        "+    manufactureDate = manufacturerDate.toDisplayDate(),",
        "   )",
    ),
)

@PlatypusThemePreviews
@Composable
private fun FilesChangedContentPreview() {
    PlatypusPreview {
        FilesChangedContent(
            state = FilesChangedUiState(isLoading = false, diff = sampleDiff),
            onOpenFile = {},
            onBack = {},
            onRetry = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun FilesChangedErrorPreview() {
    PlatypusPreview {
        FilesChangedContent(
            state = FilesChangedUiState(isLoading = false, error = "Couldn't reach Bitbucket."),
            onOpenFile = {},
            onBack = {},
            onRetry = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun PrFileDiffContentPreview() {
    PlatypusPreview {
        PrFileDiffContent(
            path = "app/data/mappers/DtoToDomain.kt",
            state = PrFileDiffUiState(isLoading = false, file = sampleFile),
            onBack = {},
            onRetry = {},
            onToggleWrap = {},
            onStartComment = {},
            onCancelComment = {},
            onDraftChanged = {},
            onPostComment = {},
        )
    }
}
