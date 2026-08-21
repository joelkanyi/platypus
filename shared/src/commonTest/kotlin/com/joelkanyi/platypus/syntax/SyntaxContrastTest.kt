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
package com.joelkanyi.platypus.syntax

import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

private fun channel(component: Double): Double {
    val cs = component / 255.0
    return if (cs <= 0.03928) cs / 12.92 else ((cs + 0.055) / 1.055).pow(2.4)
}

private fun relativeLuminance(argb: Long): Double {
    val r = ((argb shr 16) and 0xFF).toDouble()
    val g = ((argb shr 8) and 0xFF).toDouble()
    val b = (argb and 0xFF).toDouble()
    return 0.2126 * channel(r) + 0.7152 * channel(g) + 0.0722 * channel(b)
}

private fun contrast(a: Long, b: Long): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    val lighter = maxOf(la, lb)
    val darker = minOf(la, lb)
    return (lighter + 0.05) / (darker + 0.05)
}

private const val AA = 4.5

class SyntaxContrastTest {

    @Test
    fun everyLightTokenMeetsAa() {
        val ground = SyntaxPalette.LIGHT_GROUND
        (SyntaxPalette.light + (TokenRole.KEYWORD to SyntaxPalette.LIGHT_FOREGROUND)).forEach { (role, color) ->
            val ratio = contrast(color, ground)
            assertTrue(ratio >= AA, "light $role contrast $ratio < $AA")
        }
        assertTrue(contrast(SyntaxPalette.LIGHT_FOREGROUND, ground) >= AA)
    }

    @Test
    fun everyDarkTokenMeetsAa() {
        val ground = SyntaxPalette.DARK_GROUND
        SyntaxPalette.dark.forEach { (role, color) ->
            val ratio = contrast(color, ground)
            assertTrue(ratio >= AA, "dark $role contrast $ratio < $AA")
        }
        assertTrue(contrast(SyntaxPalette.DARK_FOREGROUND, ground) >= AA)
    }
}
