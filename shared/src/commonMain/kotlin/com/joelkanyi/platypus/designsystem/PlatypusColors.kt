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

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.joelkanyi.jenga.foundation.color.JengaColors
import io.github.joelkanyi.jenga.foundation.color.jengaDarkColors
import io.github.joelkanyi.jenga.foundation.color.jengaLightColors
import io.github.joelkanyi.jenga.foundation.shape.JengaShapes

private val Foreground = Color(0xFF0A0A0A)
private val Primary = Color(0xFF171717)
private val PrimaryForeground = Color(0xFFFAFAFA)
private val Muted = Color(0xFFF5F5F5)
private val MutedForeground = Color(0xFF737373)
private val Neutral600 = Color(0xFF525252)
private val Neutral400 = Color(0xFFA3A3A3)
private val Neutral300 = Color(0xFFD4D4D4)
private val Neutral800 = Color(0xFF262626)
private val Neutral900 = Color(0xFF171717)
private val BorderLight = Color(0xFFE5E5E5)
private val Danger = Color(0xFFEF4444)
private val White = Color(0xFFFFFFFF)
private val BackgroundDark = Color(0xFF0A0A0A)

fun platypusLightColors(): JengaColors = jengaLightColors().copy(
    brand = Primary,
    onBrand = PrimaryForeground,
    brandSubtle = Muted,
    onBrandSubtle = Primary,
    ink = Primary,
    onInk = PrimaryForeground,
    background = White,
    surface = White,
    surfaceVariant = Muted,
    surfaceSunk = Muted,
    textPrimary = Foreground,
    textSecondary = Neutral600,
    textMuted = MutedForeground,
    textFaint = Neutral400,
    border = BorderLight,
    borderStrong = Neutral300,
    contentDisabled = Neutral400,
    surfaceDisabled = Muted,
    borderDisabled = BorderLight,
    error = Danger,
    focusRing = Foreground.copy(alpha = 0.40f),
)

fun platypusDarkColors(): JengaColors = jengaDarkColors().copy(
    brand = PrimaryForeground,
    onBrand = Neutral900,
    brandSubtle = Neutral800,
    onBrandSubtle = PrimaryForeground,
    ink = PrimaryForeground,
    onInk = Neutral900,
    background = BackgroundDark,
    surface = BackgroundDark,
    surfaceVariant = Neutral800,
    surfaceSunk = Neutral900,
    textPrimary = PrimaryForeground,
    textSecondary = Neutral300,
    textMuted = Neutral400,
    textFaint = MutedForeground,
    border = Neutral800,
    borderStrong = Color(0xFF404040),
    contentDisabled = MutedForeground,
    surfaceDisabled = Neutral900,
    borderDisabled = Neutral800,
    error = Danger,
    focusRing = Neutral300.copy(alpha = 0.40f),
)

fun platypusShapes(): JengaShapes = JengaShapes(
    sm = RoundedCornerShape(4.dp),
    md = RoundedCornerShape(6.dp),
    lg = RoundedCornerShape(8.dp),
    xl = RoundedCornerShape(12.dp),
    control = RoundedCornerShape(6.dp),
    card = RoundedCornerShape(12.dp),
    cardLarge = RoundedCornerShape(16.dp),
)
