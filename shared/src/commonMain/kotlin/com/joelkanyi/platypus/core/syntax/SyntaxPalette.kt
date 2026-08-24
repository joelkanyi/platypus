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
package com.joelkanyi.platypus.core.syntax

/**
 * Syntax token colours as raw ARGB, so contrast can be verified in a pure unit test with no Compose
 * dependency. Each role is chosen to clear WCAG AA (>= 4.5:1) against its ground in both themes; the
 * accompanying contrast test is the guard against a regression like the inverted palette GitHub shipped.
 */
object SyntaxPalette {

    const val LIGHT_GROUND = 0xFFFFFFFFL
    const val LIGHT_FOREGROUND = 0xFF1F2937L
    const val LIGHT_GUTTER = 0xFF6B7280L
    const val DARK_GROUND = 0xFF1B1B1FL
    const val DARK_FOREGROUND = 0xFFE5E7EBL
    const val DARK_GUTTER = 0xFF8B93A1L

    val light: Map<TokenRole, Long> = mapOf(
        TokenRole.KEYWORD to 0xFF7E22CEL,
        TokenRole.STRING to 0xFF15803DL,
        TokenRole.NUMBER to 0xFFB45309L,
        TokenRole.COMMENT to 0xFF57616FL,
        TokenRole.TYPE to 0xFF0E7490L,
        TokenRole.FUNCTION to 0xFF1D4ED8L,
        TokenRole.ANNOTATION to 0xFF9A6700L,
    )

    val dark: Map<TokenRole, Long> = mapOf(
        TokenRole.KEYWORD to 0xFFC4B5FDL,
        TokenRole.STRING to 0xFF86EFACL,
        TokenRole.NUMBER to 0xFFFCD34DL,
        TokenRole.COMMENT to 0xFF9CA3AFL,
        TokenRole.TYPE to 0xFF5EEAD4L,
        TokenRole.FUNCTION to 0xFF93C5FDL,
        TokenRole.ANNOTATION to 0xFFFDE68AL,
    )
}
