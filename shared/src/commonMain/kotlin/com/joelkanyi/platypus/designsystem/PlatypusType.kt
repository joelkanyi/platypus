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
package com.joelkanyi.platypus.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.joelkanyi.platypus.shared.resources.Res
import com.joelkanyi.platypus.shared.resources.geist_bold
import com.joelkanyi.platypus.shared.resources.geist_medium
import com.joelkanyi.platypus.shared.resources.geist_mono_medium
import com.joelkanyi.platypus.shared.resources.geist_mono_regular
import com.joelkanyi.platypus.shared.resources.geist_regular
import com.joelkanyi.platypus.shared.resources.geist_semibold
import com.joelkanyi.platypus.shared.resources.jetbrains_mono_medium
import com.joelkanyi.platypus.shared.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font

@Composable
fun rememberGeistFontFamily(): FontFamily = FontFamily(
    Font(Res.font.geist_regular, FontWeight.Normal),
    Font(Res.font.geist_medium, FontWeight.Medium),
    Font(Res.font.geist_semibold, FontWeight.SemiBold),
    Font(Res.font.geist_bold, FontWeight.Bold),
)

@Composable
fun rememberGeistMonoFontFamily(): FontFamily = FontFamily(
    Font(Res.font.geist_mono_regular, FontWeight.Normal),
    Font(Res.font.geist_mono_medium, FontWeight.Medium),
)

/** JetBrains Mono: the code/diff rendering font. */
@Composable
fun rememberCodeFontFamily(): FontFamily = FontFamily(
    Font(Res.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(Res.font.jetbrains_mono_medium, FontWeight.Medium),
)
