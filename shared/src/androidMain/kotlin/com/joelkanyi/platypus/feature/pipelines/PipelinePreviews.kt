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

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.joelkanyi.platypus.domain.model.Pipeline
import com.joelkanyi.platypus.domain.model.PipelineStatus
import com.joelkanyi.platypus.domain.model.PipelineStep
import com.joelkanyi.platypus.domain.model.RefType
import com.joelkanyi.platypus.preview.PlatypusPreview
import com.joelkanyi.platypus.preview.PlatypusThemePreviews

private fun pipeline(build: Long, status: PipelineStatus) = Pipeline(
    uuid = "{$build}",
    buildNumber = build,
    status = status,
    triggerName = "PUSH",
    refType = RefType.BRANCH,
    refName = "main",
    commitHash = "a1b2c3d4e5f6",
    commitMessage = "Add retry to token refresh",
    selectorPattern = null,
    pullRequestId = null,
    creatorName = "Grace Njeri",
    creatorAvatarUrl = null,
    createdOn = "2026-08-23T09:00:00+00:00",
    completedOn = "2026-08-23T09:04:00+00:00",
    durationSeconds = 244,
)

private val samplePipelines = listOf(
    pipeline(42, PipelineStatus.SUCCESSFUL),
    pipeline(41, PipelineStatus.FAILED),
    pipeline(40, PipelineStatus.IN_PROGRESS),
    pipeline(39, PipelineStatus.STOPPED),
)

private val sampleSteps = listOf(
    PipelineStep("{s1}", "Build", PipelineStatus.SUCCESSFUL, null, null, 120),
    PipelineStep("{s2}", "Unit Tests", PipelineStatus.FAILED, null, null, 90),
    PipelineStep("{s3}", "Deploy", PipelineStatus.PENDING, null, null, 0),
)

@PlatypusThemePreviews
@Composable
private fun PipelineListPreview() {
    PlatypusPreview {
        PipelineListContent(
            repoName = "API Gateway",
            state = PipelineListUiState(isLoading = false, pipelines = samplePipelines),
            onBack = {},
            onRetry = {},
            onRefresh = {},
            onSelectFilter = {},
            onOpenPipeline = {},
            onRunClick = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun PipelineListEmptyPreview() {
    PlatypusPreview {
        PipelineListContent(
            repoName = "API Gateway",
            state = PipelineListUiState(isLoading = false, pipelines = emptyList()),
            onBack = {},
            onRetry = {},
            onRefresh = {},
            onSelectFilter = {},
            onOpenPipeline = {},
            onRunClick = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun PipelineDetailFailedPreview() {
    PlatypusPreview {
        PipelineDetailContent(
            buildNumber = 41,
            repoName = "api-gateway",
            state = PipelineDetailUiState(
                isLoading = false,
                pipeline = pipeline(41, PipelineStatus.FAILED),
                steps = sampleSteps,
            ),
            onBack = {},
            onRetry = {},
            onStop = {},
            onRerun = {},
            onDismissActionError = {},
            onOpenStepLog = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun PipelineDetailRunningPreview() {
    PlatypusPreview {
        PipelineDetailContent(
            buildNumber = 40,
            repoName = "api-gateway",
            state = PipelineDetailUiState(
                isLoading = false,
                pipeline = pipeline(40, PipelineStatus.IN_PROGRESS),
                steps = sampleSteps,
            ),
            onBack = {},
            onRetry = {},
            onStop = {},
            onRerun = {},
            onDismissActionError = {},
            onOpenStepLog = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun StepLogPreview() {
    PlatypusPreview {
        StepLogContent(
            stepName = "Unit Tests",
            state = StepLogUiState(
                isLoading = false,
                raw = "+ ./gradlew testDebugUnitTest\n" +
                    "> Task :app:compileKotlin\n" +
                    "FAILED: 1 test failed\n" +
                    "BUILD FAILED in 2m 4s",
            ),
            initialWrap = true,
            fontSize = 13.sp,
            onRetry = {},
            onBack = {},
        )
    }
}
