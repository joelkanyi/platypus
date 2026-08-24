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

/**
 * Shared monospace layout ratios used by the code viewer and the diff view so their line height and
 * horizontal-scroll width math stay identical. [CHAR_ADVANCE_RATIO] approximates the width of one
 * monospace glyph as a fraction of the font size.
 */
object CodeMetrics {
    const val LINE_HEIGHT_RATIO = 1.5f
    const val CHAR_ADVANCE_RATIO = 0.6f
}
