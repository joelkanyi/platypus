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
package com.joelkanyi.platypus.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joelkanyi.platypus.app.LocalAccountActions
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.app.PlatypusConfig
import com.joelkanyi.platypus.app.purgeSessionCaches
import com.joelkanyi.platypus.designsystem.PlatypusIcons
import com.joelkanyi.platypus.designsystem.expand
import com.joelkanyi.platypus.domain.model.Account
import com.joelkanyi.platypus.domain.model.AppSettings
import com.joelkanyi.platypus.domain.model.AuthMode
import com.joelkanyi.platypus.domain.model.CodeFontSize
import com.joelkanyi.platypus.domain.model.InboxFilter
import com.joelkanyi.platypus.domain.model.MergeStrategy
import com.joelkanyi.platypus.domain.model.RepoTab
import com.joelkanyi.platypus.domain.model.ThemeMode
import io.github.joelkanyi.jenga.component.avatar.JengaAvatar
import io.github.joelkanyi.jenga.component.avatar.JengaAvatarSize
import io.github.joelkanyi.jenga.component.button.JengaButton
import io.github.joelkanyi.jenga.component.button.JengaButtonVariant
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.card.JengaCard
import io.github.joelkanyi.jenga.component.card.JengaCardVariant
import io.github.joelkanyi.jenga.component.divider.JengaDivider
import io.github.joelkanyi.jenga.component.feedback.JengaBottomSheet
import io.github.joelkanyi.jenga.component.feedback.JengaDialog
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.menu.JengaDropdownMenu
import io.github.joelkanyi.jenga.component.menu.JengaDropdownMenuItem
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.selection.JengaRadioButton
import io.github.joelkanyi.jenga.component.selection.JengaToggle
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlinx.coroutines.launch

private const val ATLASSIAN_ACCOUNT_URL = "https://id.atlassian.com/manage-profile/account-preferences"

@Composable
fun ProfileScreen(onOpenPrivacy: () -> Unit, onOpenTerms: () -> Unit, modifier: Modifier = Modifier) {
    val dependencies = LocalPlatypusDependencies.current
    val accounts by dependencies.authRepository.accounts.collectAsStateWithLifecycle()
    val settings by dependencies.settingsStore.settings.collectAsStateWithLifecycle()
    val appLockAvailable by produceState(false) { value = dependencies.biometrics.isAvailable() }
    val scope = rememberCoroutineScope()

    ProfileContent(
        accounts = accounts,
        settings = settings,
        onUpdate = dependencies.settingsStore::update,
        appLockAvailable = appLockAvailable,
        onSignOut = { accountId ->
            scope.launch {
                dependencies.authRepository.signOut(accountId)
                dependencies.purgeSessionCaches()
            }
        },
        onDeleteAccount = { accountId ->
            scope.launch {
                dependencies.watchlistRepository.clearAccount(accountId)
                dependencies.authRepository.signOut(accountId)
                dependencies.purgeSessionCaches()
            }
        },
        onManageAtlassian = { dependencies.openUrl(ATLASSIAN_ACCOUNT_URL) },
        onOpenPrivacy = onOpenPrivacy,
        onOpenTerms = onOpenTerms,
        modifier = modifier,
    )
}

@Composable
internal fun ProfileContent(
    accounts: List<Account>,
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit,
    onSignOut: (String) -> Unit,
    appLockAvailable: Boolean = false,
    onDeleteAccount: (String) -> Unit = {},
    onManageAtlassian: () -> Unit = {},
    onOpenPrivacy: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val spacing = JengaTheme.spacing
    val sectionModifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
    val addAccount = LocalAccountActions.current.addAccount

    var menuAccountId by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<Account?>(null) }

    JengaScaffold(
        modifier = modifier,
        topBar = { JengaTopAppBar(title = "Account") },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding.expand(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                SettingsSection(title = "Accounts", modifier = sectionModifier) {
                    accounts.forEachIndexed { index, account ->
                        if (index > 0) InsetDivider()
                        AccountRow(
                            account = account,
                            menuOpen = menuAccountId == account.id,
                            onOpenMenu = { menuAccountId = account.id },
                            onDismissMenu = { menuAccountId = null },
                            onSignOut = {
                                menuAccountId = null
                                onSignOut(account.id)
                            },
                            onDelete = {
                                menuAccountId = null
                                deleteTarget = account
                            },
                        )
                    }
                    InsetDivider()
                    JengaListItem(
                        headline = "Add account",
                        supporting = "Sign in another Bitbucket account",
                        leadingContent = { JengaIcon(JengaIcons.Add, contentDescription = null) },
                        onClick = addAccount,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                SettingsSection(title = "Appearance", modifier = sectionModifier) {
                    ChoiceRow(
                        title = "Theme",
                        options = ThemeMode.entries,
                        selected = settings.theme,
                        label = ::themeLabel,
                        onSelect = { onUpdate(settings.copy(theme = it)) },
                    )
                    InsetDivider()
                    ChoiceRow(
                        title = "Code font size",
                        options = CodeFontSize.entries,
                        selected = settings.codeFontSize,
                        label = ::fontSizeLabel,
                        onSelect = { onUpdate(settings.copy(codeFontSize = it)) },
                    )
                    InsetDivider()
                    ToggleRow(
                        title = "Wrap long lines",
                        supporting = "Applies to code and diffs everywhere",
                        checked = settings.wrapCode,
                        onCheckedChange = { onUpdate(settings.copy(wrapCode = it)) },
                    )
                    InsetDivider()
                    ToggleRow(
                        title = "Render markdown by default",
                        supporting = "Show READMEs and .md files rendered, not as source",
                        checked = settings.renderMarkdownByDefault,
                        onCheckedChange = { onUpdate(settings.copy(renderMarkdownByDefault = it)) },
                    )
                }
            }

            item {
                SettingsSection(title = "Defaults", modifier = sectionModifier) {
                    ChoiceRow(
                        title = "Inbox filter",
                        options = InboxFilter.entries,
                        selected = settings.defaultInboxFilter,
                        label = ::inboxFilterLabel,
                        onSelect = { onUpdate(settings.copy(defaultInboxFilter = it)) },
                    )
                    InsetDivider()
                    ChoiceRow(
                        title = "Repositories view",
                        options = RepoTab.entries,
                        selected = settings.defaultReposTab,
                        label = ::repoTabLabel,
                        onSelect = { onUpdate(settings.copy(defaultReposTab = it)) },
                    )
                    InsetDivider()
                    ChoiceRow(
                        title = "Merge strategy",
                        options = MergeStrategy.entries,
                        selected = settings.defaultMergeStrategy,
                        label = ::mergeStrategyLabel,
                        onSelect = { onUpdate(settings.copy(defaultMergeStrategy = it)) },
                    )
                    InsetDivider()
                    ToggleRow(
                        title = "Close source branch on merge",
                        supporting = null,
                        checked = settings.closeSourceBranchOnMerge,
                        onCheckedChange = { onUpdate(settings.copy(closeSourceBranchOnMerge = it)) },
                    )
                }
            }

            item {
                SettingsSection(title = "Security", modifier = sectionModifier) {
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

            item {
                SettingsSection(title = "About", modifier = sectionModifier) {
                    JengaListItem(
                        headline = "Privacy policy",
                        trailingContent = { JengaIcon(JengaIcons.ChevronRight, contentDescription = null) },
                        onClick = onOpenPrivacy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    InsetDivider()
                    JengaListItem(
                        headline = "Terms of use",
                        trailingContent = { JengaIcon(JengaIcons.ChevronRight, contentDescription = null) },
                        onClick = onOpenTerms,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    InsetDivider()
                    JengaListItem(
                        headline = "App version",
                        supporting = PlatypusConfig.VERSION,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    val target = deleteTarget
    if (target != null) {
        JengaDialog(
            onDismissRequest = { deleteTarget = null },
            title = "Delete account?",
            text = "This removes ${target.user.displayName} and all of its data (watched repositories and " +
                "cached pull requests) from this device. Your Atlassian account itself is not affected. " +
                "To delete your Atlassian account, manage it at id.atlassian.com.",
            confirmButton = {
                JengaButton(
                    text = "Delete",
                    variant = JengaButtonVariant.Danger,
                    onClick = {
                        onDeleteAccount(target.id)
                        deleteTarget = null
                    },
                )
            },
            dismissButton = {
                JengaButton(
                    text = "Manage",
                    variant = JengaButtonVariant.Outline,
                    onClick = {
                        onManageAtlassian()
                        deleteTarget = null
                    },
                )
            },
        )
    }
}

@Composable
private fun AccountRow(
    account: Account,
    menuOpen: Boolean,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onSignOut: () -> Unit,
    onDelete: () -> Unit,
) {
    JengaListItem(
        headline = account.user.displayName,
        supporting = "@${account.user.nickname} · ${account.mode.label()}",
        leadingContent = { JengaAvatar(name = account.user.displayName, size = JengaAvatarSize.Large) },
        trailingContent = {
            Box {
                JengaIconButton(onClick = onOpenMenu) {
                    JengaIcon(PlatypusIcons.MoreVertical, contentDescription = "Manage ${account.user.displayName}")
                }
                JengaDropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
                    JengaDropdownMenuItem(
                        text = "Sign out",
                        onClick = onSignOut,
                        leadingIcon = { JengaIcon(JengaIcons.Logout, contentDescription = null) },
                    )
                    JengaDropdownMenuItem(text = "Delete account", onClick = onDelete)
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SettingsSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val spacing = JengaTheme.spacing
    JengaCard(
        variant = JengaCardVariant.Outlined,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = spacing.sm),
    ) {
        JengaText(
            text = title,
            style = JengaTheme.typography.caption,
            color = JengaTheme.colors.textMuted,
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xs),
        )
        content()
    }
}

@Composable
private fun InsetDivider() {
    JengaDivider(modifier = Modifier.padding(horizontal = JengaTheme.spacing.lg))
}

@Composable
private fun <T> ChoiceRow(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    JengaListItem(
        headline = title,
        supporting = label(selected),
        onClick = { open = true },
        modifier = Modifier.fillMaxWidth(),
    )
    if (open) {
        ChoicePickerSheet(
            title = title,
            options = options,
            selected = selected,
            label = label,
            onSelect = {
                open = false
                onSelect(it)
            },
            onDismiss = { open = false },
        )
    }
}

@Composable
private fun <T> ChoicePickerSheet(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = JengaTheme.spacing
    JengaBottomSheet(onDismissRequest = onDismiss) {
        JengaText(
            text = title,
            style = JengaTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
        )
        options.forEach { option ->
            JengaListItem(
                headline = label(option),
                trailingContent = {
                    JengaRadioButton(selected = option == selected, onClick = { onSelect(option) })
                },
                onClick = { onSelect(option) },
                modifier = Modifier.fillMaxWidth(),
            )
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

private fun AuthMode.label(): String = when (this) {
    AuthMode.API_TOKEN -> "API token"
    AuthMode.OAUTH -> "Bitbucket"
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
