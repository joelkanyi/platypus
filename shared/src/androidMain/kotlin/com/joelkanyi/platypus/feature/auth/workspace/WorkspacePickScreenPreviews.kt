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
package com.joelkanyi.platypus.feature.auth.workspace

import androidx.compose.runtime.Composable
import com.joelkanyi.platypus.domain.model.Account
import com.joelkanyi.platypus.domain.model.AuthMode
import com.joelkanyi.platypus.domain.model.BitbucketUser
import com.joelkanyi.platypus.domain.model.Workspace
import com.joelkanyi.platypus.preview.PlatypusPreview
import com.joelkanyi.platypus.preview.PlatypusThemePreviews

private val sampleAccounts = listOf(
    Account("1", BitbucketUser("{u1}", "acc1", "joelkanyi", "Joel Kanyi", null), AuthMode.API_TOKEN),
    Account("2", BitbucketUser("{u2}", "acc2", "joel-acme", "Joel (Acme Corp)", null), AuthMode.OAUTH),
)

private val sampleWorkspaces = listOf(
    Workspace(uuid = "1", slug = "acme-corp", name = "acme-corp", avatarUrl = null),
    Workspace(uuid = "2", slug = "platypus-labs", name = "platypus-labs", avatarUrl = null),
)

@Composable
private fun PreviewOf(state: WorkspacePickUiState) {
    PlatypusPreview {
        WorkspacePickContent(
            state = state,
            accounts = sampleAccounts,
            activeAccountId = "1",
            activeLabel = "Joel Kanyi",
            onSwitchAccount = {},
            onEvent = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun WorkspacesLoadingPreview() {
    PreviewOf(WorkspacePickUiState(isLoading = true))
}

@PlatypusThemePreviews
@Composable
private fun WorkspacesContentPreview() {
    PreviewOf(WorkspacePickUiState(isLoading = false, workspaces = sampleWorkspaces))
}

@PlatypusThemePreviews
@Composable
private fun WorkspacesEmptyPreview() {
    PreviewOf(WorkspacePickUiState(isLoading = false, workspaces = emptyList()))
}

@PlatypusThemePreviews
@Composable
private fun WorkspacesErrorPreview() {
    PreviewOf(WorkspacePickUiState(isLoading = false, error = "Network problem. Check your connection."))
}
