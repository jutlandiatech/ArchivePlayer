package com.thomasnorlund.archiveplayer.support

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.thomasnorlund.archiveplayer.core.category.CategoryMap
import com.thomasnorlund.archiveplayer.core.category.CategoryMapParser
import com.thomasnorlund.archiveplayer.core.text.TextCatalog
import com.thomasnorlund.archiveplayer.ui.ElementIds
import org.junit.Assert.assertTrue

/** Loads a shipped TextCatalog so tests can compare visible text with catalog values (R69). */
fun catalog(fileCode: String): TextCatalog {
    val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
    return TextCatalog.parse(fileCode, assets.open("texts/TextCatalog_$fileCode.json").bufferedReader().readText())
}

/** The bundled category map shipped in the app (R15). */
fun bundledCategoryMap(): CategoryMap {
    val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
    return CategoryMapParser.parse(assets.open("catalog/category-map.json").bufferedReader().readText())
}

fun hasStateDescription(): SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.StateDescription)

fun ComposeTestRule.stateDescriptionOf(tag: String): String? =
    onNodeWithTag(tag).fetchSemanticsNode().config.getOrElseNullable(SemanticsProperties.StateDescription) { null }

fun ComposeTestRule.textOf(tag: String): String =
    onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().config
        .getOrElseNullable(SemanticsProperties.Text) { null }.orEmpty().joinToString(" ") { it.text }

fun ComposeTestRule.exists(tag: String): Boolean =
    onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()

/** Test tags currently composed that start with [prefix], ordered top-to-bottom then left-to-right. */
fun ComposeTestRule.tagsWithPrefix(prefix: String): List<String> =
    onAllNodes(SemanticsMatcher("tag starts with $prefix") { it.config.getOrElseNullable(SemanticsProperties.TestTag) { null }?.startsWith(prefix) == true }, useUnmergedTree = true)
        .fetchSemanticsNodes().sortedWith(compareBy({ it.positionInRoot.y }, { it.positionInRoot.x }))
        .map { it.config[SemanticsProperties.TestTag] }.distinct()

/** Asserts [tag] lies horizontally inside `app_root` (no clipping off-screen at large font scale). */
fun ComposeTestRule.assertFitsWidth(tag: String) {
    val root = onNodeWithTag(ElementIds.APP_ROOT).fetchSemanticsNode().boundsInRoot
    val node = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
    assertTrue("$tag $node inside $root", node.left >= root.left - 1 && node.right <= root.right + 1)
}

/** Runs [block] at 200 % font scale on a 320 dp wide screen, restoring the device afterwards (R62, R64). */
fun ComposeTestRule.withLargeFontNarrowScreen(block: () -> Unit) {
    try {
        shell("settings put system font_scale 2.0")
        shell("wm size 720x1560")
        shell("wm density 360")
        Thread.sleep(1500)
        waitForIdle()
        block()
    } finally {
        shell("settings put system font_scale 1.0")
        shell("wm size reset")
        shell("wm density reset")
    }
}

fun ComposeTestRule.waitForTag(tag: String, timeoutMs: Long = 10_000) = waitUntil(timeoutMs) { exists(tag) }

fun ComposeTestRule.waitForGone(tag: String, timeoutMs: Long = 10_000) = waitUntil(timeoutMs) { !exists(tag) }

/** Waits for [tag], scrolls it into view when inside a scroll container, and clicks it. */
fun ComposeTestRule.tap(tag: String) {
    waitForTag(tag)
    runCatching { onNodeWithTag(tag).performScrollTo() }
    onNodeWithTag(tag).performClick()
    waitForIdle()
}

/** Scrolls the lazy container [listTag] until [tag] is composed. */
fun ComposeTestRule.scrollIn(listTag: String, tag: String) {
    waitForTag(listTag)
    onNodeWithTag(listTag).performScrollToNode(hasTestTag(tag))
    waitForIdle()
}

fun ComposeTestRule.tapIn(listTag: String, tag: String) {
    scrollIn(listTag, tag)
    onNodeWithTag(tag).performClick()
    waitForIdle()
}

fun ComposeTestRule.topOf(tag: String): Float = onNodeWithTag(tag).fetchSemanticsNode().positionInRoot.y

/** Asserts that [tags] appear in this order (top to bottom) inside the lazy container [listTag]. */
fun ComposeTestRule.assertOrderedIn(listTag: String, tags: List<String>) {
    tags.forEachIndexed { i, tag ->
        scrollIn(listTag, tag)
        if (i > 0 && exists(tags[i - 1])) assertTrue("${tags[i - 1]} before $tag", topOf(tags[i - 1]) < topOf(tag))
    }
}

fun pressBack(rule: ComposeTestRule) {
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
    rule.waitForIdle()
}

fun shell(command: String): String =
    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).executeShellCommand(command)

/** Completes first-launch setup with the defaults (UI_SETUP_SPEC.md). */
fun ComposeTestRule.completeSetup() {
    waitForTag(ElementIds.SETUP_CONTINUE_BUTTON)
    onNodeWithTag(ElementIds.SETUP_CONTINUE_BUTTON).performClick()
    onNodeWithTag(ElementIds.SETUP_CONTINUE_BUTTON).performClick()
    onNodeWithTag(ElementIds.SETUP_START_BUTTON).performClick()
    waitForIdle()
}

/** Runs a search from the Search tab root (UI_SEARCH_SPEC.md). */
fun ComposeTestRule.search(query: String) {
    tap(ElementIds.NAV_SEARCH)
    // Tabs keep their back stacks (UI_SHELL_SPEC.md N04): a second tap pops Search to its root.
    waitForIdle()
    if (!exists(ElementIds.SEARCH_QUERY_FIELD)) tap(ElementIds.NAV_SEARCH)
    waitForTag(ElementIds.SEARCH_QUERY_FIELD)
    onNodeWithTag(ElementIds.SEARCH_QUERY_FIELD).performTextClearance()
    onNodeWithTag(ElementIds.SEARCH_QUERY_FIELD).performTextInput(query)
    onNodeWithTag(ElementIds.SEARCH_QUERY_FIELD).performImeAction()
    waitForIdle()
}

/** Opens a fixture item page through Search (FixtureServer returns every fixture item for other queries). */
fun ComposeTestRule.openItem(identifier: String) {
    search(identifier)
    tapIn(ElementIds.SEARCH_RESULTS, ElementIds.searchResult(identifier))
    waitForTag(ElementIds.ITEM_SCREEN)
}

/** Opens [identifier] and starts playback at entry [entry]; returns when the mini-player shows. */
fun ComposeTestRule.play(identifier: String, entry: Int = 0) {
    openItem(identifier)
    tapIn(ElementIds.ITEM_ENTRIES, ElementIds.itemEntryRow(entry))
    waitForTag(ElementIds.MINI_PLAYER, 15_000)
}

fun ComposeTestRule.openSettingsRow(name: String) {
    tap(ElementIds.TOP_BAR_SETTINGS_BUTTON)
    tap(ElementIds.settingsRow(name))
}

/** Entry menu → Add to playlist → New playlist [name] (UI_ITEM_SPEC.md). */
fun ComposeTestRule.addEntryToNewPlaylist(entry: Int, name: String) {
    tapIn(ElementIds.ITEM_ENTRIES, ElementIds.itemEntryMenu(entry))
    tap(ElementIds.entryMenu("add_to_playlist"))
    tap(ElementIds.PLAYLIST_PICKER_NEW)
    onNodeWithTag(ElementIds.DIALOG_PLAYLIST_NAME_FIELD).performTextInput(name)
    tap(ElementIds.DIALOG_CONFIRM_BUTTON)
}

fun ComposeTestRule.addEntryToPlaylist(entry: Int, playlistId: String) {
    tapIn(ElementIds.ITEM_ENTRIES, ElementIds.itemEntryMenu(entry))
    tap(ElementIds.entryMenu("add_to_playlist"))
    tap(ElementIds.playlistPicker(playlistId))
}

fun ComposeTestRule.openLibrary(filter: String = "playlists") {
    tap(ElementIds.NAV_LIBRARY)
    tap(ElementIds.libraryFilter(filter))
}
