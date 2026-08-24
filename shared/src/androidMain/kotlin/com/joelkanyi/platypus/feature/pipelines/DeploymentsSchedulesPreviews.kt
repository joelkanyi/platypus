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
import com.joelkanyi.platypus.domain.model.Deployment
import com.joelkanyi.platypus.domain.model.DeploymentStatus
import com.joelkanyi.platypus.domain.model.Schedule
import com.joelkanyi.platypus.preview.PlatypusPreview
import com.joelkanyi.platypus.preview.PlatypusThemePreviews
import kotlinx.collections.immutable.persistentListOf

private val sampleDeployments = persistentListOf(
    Deployment(
        uuid = "{d1}",
        environmentName = "Production",
        environmentType = "Production",
        status = DeploymentStatus.SUCCESSFUL,
        statusLabel = "Successful",
        commitHash = "a1b2c3d4",
        releaseName = "v1.2.0",
        deployerName = "Joel Kanyi",
        updatedOn = "2026-08-23T09:00:00+00:00",
    ),
    Deployment(
        uuid = "{d2}",
        environmentName = "Staging",
        environmentType = "Staging",
        status = DeploymentStatus.IN_PROGRESS,
        statusLabel = "In progress",
        commitHash = "e5f6a7b8",
        releaseName = null,
        deployerName = "Grace Njeri",
        updatedOn = "2026-08-23T08:30:00+00:00",
    ),
)

private val sampleSchedules = persistentListOf(
    Schedule(uuid = "{s1}", enabled = true, refName = "main", selectorPattern = "nightly", cronPattern = "0 0 * * *"),
    Schedule(uuid = "{s2}", enabled = false, refName = "develop", selectorPattern = null, cronPattern = "0 6 * * 1"),
)

@PlatypusThemePreviews
@Composable
private fun DeploymentsPreview() {
    PlatypusPreview {
        DeploymentsContent(
            repoName = "API Gateway",
            state = DeploymentsUiState(isLoading = false, deployments = sampleDeployments),
            onBack = {},
            onRetry = {},
            onRefresh = {},
        )
    }
}

@PlatypusThemePreviews
@Composable
private fun SchedulesPreview() {
    PlatypusPreview {
        SchedulesContent(
            repoName = "API Gateway",
            state = SchedulesUiState(isLoading = false, schedules = sampleSchedules),
            onBack = {},
            onRetry = {},
            onRefresh = {},
        )
    }
}
