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

enum class TokenRole { KEYWORD, STRING, NUMBER, COMMENT, TYPE, FUNCTION, ANNOTATION }

data class Token(val start: Int, val end: Int, val role: TokenRole)

/**
 * A deliberately simple, line-scoped highlighter. It tokenizes a single line at a time (no multi-line
 * string or block-comment state), which is the standard, cheap approach for a read-only mobile viewer.
 */
class LineHighlighter(private val keywords: Set<String>, private val lineComment: String?) {
    fun tokenize(line: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                lineComment != null && line.startsWith(lineComment, i) -> {
                    tokens += Token(i, line.length, TokenRole.COMMENT)
                    i = line.length
                }

                c == '"' || c == '\'' || c == '`' -> {
                    val start = i
                    i++
                    while (i < line.length && line[i] != c) {
                        if (line[i] == '\\') i++
                        i++
                    }
                    if (i < line.length) i++
                    tokens += Token(start, i, TokenRole.STRING)
                }

                c == '@' && i + 1 < line.length && line[i + 1].isLetter() -> {
                    val start = i
                    i++
                    while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) i++
                    tokens += Token(start, i, TokenRole.ANNOTATION)
                }

                c.isDigit() -> {
                    val start = i
                    while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '.' || line[i] == '_')) i++
                    tokens += Token(start, i, TokenRole.NUMBER)
                }

                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < line.length && (line[i].isLetterOrDigit() || line[i] == '_')) i++
                    val word = line.substring(start, i)
                    val role = when {
                        word in keywords -> TokenRole.KEYWORD
                        word[0].isUpperCase() -> TokenRole.TYPE
                        i < line.length && line[i] == '(' -> TokenRole.FUNCTION
                        else -> null
                    }
                    if (role != null) tokens += Token(start, i, role)
                }

                else -> i++
            }
        }
        return tokens
    }
}

private val KOTLIN_KEYWORDS = setOf(
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is",
    "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof",
    "val", "var", "when", "while", "by", "catch", "constructor", "delegate", "dynamic", "field", "file",
    "finally", "get", "import", "init", "param", "property", "receiver", "set", "setparam", "value", "where",
    "abstract", "actual", "annotation", "companion", "const", "crossinline", "data", "enum", "expect",
    "external", "final", "infix", "inline", "inner", "internal", "lateinit", "noinline", "open", "operator",
    "out", "override", "private", "protected", "public", "reified", "sealed", "suspend", "tailrec", "vararg",
)

private val JAVA_KEYWORDS = setOf(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
    "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if",
    "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private",
    "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
    "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null", "var", "record",
)

private val JS_KEYWORDS = setOf(
    "async", "await", "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete",
    "do", "else", "export", "extends", "false", "finally", "for", "function", "if", "import", "in", "instanceof",
    "let", "new", "null", "of", "return", "super", "switch", "this", "throw", "true", "try", "typeof", "var",
    "void", "while", "with", "yield", "interface", "type", "enum", "implements", "readonly", "as",
)

private val PYTHON_KEYWORDS = setOf(
    "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", "else",
    "except", "False", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "None",
    "nonlocal", "not", "or", "pass", "raise", "return", "True", "try", "while", "with", "yield", "self",
)

private val SWIFT_KEYWORDS = setOf(
    "associatedtype", "class", "deinit", "enum", "extension", "func", "import", "init", "let", "protocol",
    "struct", "subscript", "typealias", "var", "break", "case", "continue", "default", "defer", "do", "else",
    "fallthrough", "for", "guard", "if", "in", "repeat", "return", "switch", "where", "while", "as", "catch",
    "false", "is", "nil", "self", "super", "throw", "throws", "true", "try", "private", "public", "internal",
    "static", "final", "override", "mutating", "some", "any", "weak", "unowned", "lazy", "open",
)

fun highlighterFor(fileName: String): LineHighlighter = when (fileName.substringAfterLast('.', "").lowercase()) {
    "kt", "kts" -> LineHighlighter(KOTLIN_KEYWORDS, "//")
    "java" -> LineHighlighter(JAVA_KEYWORDS, "//")
    "js", "jsx", "ts", "tsx", "mjs", "cjs" -> LineHighlighter(JS_KEYWORDS, "//")
    "py" -> LineHighlighter(PYTHON_KEYWORDS, "#")
    "swift" -> LineHighlighter(SWIFT_KEYWORDS, "//")
    "c", "h", "cpp", "cc", "hpp", "cs", "go", "rs", "kt2" -> LineHighlighter(JAVA_KEYWORDS, "//")
    "sh", "bash", "yml", "yaml", "toml" -> LineHighlighter(emptySet(), "#")
    "json" -> LineHighlighter(emptySet(), null)
    else -> LineHighlighter(emptySet(), "//")
}
