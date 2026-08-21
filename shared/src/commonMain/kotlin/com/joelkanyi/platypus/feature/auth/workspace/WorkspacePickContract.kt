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

import androidx.compose.runtime.Immutable
import com.joelkanyi.platypus.domain.model.Workspace

@Immutable
data class WorkspacePickUiState(
    val isLoading: Boolean = true,
    val workspaces: List<Workspace> = emptyList(),
    val error: String? = null,
)

sealed interface WorkspacePickUiEvent {
    data object Retry : WorkspacePickUiEvent

    data object SignOut : WorkspacePickUiEvent

    data class Select(val workspace: Workspace) : WorkspacePickUiEvent
}

sealed interface WorkspacePickUiEffect {
    data class Selected(val workspace: Workspace) : WorkspacePickUiEffect
}
