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

import androidx.compose.runtime.Immutable
import com.joelkanyi.platypus.domain.model.ActivityItem
import com.joelkanyi.platypus.domain.model.MergeStrategy
import com.joelkanyi.platypus.domain.model.PrApproval
import com.joelkanyi.platypus.domain.model.PrComment
import com.joelkanyi.platypus.domain.model.PullRequestDetail

@Immutable
data class PrDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val detail: PullRequestDetail? = null,
    val hasConflicts: Boolean = false,
    val activity: List<ActivityItem> = emptyList(),
    val commentDraft: String = "",
    val replyingTo: PrComment? = null,
    val postingComment: Boolean = false,
    val actionInProgress: Boolean = false,
    val actionError: String? = null,
    val descriptionExpanded: Boolean = false,
    val readinessExpanded: Boolean = false,
    val reviewersExpanded: Boolean = false,
    val showReviewSheet: Boolean = false,
    val showMergeSheet: Boolean = false,
    val showDeclineDialog: Boolean = false,
    val mergeStrategy: MergeStrategy = MergeStrategy.MERGE_COMMIT,
    val closeSourceBranch: Boolean = false,
) {
    val canAct: Boolean get() = detail?.isOpen == true && !actionInProgress

    val isApproved: Boolean get() = detail?.myApproval == PrApproval.APPROVED

    val hasRequestedChanges: Boolean get() = detail?.myApproval == PrApproval.CHANGES_REQUESTED

    val canReview: Boolean get() = detail?.isAuthoredByMe == false
}

sealed interface PrDetailEvent {
    data object Retry : PrDetailEvent

    data object ToggleApprove : PrDetailEvent

    data object ToggleRequestChanges : PrDetailEvent

    data class CommentDraftChanged(val text: String) : PrDetailEvent

    data class StartReply(val comment: PrComment) : PrDetailEvent

    data object CancelReply : PrDetailEvent

    data object PostComment : PrDetailEvent

    data class ResolveComment(val comment: PrComment) : PrDetailEvent

    data class ToggleDescription(val expanded: Boolean) : PrDetailEvent

    data class ToggleReadiness(val expanded: Boolean) : PrDetailEvent

    data class ToggleReviewers(val expanded: Boolean) : PrDetailEvent

    data object OpenReviewSheet : PrDetailEvent

    data object DismissReviewSheet : PrDetailEvent

    data object OpenMergeSheet : PrDetailEvent

    data object DismissMergeSheet : PrDetailEvent

    data class SelectMergeStrategy(val strategy: MergeStrategy) : PrDetailEvent

    data class ToggleCloseSourceBranch(val close: Boolean) : PrDetailEvent

    data object ConfirmMerge : PrDetailEvent

    data object OpenDeclineDialog : PrDetailEvent

    data object DismissDeclineDialog : PrDetailEvent

    data object ConfirmDecline : PrDetailEvent

    data object DismissActionError : PrDetailEvent
}
