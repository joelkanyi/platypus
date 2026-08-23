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
package com.joelkanyi.platypus.feature.pr.files

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joelkanyi.platypus.app.LocalPlatypusDependencies
import com.joelkanyi.platypus.core.result.NetworkResult
import com.joelkanyi.platypus.core.result.getOrNull
import com.joelkanyi.platypus.core.result.userMessage
import com.joelkanyi.platypus.designsystem.DiffRow
import com.joelkanyi.platypus.designsystem.DiffRowType
import com.joelkanyi.platypus.designsystem.PlatypusMarkdown
import com.joelkanyi.platypus.designsystem.parseDiffRows
import com.joelkanyi.platypus.designsystem.rememberCodeFontFamily
import com.joelkanyi.platypus.domain.model.PrComment
import com.joelkanyi.platypus.domain.model.PrDiffFile
import com.joelkanyi.platypus.domain.repository.PullRequestRepository
import io.github.joelkanyi.jenga.component.avatar.JengaAvatar
import io.github.joelkanyi.jenga.component.avatar.JengaAvatarSize
import io.github.joelkanyi.jenga.component.button.JengaButton
import io.github.joelkanyi.jenga.component.button.JengaButtonVariant
import io.github.joelkanyi.jenga.component.button.JengaIconButton
import io.github.joelkanyi.jenga.component.chip.JengaChip
import io.github.joelkanyi.jenga.component.feedback.JengaBottomSheet
import io.github.joelkanyi.jenga.component.icon.JengaIcon
import io.github.joelkanyi.jenga.component.icon.JengaIcons
import io.github.joelkanyi.jenga.component.progress.jengaShimmer
import io.github.joelkanyi.jenga.component.scaffold.JengaScaffold
import io.github.joelkanyi.jenga.component.scaffold.JengaTopAppBar
import io.github.joelkanyi.jenga.component.state.JengaEmptyState
import io.github.joelkanyi.jenga.component.state.JengaErrorState
import io.github.joelkanyi.jenga.component.text.JengaText
import io.github.joelkanyi.jenga.component.textfield.JengaTextField
import io.github.joelkanyi.jenga.theme.JengaTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max

@Immutable
data class PrFileDiffUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val file: PrDiffFile? = null,
    val comments: List<PrComment> = emptyList(),
    val wrap: Boolean = false,
    val composerLine: Int? = null,
    val draft: String = "",
    val posting: Boolean = false,
    val actionError: String? = null,
)

class PrFileDiffViewModel(
    private val repository: PullRequestRepository,
    private val accountId: String,
    private val workspace: String,
    private val repoSlug: String,
    private val prId: Long,
    private val path: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrFileDiffUiState())
    val uiState: StateFlow<PrFileDiffUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun toggleWrap() = _uiState.update { it.copy(wrap = !it.wrap) }

    fun startComment(line: Int?) = _uiState.update { it.copy(composerLine = line, draft = "") }

    fun cancelComment() = _uiState.update { it.copy(composerLine = null, draft = "") }

    fun draftChanged(text: String) = _uiState.update { it.copy(draft = text) }

    fun dismissError() = _uiState.update { it.copy(actionError = null) }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.diff(accountId, workspace, repoSlug, prId)) {
                is NetworkResult.Success ->
                    _uiState.update {
                        it.copy(isLoading = false, file = result.data.files.firstOrNull { f -> f.path == path })
                    }
                is NetworkResult.Failure -> _uiState.update { it.copy(isLoading = false, error = result.userMessage()) }
            }
            loadComments()
        }
    }

    private suspend fun loadComments() {
        val all = repository.comments(accountId, workspace, repoSlug, prId).getOrNull() ?: return
        _uiState.update { it.copy(comments = all.filter { c -> c.inlinePath == path }) }
    }

    fun postComment() {
        val state = _uiState.value
        val raw = state.draft.trim()
        val line = state.composerLine
        if (raw.isEmpty() || state.posting) return
        _uiState.update { it.copy(posting = true) }
        viewModelScope.launch {
            val result = repository.addComment(
                accountId = accountId,
                workspaceSlug = workspace,
                repoSlug = repoSlug,
                id = prId,
                raw = raw,
                parentId = null,
                inlinePath = path,
                inlineTo = line,
            )
            when (result) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(posting = false, composerLine = null, draft = "") }
                    loadComments()
                }
                is NetworkResult.Failure ->
                    _uiState.update { it.copy(posting = false, actionError = result.userMessage()) }
            }
        }
    }
}

@Composable
fun PrFileDiffScreen(
    accountId: String,
    workspace: String,
    repoSlug: String,
    prId: Long,
    path: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dependencies = LocalPlatypusDependencies.current
    val viewModel = viewModel(key = "filediff/$accountId/$workspace/$repoSlug/$prId/$path") {
        PrFileDiffViewModel(dependencies.pullRequestRepository, accountId, workspace, repoSlug, prId, path)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PrFileDiffContent(
        path = path,
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onToggleWrap = viewModel::toggleWrap,
        onStartComment = viewModel::startComment,
        onCancelComment = viewModel::cancelComment,
        onDraftChanged = viewModel::draftChanged,
        onPostComment = viewModel::postComment,
        modifier = modifier,
    )
}

@Composable
internal fun PrFileDiffContent(
    path: String,
    state: PrFileDiffUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleWrap: () -> Unit,
    onStartComment: (Int?) -> Unit,
    onCancelComment: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onPostComment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    JengaScaffold(
        modifier = modifier,
        topBar = {
            JengaTopAppBar(
                title = path.substringAfterLast('/'),
                subtitle = path.substringBeforeLast('/', "").ifBlank { null },
                navigationIcon = {
                    JengaIconButton(onClick = onBack) {
                        JengaIcon(JengaIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    JengaChip(
                        label = if (state.wrap) "Wrap" else "No wrap",
                        selected = state.wrap,
                        onClick = onToggleWrap,
                    )
                },
            )
        },
    ) { innerPadding ->
        when {
            state.error != null -> JengaErrorState(
                title = "Couldn't load diff",
                description = state.error,
                actionLabel = "Try again",
                onAction = onRetry,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            state.file != null -> DiffWithComments(
                file = state.file,
                comments = state.comments,
                wrap = state.wrap,
                onLongPressLine = onStartComment,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            !state.isLoading -> JengaEmptyState(
                title = "File not found",
                description = "This file is no longer part of the pull request.",
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            else -> DiffSkeleton(modifier = Modifier.fillMaxSize().padding(innerPadding))
        }
    }

    if (state.composerLine != null) {
        InlineComposerSheet(
            line = state.composerLine,
            draft = state.draft,
            posting = state.posting,
            error = state.actionError,
            onDraftChanged = onDraftChanged,
            onPost = onPostComment,
            onDismiss = onCancelComment,
        )
    }
}

private sealed interface DiffDisplayItem {
    data class Code(val row: DiffRow) : DiffDisplayItem

    data class Comment(val comment: PrComment) : DiffDisplayItem
}

@Composable
private fun DiffWithComments(
    file: PrDiffFile,
    comments: List<PrComment>,
    wrap: Boolean,
    onLongPressLine: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = remember(file) { parseDiffRows(file.lines) }
    val byLine = remember(comments) { comments.filter { it.parentId == null }.groupBy { it.inlineTo } }
    val repliesByParent = remember(comments) { comments.filter { it.parentId != null }.groupBy { it.parentId } }
    val display = remember(rows, byLine) {
        buildList {
            rows.forEach { row ->
                add(DiffDisplayItem.Code(row))
                row.newLine?.let { ln ->
                    byLine[ln]?.forEach { comment ->
                        add(DiffDisplayItem.Comment(comment))
                        repliesByParent[comment.id]?.forEach { add(DiffDisplayItem.Comment(it)) }
                    }
                }
            }
            byLine[null]?.forEach { add(DiffDisplayItem.Comment(it)) }
        }
    }

    val mono = rememberCodeFontFamily()
    val codeStyle = remember(mono) { TextStyle(fontFamily = mono, fontSize = 13.sp, lineHeight = 13.sp * 1.5f) }
    val hScroll = rememberScrollState()
    val density = LocalDensity.current
    val charWidth = with(density) { (13.sp.toPx() * 0.6f).toDp() }
    val maxLineDigits = max(2, (rows.maxOfOrNull { max(it.oldLine ?: 0, it.newLine ?: 0) } ?: 0).toString().length)
    val gutterWidth = (maxLineDigits * 9 + 12).dp
    val maxChars = remember(rows) { rows.maxOfOrNull { it.text.length } ?: 0 }
    val codeWidth = charWidth * maxChars + 24.dp

    LazyColumn(modifier = modifier) {
        item { CommentHint() }
        items(display.size) { index ->
            when (val item = display[index]) {
                is DiffDisplayItem.Code -> DiffCodeRow(
                    row = item.row,
                    codeStyle = codeStyle,
                    gutterWidth = gutterWidth,
                    codeWidth = codeWidth,
                    wrap = wrap,
                    hScroll = hScroll,
                    onLongPress = { onLongPressLine(item.row.newLine ?: item.row.oldLine) },
                )
                is DiffDisplayItem.Comment -> InlineCommentRow(item.comment)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiffCodeRow(
    row: DiffRow,
    codeStyle: TextStyle,
    gutterWidth: Dp,
    codeWidth: Dp,
    wrap: Boolean,
    hScroll: ScrollState,
    onLongPress: () -> Unit,
) {
    val colors = JengaTheme.colors
    if (row.type == DiffRowType.HUNK) {
        JengaText(
            text = row.text,
            style = codeStyle,
            color = colors.textMuted,
            softWrap = false,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().background(
                colors.surfaceVariant,
            ).padding(horizontal = 8.dp, vertical = 4.dp),
        )
        return
    }
    val tint = when (row.type) {
        DiffRowType.ADD -> colors.successContainer
        DiffRowType.DELETE -> colors.errorContainer
        else -> colors.background
    }
    val sign = when (row.type) {
        DiffRowType.ADD -> "+"
        DiffRowType.DELETE -> "-"
        else -> " "
    }
    val signColor = when (row.type) {
        DiffRowType.ADD -> colors.success
        DiffRowType.DELETE -> colors.error
        else -> colors.textMuted
    }
    val clickable = Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress)
    if (wrap) {
        Row(modifier = Modifier.fillMaxWidth().background(tint).then(clickable)) {
            Gutter(row.oldLine, gutterWidth, codeStyle, colors.textMuted)
            Gutter(row.newLine, gutterWidth, codeStyle, colors.textMuted)
            JengaText(sign, style = codeStyle, color = signColor, modifier = Modifier.width(16.dp))
            JengaText(
                text = row.text.ifEmpty { " " },
                style = codeStyle,
                color = colors.textPrimary,
                softWrap = true,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(hScroll).then(clickable)) {
            Row(modifier = Modifier.background(tint)) {
                Gutter(row.oldLine, gutterWidth, codeStyle, colors.textMuted)
                Gutter(row.newLine, gutterWidth, codeStyle, colors.textMuted)
                JengaText(sign, style = codeStyle, color = signColor, modifier = Modifier.width(16.dp))
                JengaText(
                    text = row.text.ifEmpty { " " },
                    style = codeStyle,
                    color = colors.textPrimary,
                    softWrap = false,
                    maxLines = 1,
                    modifier = Modifier.width(codeWidth),
                )
            }
        }
    }
}

@Composable
private fun Gutter(number: Int?, gutterWidth: Dp, style: TextStyle, color: Color) {
    JengaText(
        text = number?.toString().orEmpty(),
        style = style,
        color = color,
        textAlign = TextAlign.End,
        softWrap = false,
        maxLines = 1,
        modifier = Modifier.width(gutterWidth).padding(end = 6.dp),
    )
}

@Composable
private fun CommentHint() {
    val spacing = JengaTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JengaTheme.colors.surfaceVariant)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        JengaIcon(
            JengaIcons.MessageCircle,
            contentDescription = null,
            tint = JengaTheme.colors.textMuted,
        )
        JengaText(
            text = "Long-press any line to comment on it",
            style = JengaTheme.typography.caption,
            color = JengaTheme.colors.textMuted,
        )
    }
}

@Composable
private fun DiffSkeleton(modifier: Modifier = Modifier) {
    val spacing = JengaTheme.spacing
    Column(
        modifier = modifier.padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        repeat(14) { index ->
            Box(
                Modifier
                    .height(14.dp)
                    .fillMaxWidth(WIDTH_FRACTIONS[index % WIDTH_FRACTIONS.size])
                    .clip(JengaTheme.shapes.control)
                    .jengaShimmer(),
            )
        }
    }
}

private val WIDTH_FRACTIONS = listOf(0.9f, 0.6f, 0.75f, 0.5f, 0.85f, 0.4f, 0.7f)

@Composable
private fun InlineCommentRow(comment: PrComment) {
    val spacing = JengaTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JengaTheme.colors.surfaceVariant)
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .padding(start = if (comment.parentId != null) spacing.lg else 0.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        JengaAvatar(name = comment.authorName, size = JengaAvatarSize.Small)
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            JengaText(
                text = comment.authorName,
                style = JengaTheme.typography.bodySmall,
                color = JengaTheme.colors.textPrimary,
            )
            PlatypusMarkdown(content = comment.content)
        }
    }
}

@Composable
private fun InlineComposerSheet(
    line: Int?,
    draft: String,
    posting: Boolean,
    error: String?,
    onDraftChanged: (String) -> Unit,
    onPost: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = JengaTheme.spacing
    JengaBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().imePadding().padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            JengaText(
                text = line?.let { "Comment on line $it" } ?: "Comment on this file",
                style = JengaTheme.typography.titleMedium,
            )
            error?.let {
                JengaText(text = it, style = JengaTheme.typography.bodySmall, color = JengaTheme.colors.error)
            }
            JengaTextField(
                value = draft,
                onValueChange = onDraftChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Leave a comment",
                singleLine = false,
                enabled = !posting,
            )
            JengaButton(
                text = "Comment",
                onClick = onPost,
                modifier = Modifier.fillMaxWidth(),
                variant = JengaButtonVariant.Primary,
                enabled = draft.isNotBlank() && !posting,
                loading = posting,
            )
        }
    }
}
