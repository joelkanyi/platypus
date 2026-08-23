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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun PlatypusMarkdown(content: String, modifier: Modifier = Modifier) {
    val colors = JengaTheme.colors
    val typography = JengaTheme.typography
    val mono = rememberGeistMonoFontFamily()
    val context = LocalPlatformContext.current
    val transformer = remember(context) { CoilImageTransformer(SingletonImageLoader.get(context)) }
    val normalized = remember(content) { normalizeBitbucketMarkdown(content) }
    Markdown(
        content = normalized,
        modifier = modifier.fillMaxWidth(),
        imageTransformer = transformer,
        colors = markdownColor(
            text = colors.textSecondary,
            codeBackground = colors.surfaceVariant,
            inlineCodeBackground = colors.surfaceVariant,
            dividerColor = colors.surfaceVariant,
            tableBackground = colors.surfaceVariant,
        ),
        typography = markdownTypography(
            h1 = typography.headingMedium,
            h2 = typography.headingMedium,
            h3 = typography.titleMedium,
            h4 = typography.titleMedium,
            h5 = typography.bodyMedium,
            h6 = typography.bodyMedium,
            text = typography.bodyMedium,
            paragraph = typography.bodyMedium,
            ordered = typography.bodyMedium,
            bullet = typography.bodyMedium,
            list = typography.bodyMedium,
            quote = typography.bodyMedium,
            code = typography.bodySmall.copy(fontFamily = mono),
            inlineCode = typography.bodyMedium.copy(fontFamily = mono),
            table = typography.bodySmall,
            textLink = TextLinkStyles(
                style = SpanStyle(color = colors.info, textDecoration = TextDecoration.Underline),
            ),
        ),
    )
}

private val IMAGE_WITH_ATTRS = Regex("""(!\[[^\]]*\]\([^)]*\))\s*\{:[^}]*\}""")
private val ATTR_BLOCK = Regex("""\{:[^}]*\}""")
private val WEB_HOST_IMAGE = Regex("""!\[([^\]]*)\]\((https?://bitbucket\.org/[^)]*)\)""")

internal fun normalizeBitbucketMarkdown(raw: String): String {
    var text = IMAGE_WITH_ATTRS.replace(raw) { it.groupValues[1] }
    text = ATTR_BLOCK.replace(text, "")
    text = WEB_HOST_IMAGE.replace(text) { match ->
        val label = match.groupValues[1].ifBlank { "View image" }
        "[🖼️ $label](${match.groupValues[2]})"
    }
    return text
}

private class CoilImageTransformer(private val imageLoader: ImageLoader) : ImageTransformer {
    @Composable
    override fun transform(link: String): ImageData = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(link)
            .size(coil3.size.Size.ORIGINAL)
            .build(),
        imageLoader = imageLoader,
    ).let { ImageData(it) }

    @Composable
    override fun intrinsicSize(painter: Painter): Size {
        var size by remember(painter) { mutableStateOf(painter.intrinsicSize) }
        if (painter is AsyncImagePainter) {
            val painterState = painter.state.collectAsState()
            painterState.value.painter?.intrinsicSize?.also { size = it }
        }
        return size
    }
}
