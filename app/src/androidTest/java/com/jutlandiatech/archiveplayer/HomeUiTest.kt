package com.jutlandiatech.archiveplayer

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jutlandiatech.archiveplayer.core.catalog.MediaKind
import com.jutlandiatech.archiveplayer.support.FixtureServer
import com.jutlandiatech.archiveplayer.support.PlayerProbe
import com.jutlandiatech.archiveplayer.support.*
import com.jutlandiatech.archiveplayer.ui.ElementIds
import com.jutlandiatech.archiveplayer.ui.ElementIds.homeCard
import com.jutlandiatech.archiveplayer.ui.ElementIds.homeCategoryRow
import com.jutlandiatech.archiveplayer.ui.ElementIds.homeRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/** UI_HOME_SPEC.md scenarios H01–H12 by element ID against FixtureServer. */
@RunWith(AndroidJUnit4::class)
class HomeUiTest {
    @get:Rule(order = 0) val server = FixtureServer()
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    @Before fun setUp() = rule.completeSetup()

    private val map by lazy { bundledCategoryMap() }
    private val audioRoot by lazy { map.roots.first { it.kind != MediaKind.VIDEO } }
    private val audioRow by lazy { homeCategoryRow(audioRoot.id) }
    private val audioTerm by lazy { audioRoot.query!!.let { it.collections.firstOrNull() ?: it.subjects.first() } }
    private val personalRows = listOf(
        ElementIds.HOME_ROW_CONTINUE, ElementIds.HOME_ROW_RECENT, ElementIds.HOME_ROW_PLAYLISTS,
        ElementIds.HOME_ROW_DOWNLOADS, ElementIds.HOME_ROW_NEW_FOLLOWED, ElementIds.HOME_ROW_BECAUSE,
    )

    private fun playFor60sThenPause(identifier: String = "fx_radio_show") {
        rule.play(identifier)
        PlayerProbe.connect().use { p ->
            assertTrue(p.await { isPlaying })
            p.seekTo(60_000)
            p.await { positionMs >= 61_000 }
            p.pause()
        }
        rule.tap(ElementIds.NAV_HOME)
    }

    @Test fun H01_freshInstallShowsCuratedRowsInMapOrder() {
        rule.waitForTag(ElementIds.HOME_LIST)
        rule.assertOrderedIn(ElementIds.HOME_LIST, map.roots.map { homeRow(homeCategoryRow(it.id)) })
        personalRows.forEach { assertFalse(it, rule.exists(homeRow(it))) }
    }

    @Test fun H02_A17_continueRowFirstAfterPlayback() {
        playFor60sThenPause()
        rule.assertOrderedIn(ElementIds.HOME_LIST, listOf(homeRow(ElementIds.HOME_ROW_CONTINUE), homeRow(ElementIds.HOME_ROW_RECENT), homeRow(audioRow)))
        rule.scrollIn(ElementIds.HOME_LIST, homeRow(ElementIds.HOME_ROW_CONTINUE))
        rule.waitForTag(homeCard(ElementIds.HOME_ROW_CONTINUE, "fx_radio_show"))
        rule.waitForTag(ElementIds.homeCardProgress("fx_radio_show"))
    }

    @Test fun H03_continueCardResumesAtStoredPosition() {
        playFor60sThenPause()
        rule.activityRule.scenario.recreate()
        rule.tapIn(ElementIds.HOME_LIST, homeCard(ElementIds.HOME_ROW_CONTINUE, "fx_radio_show"))
        PlayerProbe.connect().use { p ->
            assertTrue(p.await { isPlaying })
            assertTrue("position ${p.positionMs}", abs(p.positionMs - 61_000) < 3_000)
        }
    }

    @Test fun H04_finishedEntryLeavesContinueStaysInRecent() {
        rule.play("fx_radio_show")
        PlayerProbe.connect().use { p ->
            assertTrue(p.await { isPlaying })
            p.seekToEndMinus(10_000)
            assertTrue(p.await(20_000) { currentIndex == 1 })
            p.pause()
        }
        rule.tap(ElementIds.NAV_HOME)
        rule.scrollIn(ElementIds.HOME_LIST, homeRow(ElementIds.HOME_ROW_RECENT))
        rule.waitForTag(homeCard(ElementIds.HOME_ROW_RECENT, "fx_radio_show"))
        assertTrue(rule.tagsWithPrefix("home_card_progress_").isEmpty())
    }

    @Test fun H05_playlistsRowLikedFirst() {
        rule.openItem("fx_radio_show")
        rule.tap(ElementIds.ITEM_LIKE_BUTTON)
        rule.addEntryToNewPlaylist(0, "Noir")
        rule.tap(ElementIds.NAV_HOME)
        rule.scrollIn(ElementIds.HOME_LIST, homeRow(ElementIds.HOME_ROW_PLAYLISTS))
        val playlists = rule.tagsWithPrefix("home_playlist_")
        assertEquals(2, playlists.size)
        assertEquals(ElementIds.homePlaylist(ElementIds.LIKED_PLAYLIST_ID), playlists.first())
    }

    @Test fun H06_followedCollectionRefreshThrottled() {
        rule.openItem("fx_radio_show")
        rule.tap(ElementIds.ITEM_FOLLOW_BUTTON)
        rule.tap(ElementIds.NAV_HOME)
        rule.waitUntil(10_000) { server.searchRequestsFor("fx_collection").isNotEmpty() }
        val afterFirst = server.searchRequestsFor("fx_collection").size
        rule.activityRule.scenario.recreate()
        rule.waitForTag(ElementIds.HOME_LIST)
        Thread.sleep(2_000)
        assertEquals(afterFirst, server.searchRequestsFor("fx_collection").size)
        rule.onNodeWithTag(ElementIds.HOME_LIST).performTouchInput { swipeDown() }
        rule.waitUntil(10_000) { server.searchRequestsFor("fx_collection").size > afterFirst }
    }

    @Test fun H07_A05_contentLanguageRanksRowWithoutHidingUntagged() {
        rule.openSettingsRow("content_languages")
        rule.tap(ElementIds.settingsContentLanguage("fr"))
        rule.tap(ElementIds.settingsContentLanguage("en"))
        pressBack(rule); pressBack(rule)
        rule.tap(ElementIds.NAV_HOME)
        val row = homeRow(audioRow)
        rule.scrollIn(ElementIds.HOME_LIST, row)
        rule.scrollIn(row, homeCard(audioRow, "fx_french"))
        val cards = rule.tagsWithPrefix("home_card_${audioRow}_")
        assertEquals(homeCard(audioRow, "fx_french"), cards.first())
        rule.scrollIn(row, homeCard(audioRow, "fx_untagged"))
    }

    @Test fun H08_failingRowShowsRetryOthersRender() {
        server.failSearch(audioTerm)
        // Home loads its rows as soon as setup finishes, before a test can install server rules: refresh after.
        rule.onNodeWithTag(ElementIds.HOME_LIST).performTouchInput { swipeDown() }
        rule.waitForTag(ElementIds.homeRowError(audioRow), 60_000)
        map.roots.filter { it.id != audioRoot.id }.take(1).forEach { rule.scrollIn(ElementIds.HOME_LIST, homeRow(homeCategoryRow(it.id))) }
        server.clearRules()
        rule.tapIn(ElementIds.HOME_LIST, ElementIds.homeRowRetry(audioRow))
        rule.waitForGone(ElementIds.homeRowError(audioRow), 30_000)
        assertTrue(rule.tagsWithPrefix("home_card_${audioRow}_").isNotEmpty())
    }

    @Test fun H09_A39_slowRowShowsStillLoadingRestUsable() {
        server.delaySearch(audioTerm, 25_000)
        // Home loads its rows as soon as setup finishes, before a test can install server rules: refresh after.
        rule.onNodeWithTag(ElementIds.HOME_LIST).performTouchInput { swipeDown() }
        rule.waitForTag(ElementIds.homeRowLoading(audioRow))
        rule.waitForTag(ElementIds.homeRowRetry(audioRow), 20_000)
        rule.tap(ElementIds.NAV_SEARCH)
        rule.waitForTag(ElementIds.SEARCH_SCREEN)
    }

    @Test fun H10_cardOpensItemBackRestoresScroll() {
        val row = homeRow(audioRow)
        rule.scrollIn(ElementIds.HOME_LIST, row)
        val top = rule.topOf(row)
        rule.tapIn(row, homeCard(audioRow, "fx_radio_show"))
        rule.waitForTag(ElementIds.ITEM_SCREEN)
        pressBack(rule)
        rule.waitForTag(ElementIds.HOME_SCREEN)
        assertTrue(abs(rule.topOf(row) - top) < 2f)
    }

    @Test fun H11_S08_pausedHistoryAddsNoPersonalRows() {
        rule.openSettingsRow("history")
        rule.tap(ElementIds.settingsSwitch("history_pause"))
        pressBack(rule); pressBack(rule)
        playFor60sThenPause()
        rule.waitForTag(ElementIds.HOME_LIST)
        assertFalse(rule.exists(homeRow(ElementIds.HOME_ROW_CONTINUE)))
        assertFalse(rule.exists(homeRow(ElementIds.HOME_ROW_RECENT)))
    }

    @Test fun H12_largeFontNarrowScreenAllLanguages() = rule.withLargeFontNarrowScreen {
        listOf("en", "da", "de").forEach { iso ->
            rule.openSettingsRow("app_language")
            rule.tap(ElementIds.settingsLanguage(iso))
            pressBack(rule); pressBack(rule)
            rule.tap(ElementIds.NAV_HOME)
            val first = homeRow(homeCategoryRow(map.roots.first().id))
            rule.scrollIn(ElementIds.HOME_LIST, first)
            rule.assertFitsWidth(ElementIds.homeRowTitle(homeCategoryRow(map.roots.first().id)))
            rule.assertFitsWidth(ElementIds.NAV_BAR)
        }
    }
}
