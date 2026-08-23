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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.joelkanyi.jenga.component.progress.jengaShimmer
import io.github.joelkanyi.jenga.theme.JengaTheme

@Composable
fun PlatypusSkeletonLine(widthFraction: Float, modifier: Modifier = Modifier, height: Dp = 14.dp) {
    Box(
        modifier
            .height(height)
            .fillMaxWidth(widthFraction)
            .clip(JengaTheme.shapes.control)
            .jengaShimmer(),
    )
}

@Composable
fun PlatypusListRowSkeleton(modifier: Modifier = Modifier) {
    val spacing = JengaTheme.spacing
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Box(Modifier.size(36.dp).clip(JengaTheme.shapes.pill).jengaShimmer())
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            PlatypusSkeletonLine(widthFraction = 0.7f)
            PlatypusSkeletonLine(widthFraction = 0.4f, height = 12.dp)
        }
    }
}

@Composable
fun PlatypusListSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    count: Int = 8,
) {
    LazyColumn(modifier = modifier, contentPadding = contentPadding) {
        items(count) { PlatypusListRowSkeleton() }
    }
}
