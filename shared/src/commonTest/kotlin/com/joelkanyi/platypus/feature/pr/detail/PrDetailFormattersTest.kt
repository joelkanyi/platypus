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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PrDetailFormattersTest {

    private fun reviewer(approval: PrApproval) =
        PrReviewer(uuid = "u", name = "n", avatarUrl = null, approval = approval)

    @Test
    fun reviewerSummaryCountsEachApprovalStateAndFallsBackWhenEmpty() {
        assertEquals("No responses yet", reviewerSummary(emptyList()))
        assertEquals(
            "1 approved · 2 changes requested · 1 pending",
            reviewerSummary(
                listOf(
                    reviewer(PrApproval.APPROVED),
                    reviewer(PrApproval.CHANGES_REQUESTED),
                    reviewer(PrApproval.CHANGES_REQUESTED),
                    reviewer(PrApproval.NONE),
                ),
            ),
        )
    }

    @Test
    fun mergeStrategyLabelAndHintCoverEveryStrategy() {
        MergeStrategy.entries.forEach { strategy ->
            assertEquals(true, mergeStrategyLabel(strategy).isNotBlank())
            assertEquals(true, mergeStrategyHint(strategy).isNotBlank())
        }
        assertEquals("Squash", mergeStrategyLabel(MergeStrategy.SQUASH))
    }

    @Test
    fun stateLozengeMapsKnownStatesAndDropsOther() {
        assertEquals("Merged" to JengaBadgeTone.Brand, stateLozenge(PrState.MERGED))
        assertEquals("Open" to JengaBadgeTone.Success, stateLozenge(PrState.OPEN))
        assertNull(stateLozenge(PrState.OTHER))
    }

    @Test
    fun approvalPillMapsEveryApproval() {
        assertEquals("Approved" to JengaBadgeTone.Success, approvalPill(PrApproval.APPROVED))
        assertEquals("Changes requested" to JengaBadgeTone.Warning, approvalPill(PrApproval.CHANGES_REQUESTED))
        assertEquals("Pending" to JengaBadgeTone.Neutral, approvalPill(PrApproval.NONE))
    }
}
