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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HighlightingTest {

    private fun roleOf(line: String, word: String, highlighter: LineHighlighter): TokenRole? {
        val start = line.indexOf(word)
        return highlighter.tokenize(line).firstOrNull { it.start == start }?.role
    }

    @Test
    fun kotlinGoldenLine() {
        val highlighter = highlighterFor("Main.kt")
        val line = "    val count: Int = 42 // total"
        assertEquals(TokenRole.KEYWORD, roleOf(line, "val", highlighter))
        assertEquals(TokenRole.TYPE, roleOf(line, "Int", highlighter))
        assertEquals(TokenRole.NUMBER, roleOf(line, "42", highlighter))
        assertEquals(TokenRole.COMMENT, roleOf(line, "// total", highlighter))
    }

    @Test
    fun stringAndAnnotationAndFunction() {
        val highlighter = highlighterFor("Main.kt")
        assertEquals(TokenRole.STRING, roleOf("""val s = "hello"""", "\"hello\"", highlighter))
        assertEquals(TokenRole.ANNOTATION, roleOf("@Inject class A", "@Inject", highlighter))
        assertEquals(TokenRole.FUNCTION, roleOf("    println(x)", "println", highlighter))
    }

    @Test
    fun commentTokenNotEmittedForPythonHash() {
        val highlighter = highlighterFor("script.py")
        val tokens = highlighter.tokenize("x = 1  # note")
        assertTrue(tokens.any { it.role == TokenRole.COMMENT })
        assertEquals(TokenRole.KEYWORD, roleOf("def main():", "def", highlighter))
    }
}
