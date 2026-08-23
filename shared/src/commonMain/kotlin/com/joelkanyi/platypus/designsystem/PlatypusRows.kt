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
package com.joelkanyi.platypus.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.joelkanyi.platypus.domain.model.Commit
import com.joelkanyi.platypus.domain.model.PrRelationship
import com.joelkanyi.platypus.domain.model.PullRequest
import io.github.joelkanyi.jenga.component.avatar.JengaAvatar
import io.github.joelkanyi.jenga.component.avatar.JengaAvatarSize
import io.github.joelkanyi.jenga.component.badge.JengaBadge
import io.github.joelkanyi.jenga.component.badge.JengaBadgeTone
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.list.JengaListItem
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun PlatypusPullRequestRow(
    pullRequest: PullRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showRepo: Boolean = false,
    showRelationship: Boolean = false,
) {
    val spacing = JengaTheme.spacing
    val supporting = buildList {
        if (showRepo) add(pullRequest.repoName)
        add("#${pullRequest.id}")
        add(pullRequest.authorName)
        add(shortDate(pullRequest.updatedOn))
    }.joinToString(" · ")
    JengaListItem(
        modifier = modifier,
        headline = pullRequest.title,
        supporting = supporting,
        leadingContent = { JengaAvatar(name = pullRequest.authorName) },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                if (showRelationship) {
                    relationshipBadge(pullRequest.relationship)?.let { (label, tone) ->
                        JengaBadge(text = label, tone = tone)
                    }
                }
                if (pullRequest.commentCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
                    ) {
                        JengaIcon(JengaIcons.MessageCircle, contentDescription = "Comments")
                        JengaText(
                            text = pullRequest.commentCount.toString(),
                            style = JengaTheme.typography.caption,
                            color = JengaTheme.colors.textMuted,
                        )
                    }
                }
            }
        },
        onClick = onClick,
    )
}

@Composable
fun PlatypusCommitRow(commit: Commit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = JengaTheme.spacing
    JengaListItem(
        modifier = modifier,
        headline = commit.subject,
        supporting = "${commit.authorName} · ${shortDate(commit.date)}",
        leadingContent = { JengaAvatar(name = commit.authorName, size = JengaAvatarSize.Small) },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                JengaBadge(text = commit.shortHash)
                JengaIcon(JengaIcons.ChevronRight, contentDescription = null)
            }
        },
        onClick = onClick,
    )
}

private fun relationshipBadge(relationship: PrRelationship): Pair<String, JengaBadgeTone>? = when (relationship) {
    PrRelationship.TO_REVIEW -> "Review" to JengaBadgeTone.Brand
    PrRelationship.MINE -> "Mine" to JengaBadgeTone.Neutral
    PrRelationship.OTHER -> null
}
