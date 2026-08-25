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
package com.joelkanyi.platypus.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

val platypusNavConfig: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(InboxKey::class, InboxKey.serializer())
            subclass(RepositoriesKey::class, RepositoriesKey.serializer())
            subclass(ProfileKey::class, ProfileKey.serializer())
            subclass(SearchKey::class, SearchKey.serializer())
            subclass(PrivacyKey::class, PrivacyKey.serializer())
            subclass(TermsKey::class, TermsKey.serializer())
            subclass(PullRequestKey::class, PullRequestKey.serializer())
            subclass(PullRequestDiffKey::class, PullRequestDiffKey.serializer())
            subclass(FilesChangedKey::class, FilesChangedKey.serializer())
            subclass(PrFileDiffKey::class, PrFileDiffKey.serializer())
            subclass(PrCommitsKey::class, PrCommitsKey.serializer())
            subclass(RepositoryOverviewKey::class, RepositoryOverviewKey.serializer())
            subclass(RepoPullRequestsKey::class, RepoPullRequestsKey.serializer())
            subclass(RepositoryBrowseKey::class, RepositoryBrowseKey.serializer())
            subclass(FileViewerKey::class, FileViewerKey.serializer())
            subclass(CommitsKey::class, CommitsKey.serializer())
            subclass(CommitDetailKey::class, CommitDetailKey.serializer())
            subclass(PipelinesKey::class, PipelinesKey.serializer())
            subclass(PipelineDetailKey::class, PipelineDetailKey.serializer())
            subclass(PipelineStepLogKey::class, PipelineStepLogKey.serializer())
            subclass(DeploymentsKey::class, DeploymentsKey.serializer())
            subclass(SchedulesKey::class, SchedulesKey.serializer())
        }
    }
}
