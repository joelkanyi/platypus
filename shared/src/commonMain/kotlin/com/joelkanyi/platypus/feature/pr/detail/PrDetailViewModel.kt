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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.getOrNull
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.domain.model.AccountId
import com.joelkanyi.platypus.domain.model.CommitHash
import com.joelkanyi.platypus.domain.model.MergePair
import com.joelkanyi.platypus.domain.model.MergeStrategy
import com.joelkanyi.platypus.domain.model.PrApproval
import com.joelkanyi.platypus.domain.model.PrComment
import com.joelkanyi.platypus.domain.model.PrId
import com.joelkanyi.platypus.domain.model.PrRef
import com.joelkanyi.platypus.domain.model.PullRequestDetail
import com.joelkanyi.platypus.domain.model.RepoRef
import com.joelkanyi.platypus.domain.model.RepoSlug
import com.joelkanyi.platypus.domain.model.WorkspaceSlug
import com.joelkanyi.platypus.domain.repository.PullRequestRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PrDetailViewModel(
    private val repository: PullRequestRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
    private val prId: Long,
    initialMergeStrategy: MergeStrategy,
    private val defaultCloseSourceBranch: Boolean,
) : ViewModel() {

    private val prRef = PrRef(RepoRef(AccountId(accountId), WorkspaceSlug(workspace), RepoSlug(repoSlug)), PrId(prId))

    private val _uiState = MutableStateFlow(PrDetailUiState(mergeStrategy = initialMergeStrategy))
    val uiState: StateFlow<PrDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: PrDetailEvent) {
        when (event) {
            PrDetailEvent.Retry -> load()
            PrDetailEvent.ToggleApprove -> toggleApprove()
            PrDetailEvent.ToggleRequestChanges -> toggleRequestChanges()
            is PrDetailEvent.CommentDraftChanged -> _uiState.update { it.copy(commentDraft = event.text) }
            is PrDetailEvent.StartReply -> _uiState.update { it.copy(replyingTo = event.comment) }
            PrDetailEvent.CancelReply -> _uiState.update { it.copy(replyingTo = null) }
            PrDetailEvent.PostComment -> postComment()
            is PrDetailEvent.ResolveComment -> resolve(event.comment)
            is PrDetailEvent.ToggleDescription -> _uiState.update { it.copy(descriptionExpanded = event.expanded) }
            is PrDetailEvent.ToggleReadiness -> _uiState.update { it.copy(readinessExpanded = event.expanded) }
            is PrDetailEvent.ToggleReviewers -> _uiState.update { it.copy(reviewersExpanded = event.expanded) }
            PrDetailEvent.OpenReviewSheet -> _uiState.update { it.copy(showReviewSheet = true) }
            PrDetailEvent.DismissReviewSheet -> _uiState.update { it.copy(showReviewSheet = false) }
            PrDetailEvent.OpenMergeSheet -> _uiState.update {
                it.copy(showMergeSheet = true, closeSourceBranch = defaultCloseSourceBranch)
            }
            PrDetailEvent.DismissMergeSheet -> _uiState.update { it.copy(showMergeSheet = false) }
            is PrDetailEvent.SelectMergeStrategy -> _uiState.update { it.copy(mergeStrategy = event.strategy) }
            is PrDetailEvent.ToggleCloseSourceBranch -> _uiState.update { it.copy(closeSourceBranch = event.close) }
            PrDetailEvent.ConfirmMerge -> merge()
            PrDetailEvent.OpenDeclineDialog -> _uiState.update { it.copy(showDeclineDialog = true) }
            PrDetailEvent.DismissDeclineDialog -> _uiState.update { it.copy(showDeclineDialog = false) }
            PrDetailEvent.ConfirmDecline -> decline()
            PrDetailEvent.DismissActionError -> _uiState.update { it.copy(actionError = null) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.detail(prRef)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, detail = result.data) }
                    loadActivity()
                    loadConflicts(result.data)
                }
                is NetworkResult.Failure ->
                    _uiState.update { it.copy(isLoading = false, error = result.userMessage()) }
            }
        }
    }

    private suspend fun loadActivity() {
        val activity = repository.activity(prRef).getOrNull() ?: return
        _uiState.update { it.copy(activity = activity.sortedByDescending { item -> item.date }) }
    }

    private fun loadConflicts(detail: PullRequestDetail) {
        if (!detail.isOpen) return
        viewModelScope.launch {
            val conflicts = repository.hasConflicts(
                repo = prRef.repo,
                pair = MergePair(CommitHash(detail.sourceCommit), CommitHash(detail.destinationCommit)),
            ).getOrNull() ?: return@launch
            _uiState.update { it.copy(hasConflicts = conflicts) }
        }
    }

    private fun refreshDetail() {
        viewModelScope.launch {
            repository.detail(prRef).getOrNull()?.let { fresh ->
                _uiState.update { it.copy(detail = fresh) }
            }
            loadActivity()
        }
    }

    private fun resolve(comment: PrComment) {
        viewModelScope.launch {
            val result = repository.resolveComment(
                pr = prRef,
                commentId = comment.id,
                resolve = !comment.resolved,
            )
            when (result) {
                is NetworkResult.Success -> loadActivity()
                is NetworkResult.Failure -> _uiState.update { it.copy(actionError = result.userMessage()) }
            }
        }
    }

    private fun toggleApprove() {
        val current = _uiState.value.detail ?: return
        val approving = current.myApproval != PrApproval.APPROVED
        val previous = current.myApproval
        _uiState.update { it.copy(showReviewSheet = false) }
        setMyApproval(if (approving) PrApproval.APPROVED else PrApproval.NONE)
        viewModelScope.launch {
            val result = if (approving) {
                repository.approve(prRef)
            } else {
                repository.unapprove(prRef)
            }
            when (result) {
                is NetworkResult.Success -> refreshDetail()
                is NetworkResult.Failure -> {
                    setMyApproval(previous)
                    _uiState.update { it.copy(actionError = result.userMessage()) }
                }
            }
        }
    }

    private fun toggleRequestChanges() {
        val current = _uiState.value.detail ?: return
        val requesting = current.myApproval != PrApproval.CHANGES_REQUESTED
        val previous = current.myApproval
        _uiState.update { it.copy(showReviewSheet = false) }
        setMyApproval(if (requesting) PrApproval.CHANGES_REQUESTED else PrApproval.NONE)
        viewModelScope.launch {
            val result = if (requesting) {
                repository.requestChanges(prRef)
            } else {
                repository.unrequestChanges(prRef)
            }
            when (result) {
                is NetworkResult.Success -> refreshDetail()
                is NetworkResult.Failure -> {
                    setMyApproval(previous)
                    _uiState.update { it.copy(actionError = result.userMessage()) }
                }
            }
        }
    }

    private fun postComment() {
        val state = _uiState.value
        val raw = state.commentDraft.trim()
        if (raw.isEmpty() || state.postingComment) return
        _uiState.update { it.copy(postingComment = true) }
        viewModelScope.launch {
            val result = repository.addComment(prRef, raw, state.replyingTo?.id)
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            postingComment = false,
                            commentDraft = "",
                            replyingTo = null,
                            showReviewSheet = false,
                        )
                    }
                    loadActivity()
                }
                is NetworkResult.Failure -> _uiState.update {
                    it.copy(postingComment = false, actionError = result.userMessage())
                }
            }
        }
    }

    private fun merge() {
        val state = _uiState.value
        _uiState.update { it.copy(actionInProgress = true, showMergeSheet = false) }
        viewModelScope.launch {
            val result = repository.merge(
                pr = prRef,
                strategy = state.mergeStrategy,
                message = null,
                closeSourceBranch = state.closeSourceBranch,
            )
            applyTerminalResult(result)
        }
    }

    private fun decline() {
        _uiState.update { it.copy(actionInProgress = true, showDeclineDialog = false) }
        viewModelScope.launch {
            applyTerminalResult(repository.decline(prRef))
        }
    }

    private fun applyTerminalResult(result: NetworkResult<PullRequestDetail>) {
        when (result) {
            is NetworkResult.Success -> {
                _uiState.update { it.copy(actionInProgress = false, detail = result.data) }
                viewModelScope.launch { loadActivity() }
            }
            is NetworkResult.Failure ->
                _uiState.update { it.copy(actionInProgress = false, actionError = result.userMessage()) }
        }
    }

    private fun setMyApproval(approval: PrApproval) {
        _uiState.update { state ->
            val detail = state.detail ?: return@update state
            state.copy(detail = detail.copy(myApproval = approval))
        }
    }
}
