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
package com.joelkanyi.platypus.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.designsystem.expand
import com.joelkanyi.platypus.domain.model.AppSettings
import com.joelkanyi.platypus.domain.model.CodeFontSize
import com.joelkanyi.platypus.domain.model.InboxFilter
import com.joelkanyi.platypus.domain.model.MergeStrategy
import com.joelkanyi.platypus.domain.model.RepoTab
import com.joelkanyi.platypus.domain.model.ThemeMode
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.chip.JengaChip
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.selection.JengaToggle
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun SettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val dependencies = LocalPlatypusDependencies.current
    val store = dependencies.settingsStore
    val settings by store.settings.collectAsStateWithLifecycle()
    val appLockAvailable by produceState(false) { value = dependencies.biometrics.isAvailable() }
    SettingsContent(
        settings = settings,
        onUpdate = store::update,
        onBack = onBack,
        appLockAvailable = appLockAvailable,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsContent(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit,
    onBack: () -> Unit,
    appLockAvailable: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = "Settings",
                navigationIcon = {
                    JengaIconButton(onClick = onBack) {
                        JengaIcon(JengaIcons.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding.expand(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            item { SectionLabel("Appearance") }
            item {
                ChoiceRow(
                    title = "Theme",
                    options = ThemeMode.entries,
                    selected = settings.theme,
                    label = ::themeLabel,
                    onSelect = { onUpdate(settings.copy(theme = it)) },
                )
            }
            item {
                ChoiceRow(
                    title = "Code font size",
                    options = CodeFontSize.entries,
                    selected = settings.codeFontSize,
                    label = ::fontSizeLabel,
                    onSelect = { onUpdate(settings.copy(codeFontSize = it)) },
                )
            }
            item {
                ToggleRow(
                    title = "Wrap long lines",
                    supporting = "Applies to code and diffs everywhere",
                    checked = settings.wrapCode,
                    onCheckedChange = { onUpdate(settings.copy(wrapCode = it)) },
                )
            }
            item {
                ToggleRow(
                    title = "Render markdown by default",
                    supporting = "Show READMEs and .md files rendered, not as source",
                    checked = settings.renderMarkdownByDefault,
                    onCheckedChange = { onUpdate(settings.copy(renderMarkdownByDefault = it)) },
                )
            }

            item { SectionLabel("Defaults") }
            item {
                ChoiceRow(
                    title = "Inbox filter",
                    options = InboxFilter.entries,
                    selected = settings.defaultInboxFilter,
                    label = ::inboxFilterLabel,
                    onSelect = { onUpdate(settings.copy(defaultInboxFilter = it)) },
                )
            }
            item {
                ChoiceRow(
                    title = "Repositories view",
                    options = RepoTab.entries,
                    selected = settings.defaultReposTab,
                    label = ::repoTabLabel,
                    onSelect = { onUpdate(settings.copy(defaultReposTab = it)) },
                )
            }
            item {
                ChoiceRow(
                    title = "Merge strategy",
                    options = MergeStrategy.entries,
                    selected = settings.defaultMergeStrategy,
                    label = ::mergeStrategyLabel,
                    onSelect = { onUpdate(settings.copy(defaultMergeStrategy = it)) },
                )
            }
            item {
                ToggleRow(
                    title = "Close source branch on merge",
                    supporting = null,
                    checked = settings.closeSourceBranchOnMerge,
                    onCheckedChange = { onUpdate(settings.copy(closeSourceBranchOnMerge = it)) },
                )
            }

            item { SectionLabel("Security") }
            item {
                ToggleRow(
                    title = "Require unlock",
                    supporting = if (appLockAvailable) {
                        "Unlock with biometrics or device credential each time you open Platypus"
                    } else {
                        "Set up a fingerprint, face, or screen lock on this device to enable"
                    },
                    checked = settings.appLockEnabled && appLockAvailable,
                    enabled = appLockAvailable,
                    onCheckedChange = { onUpdate(settings.copy(appLockEnabled = it)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    val spacing = JengaTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        JengaText(text = title, style = JengaTheme.typography.bodyMedium, color = JengaTheme.colors.textPrimary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            options.forEach { option ->
                JengaChip(
                    label = label(option),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    supporting: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    JengaListItem(
        headline = title,
        supporting = supporting,
        trailingContent = { JengaToggle(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionLabel(text: String) {
    JengaText(
        text = text.uppercase(),
        style = JengaTheme.typography.caption,
        color = JengaTheme.colors.textMuted,
    )
}

private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun fontSizeLabel(size: CodeFontSize): String = when (size) {
    CodeFontSize.SMALL -> "Small"
    CodeFontSize.MEDIUM -> "Medium"
    CodeFontSize.LARGE -> "Large"
}

private fun inboxFilterLabel(filter: InboxFilter): String = when (filter) {
    InboxFilter.TO_REVIEW -> "To review"
    InboxFilter.MINE -> "Mine"
    InboxFilter.ALL -> "All"
}

private fun repoTabLabel(tab: RepoTab): String = when (tab) {
    RepoTab.WATCHING -> "Watching"
    RepoTab.BROWSE -> "Browse"
}

private fun mergeStrategyLabel(strategy: MergeStrategy): String = when (strategy) {
    MergeStrategy.MERGE_COMMIT -> "Merge commit"
    MergeStrategy.SQUASH -> "Squash"
    MergeStrategy.FAST_FORWARD -> "Fast forward"
}
