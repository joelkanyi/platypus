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
package com.joelkanyi.platypus.feature.inbox

import androidx.compose.runtime.Immutable
import com.joelkanyi.platypus.domain.model.InboxFilter
import com.joelkanyi.platypus.domain.model.InboxSourceFailure
import com.joelkanyi.platypus.domain.model.PrRelationship
import com.joelkanyi.platypus.domain.model.PullRequest

@Immutable
data class InboxUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val hasWatchlist: Boolean = true,
    val filter: InboxFilter = InboxFilter.TO_REVIEW,
    val pullRequests: List<PullRequest> = emptyList(),
    val failures: List<InboxSourceFailure> = emptyList(),
    val lastUpdatedEpochMs: Long? = null,
) {
    val toReviewCount: Int get() = pullRequests.count { it.relationship == PrRelationship.TO_REVIEW }

    val mineCount: Int get() = pullRequests.count { it.relationship == PrRelationship.MINE }

    val visible: List<PullRequest>
        get() = when (filter) {
            InboxFilter.TO_REVIEW -> pullRequests.filter { it.relationship == PrRelationship.TO_REVIEW }
            InboxFilter.MINE -> pullRequests.filter { it.relationship == PrRelationship.MINE }
            InboxFilter.ALL -> pullRequests
        }
}

sealed interface InboxUiEvent {
    data class SelectFilter(val filter: InboxFilter) : InboxUiEvent

    data object Refresh : InboxUiEvent

    data object Retry : InboxUiEvent
}
