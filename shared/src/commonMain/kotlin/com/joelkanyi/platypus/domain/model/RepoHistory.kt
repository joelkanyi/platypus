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
package com.joelkanyi.platypus.domain.model

data class Branch(val name: String, val targetHash: String)

data class Commit(val hash: String, val message: String, val authorName: String, val date: String) {
    val shortHash: String get() = hash.take(7)

    val subject: String get() = message.lineSequence().firstOrNull().orEmpty()
}

data class CommitPage(val commits: List<Commit>, val next: String?)

data class CommitDetail(val commit: Commit, val diffLines: List<String>)
