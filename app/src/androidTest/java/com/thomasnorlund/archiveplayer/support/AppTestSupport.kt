package com.thomasnorlund.archiveplayer.support

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.thomasnorlund.archiveplayer.core.text.TextCatalog
import com.thomasnorlund.archiveplayer.ui.ElementIds

/** Loads a shipped TextCatalog so tests can compare visible text with catalog values (R69). */
fun catalog(fileCode: String): TextCatalog {
    val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
    return TextCatalog.parse(fileCode, assets.open("texts/TextCatalog_$fileCode.json").bufferedReader().readText())
}

fun hasStateDescription(): SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)

fun AndroidComposeTestRule<*, *>.stateDescriptionOf(tag: String): String? =
    onNodeWithTag(tag).fetchSemanticsNode().config.getOrElseNullable(SemanticsProperties.StateDescription) { null }

/** Completes first-launch setup with the defaults (UI_SETUP_SPEC.md). */
fun AndroidComposeTestRule<*, *>.completeSetup() {
    onNodeWithTag(ElementIds.SETUP_CONTINUE_BUTTON).performClick()
    onNodeWithTag(ElementIds.SETUP_CONTINUE_BUTTON).performClick()
    onNodeWithTag(ElementIds.SETUP_START_BUTTON).performClick()
    waitForIdle()
}
