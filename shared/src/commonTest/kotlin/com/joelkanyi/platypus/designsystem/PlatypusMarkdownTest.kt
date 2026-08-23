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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatypusMarkdownTest {

    @Test
    fun stripsBitbucketImageAttributeBlock() {
        val input = "![](https://example.com/a.png){: data-width='50' data-layout='align-start' }"
        assertTrue("{:" !in normalizeBitbucketMarkdown(input))
    }

    @Test
    fun rewritesPrCommentImageToLink() {
        val url = "https://bitbucket.org/repo/y89ApqG/images/1-shot.png"
        val result = normalizeBitbucketMarkdown("![](" + url + ")")
        assertEquals("[🖼️ View image](" + url + ")", result)
    }

    @Test
    fun keepsFetchableImagesInline() {
        val input = "![badge](https://img.shields.io/badge/x.svg)"
        assertEquals(input, normalizeBitbucketMarkdown(input))
    }

    @Test
    fun stripsAttributesThenRewritesWebHostImage() {
        val url = "https://bitbucket.org/repo/y89ApqG/images/1-shot.png"
        val result = normalizeBitbucketMarkdown("![shot](" + url + "){: data-width='50' }")
        assertEquals("[🖼️ shot](" + url + ")", result)
    }
}
