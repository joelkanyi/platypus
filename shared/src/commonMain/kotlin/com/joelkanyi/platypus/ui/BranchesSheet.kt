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
package com.joelkanyi.platypus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import io.github.joelkanyi.jenga.component.feedback.JengaBottomSheet
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.search.JengaSearchField
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun BranchesSheet(
    accountId: String,
    workspace: String,
    repoSlug: String,
    currentRef: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "$accountId/$workspace/$repoSlug/branches") {
        BranchesViewModel(dependencies.repoContentRepository, accountId, workspace, repoSlug)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BranchesSheetContent(state = state, currentRef = currentRef, onSelect = onSelect, onDismiss = onDismiss)
}

@Composable
internal fun BranchesSheetContent(
    state: BranchesUiState,
    currentRef: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = JengaTheme.spacing
    var query by rememberSaveable { mutableStateOf("") }

    JengaBottomSheet(onDismissRequest = onDismiss) {
        JengaText(
            text = "Branches",
            style = JengaTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
        )
        JengaSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Filter branches",
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.xs),
        )
        when {
            state.error != null -> JengaErrorState(
                title = "Couldn't load branches",
                description = state.error,
                modifier = Modifier.fillMaxWidth().padding(spacing.lg),
            )

            state.isLoading -> JengaText(
                text = "Loading...",
                color = JengaTheme.colors.textMuted,
                modifier = Modifier.padding(spacing.lg),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                items(
                    state.branches.filter { it.name.contains(query, ignoreCase = true) },
                    key = { it.name },
                ) { branch ->
                    JengaListItem(
                        headline = branch.name,
                        trailingContent = if (branch.name == currentRef) {
                            { JengaIcon(JengaIcons.Check, contentDescription = "Current") }
                        } else {
                            null
                        },
                        onClick = { onSelect(branch.name) },
                    )
                }
            }
        }
    }
}
