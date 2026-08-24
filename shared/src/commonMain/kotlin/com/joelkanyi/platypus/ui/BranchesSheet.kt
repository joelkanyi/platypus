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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.domain.model.Branch
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
    val spacing = JengaTheme.spacing
    var branches by remember { mutableStateOf<List<Branch>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        when (val result = dependencies.repoContentRepository.branches(accountId, workspace, repoSlug)) {
            is NetworkResult.Success -> branches = result.data
            is NetworkResult.Failure -> error = result.userMessage()
        }
    }

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
        val list = branches
        when {
            error != null -> JengaErrorState(
                title = "Couldn't load branches",
                description = error,
                modifier = Modifier.fillMaxWidth().padding(spacing.lg),
            )

            list == null -> JengaText(
                text = "Loading...",
                color = JengaTheme.colors.textMuted,
                modifier = Modifier.padding(spacing.lg),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                items(list.filter { it.name.contains(query, ignoreCase = true) }, key = { it.name }) { branch ->
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
