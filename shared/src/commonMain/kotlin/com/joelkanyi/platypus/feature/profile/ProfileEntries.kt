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
package com.joelkanyi.platypus.feature.profile

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.joelkanyi.platypus.navigation.Navigator
import com.joelkanyi.platypus.navigation.PrivacyKey
import com.joelkanyi.platypus.navigation.ProfileKey
import com.joelkanyi.platypus.navigation.TermsKey

fun EntryProviderScope<NavKey>.profileEntries(navigator: Navigator) {
    entry<ProfileKey> {
        ProfileScreen(
            onOpenPrivacy = { navigator.navigate(PrivacyKey) },
            onOpenTerms = { navigator.navigate(TermsKey) },
        )
    }
}
