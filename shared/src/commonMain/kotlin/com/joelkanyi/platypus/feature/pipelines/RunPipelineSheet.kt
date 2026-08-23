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
package com.joelkanyi.platypus.feature.pipelines

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.joelkanyi.platypus.domain.model.PipelineTriggerRequest
import com.joelkanyi.platypus.domain.model.PipelineVariable
import com.joelkanyi.platypus.domain.model.RefType
import com.joelkanyi.platypus.feature.repo.branches.BranchesSheet
import io.github.joelkanyi.jenga.component.button.JengaButton
import io.github.joelkanyi.jenga.component.button.JengaButtonVariant
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.expandable.JengaExpandableRow
import io.github.joelkanyi.jenga.component.feedback.JengaBottomSheet
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.component.textfield.JengaTextField
import io.github.joelkanyi.jenga.theme.JengaTheme

private class VariableDraft(key: String = "", value: String = "") {
    var key by mutableStateOf(key)
    var value by mutableStateOf(value)
}

@Composable
fun RunPipelineSheet(
    accountId: String,
    workspace: String,
    repoSlug: String,
    isTriggering: Boolean,
    error: String?,
    onRun: (PipelineTriggerRequest) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = JengaTheme.spacing
    val colors = JengaTheme.colors
    var branch by remember { mutableStateOf("") }
    var customPattern by remember { mutableStateOf("") }
    var showBranchPicker by remember { mutableStateOf(false) }
    var variablesExpanded by remember { mutableStateOf(false) }
    val variables = remember { mutableStateListOf<VariableDraft>() }

    JengaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            JengaText(text = "Run pipeline", style = JengaTheme.typography.titleMedium)
            JengaText(
                text = "Pick a branch. Leave the custom pipeline empty to run the default pipeline.",
                style = JengaTheme.typography.bodySmall,
                color = colors.textMuted,
            )

            JengaText(text = "Branch", style = JengaTheme.typography.caption, color = colors.textMuted)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.border, JengaTheme.shapes.control)
                    .clickable { showBranchPicker = true }
                    .padding(horizontal = spacing.md, vertical = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                JengaText(
                    text = branch.ifBlank { "Select a branch" },
                    color = if (branch.isBlank()) colors.textMuted else colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                JengaIcon(JengaIcons.ChevronDown, contentDescription = null, tint = colors.textMuted, size = 18.dp)
            }

            JengaTextField(
                value = customPattern,
                onValueChange = { customPattern = it },
                label = "Custom pipeline (optional)",
                placeholder = "e.g. deploy-production",
                modifier = Modifier.fillMaxWidth(),
            )

            JengaExpandableRow(
                expanded = variablesExpanded,
                onExpandedChange = { variablesExpanded = it },
                header = {
                    JengaText(text = "Variables (${variables.size})", style = JengaTheme.typography.bodyMedium)
                },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    variables.forEachIndexed { index, draft ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            JengaTextField(
                                value = draft.key,
                                onValueChange = { draft.key = it },
                                placeholder = "KEY",
                                modifier = Modifier.weight(1f),
                            )
                            JengaTextField(
                                value = draft.value,
                                onValueChange = { draft.value = it },
                                placeholder = "value",
                                modifier = Modifier.weight(1f),
                            )
                            JengaIconButton(onClick = { variables.removeAt(index) }) {
                                JengaIcon(JengaIcons.Close, contentDescription = "Remove variable", size = 16.dp)
                            }
                        }
                    }
                    JengaButton(
                        text = "Add variable",
                        onClick = { variables.add(VariableDraft()) },
                        variant = JengaButtonVariant.Outline,
                        leadingIcon = { JengaIcon(JengaIcons.Add, contentDescription = null, size = 16.dp) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (error != null) {
                JengaText(text = error, style = JengaTheme.typography.bodySmall, color = colors.error)
            }

            JengaButton(
                text = "Run pipeline",
                onClick = {
                    onRun(
                        PipelineTriggerRequest(
                            refType = RefType.BRANCH,
                            refName = branch.trim(),
                            customPattern = customPattern.trim().takeIf { it.isNotBlank() },
                            variables = variables
                                .filter { it.key.isNotBlank() }
                                .map { PipelineVariable(it.key.trim(), it.value) },
                        ),
                    )
                },
                enabled = branch.isNotBlank() && !isTriggering,
                loading = isTriggering,
                leadingIcon = { JengaIcon(JengaIcons.Flash, contentDescription = null, size = 16.dp) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showBranchPicker) {
        BranchesSheet(
            accountId = accountId,
            workspace = workspace,
            repoSlug = repoSlug,
            currentRef = branch,
            onSelect = {
                branch = it
                showBranchPicker = false
            },
            onDismiss = { showBranchPicker = false },
        )
    }
}
