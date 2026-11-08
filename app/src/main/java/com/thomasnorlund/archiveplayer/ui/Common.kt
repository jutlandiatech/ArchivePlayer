package com.thomasnorlund.archiveplayer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Semantic root: exposes test tags as resource IDs for UI Automator (R69). */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.idRoot(tag: String): Modifier = semantics { testTagsAsResourceId = true }.testTag(tag)

/** A text-glyph icon button with a localized accessibility label (no icon library needed). */
@Composable
fun GlyphButton(glyph: String, label: String, tag: String, modifier: Modifier = Modifier, extra: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).testTag(tag).semantics { contentDescription = label }.then(extra),
    ) {
        Text(glyph, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun Glyph(glyph: String) = Box { Text(glyph, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary) }
