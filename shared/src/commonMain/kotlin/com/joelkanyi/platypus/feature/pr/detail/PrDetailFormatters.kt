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
package com.joelkanyi.platypus.feature.pr.detail

import com.joelkanyi.platypus.domain.model.MergeStrategy
import com.joelkanyi.platypus.domain.model.PrApproval
import com.joelkanyi.platypus.domain.model.PrReviewer
import com.joelkanyi.platypus.domain.model.PrState
import io.github.joelkanyi.jenga.component.badge.JengaBadgeTone

internal fun reviewerSummary(reviewers: List<PrReviewer>): String {
    val approved = reviewers.count { it.approval == PrApproval.APPROVED }
    val changes = reviewers.count { it.approval == PrApproval.CHANGES_REQUESTED }
    val pending = reviewers.count { it.approval == PrApproval.NONE }
    return buildList {
        if (approved > 0) add("$approved approved")
        if (changes > 0) add("$changes changes requested")
        if (pending > 0) add("$pending pending")
    }.joinToString(" · ").ifEmpty { "No responses yet" }
}

internal fun mergeStrategyLabel(strategy: MergeStrategy): String = when (strategy) {
    MergeStrategy.MERGE_COMMIT -> "Merge commit"
    MergeStrategy.SQUASH -> "Squash"
    MergeStrategy.FAST_FORWARD -> "Fast forward"
}

internal fun mergeStrategyHint(strategy: MergeStrategy): String = when (strategy) {
    MergeStrategy.MERGE_COMMIT -> "Keeps every commit and adds a merge commit."
    MergeStrategy.SQUASH -> "Combines all commits into one on the destination."
    MergeStrategy.FAST_FORWARD -> "Moves the branch pointer, no merge commit."
}

internal fun stateLozenge(state: PrState): Pair<String, JengaBadgeTone>? = when (state) {
    PrState.OPEN -> "Open" to JengaBadgeTone.Success
    PrState.MERGED -> "Merged" to JengaBadgeTone.Brand
    PrState.DECLINED -> "Declined" to JengaBadgeTone.Error
    PrState.SUPERSEDED -> "Superseded" to JengaBadgeTone.Neutral
    PrState.OTHER -> null
}

internal fun approvalPill(approval: PrApproval): Pair<String, JengaBadgeTone> = when (approval) {
    PrApproval.APPROVED -> "Approved" to JengaBadgeTone.Success
    PrApproval.CHANGES_REQUESTED -> "Changes requested" to JengaBadgeTone.Warning
    PrApproval.NONE -> "Pending" to JengaBadgeTone.Neutral
}
