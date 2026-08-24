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

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.joelkanyi.platypus.navigation.CommitDetailKey
import com.joelkanyi.platypus.navigation.DeploymentsKey
import com.joelkanyi.platypus.navigation.Navigator
import com.joelkanyi.platypus.navigation.PipelineDetailKey
import com.joelkanyi.platypus.navigation.PipelineStepLogKey
import com.joelkanyi.platypus.navigation.PipelinesKey
import com.joelkanyi.platypus.navigation.PullRequestKey
import com.joelkanyi.platypus.navigation.SchedulesKey

fun EntryProviderScope<NavKey>.pipelineEntries(navigator: Navigator) {
    entry<DeploymentsKey> { key ->
        DeploymentsScreen(
            accountId = key.accountId,
            workspace = key.workspace,
            repoSlug = key.repoSlug,
            repoName = key.repoName,
            onBack = navigator::goBack,
        )
    }
    entry<SchedulesKey> { key ->
        SchedulesScreen(
            accountId = key.accountId,
            workspace = key.workspace,
            repoSlug = key.repoSlug,
            repoName = key.repoName,
            onBack = navigator::goBack,
        )
    }
    entry<PipelinesKey> { key ->
        PipelineListScreen(
            accountId = key.accountId,
            workspace = key.workspace,
            repoSlug = key.repoSlug,
            repoName = key.repoName,
            onOpenPipeline = { pipeline ->
                navigator.navigate(
                    PipelineDetailKey(
                        key.accountId,
                        key.workspace,
                        key.repoSlug,
                        pipeline.uuid,
                        pipeline.buildNumber,
                    ),
                )
            },
            onBack = navigator::goBack,
        )
    }
    entry<PipelineDetailKey> { key ->
        PipelineDetailScreen(
            accountId = key.accountId,
            workspace = key.workspace,
            repoSlug = key.repoSlug,
            pipelineUuid = key.pipelineUuid,
            buildNumber = key.buildNumber,
            onOpenStepLog = { step ->
                navigator.navigate(
                    PipelineStepLogKey(
                        key.accountId,
                        key.workspace,
                        key.repoSlug,
                        key.pipelineUuid,
                        step.uuid,
                        step.name,
                    ),
                )
            },
            onOpenPipeline = { pipeline ->
                navigator.navigate(
                    PipelineDetailKey(
                        key.accountId,
                        key.workspace,
                        key.repoSlug,
                        pipeline.uuid,
                        pipeline.buildNumber,
                    ),
                )
            },
            onOpenCommit = { hash ->
                navigator.navigate(CommitDetailKey(key.accountId, key.workspace, key.repoSlug, hash))
            },
            onOpenPullRequest = { prId ->
                navigator.navigate(PullRequestKey(key.accountId, key.workspace, key.repoSlug, prId, key.repoSlug))
            },
            onBack = navigator::goBack,
        )
    }
    entry<PipelineStepLogKey> { key ->
        PipelineStepLogScreen(
            accountId = key.accountId,
            workspace = key.workspace,
            repoSlug = key.repoSlug,
            pipelineUuid = key.pipelineUuid,
            stepUuid = key.stepUuid,
            stepName = key.stepName,
            onBack = navigator::goBack,
        )
    }
}
