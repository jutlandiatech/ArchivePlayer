package com.jutlandiatech.archiveplayer

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jutlandiatech.archiveplayer.support.FixtureServer
import com.jutlandiatech.archiveplayer.support.*
import com.jutlandiatech.archiveplayer.ui.ElementIds
import com.jutlandiatech.archiveplayer.ui.ElementIds.searchFilterKind
import com.jutlandiatech.archiveplayer.ui.ElementIds.searchResult
import com.jutlandiatech.archiveplayer.ui.ElementIds.searchSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI_SEARCH_SPEC.md scenarios Q01–Q14 by element ID against FixtureServer. */
@RunWith(AndroidJUnit4::class)
class SearchUiTest {
    @get:Rule(order = 0) val server = FixtureServer()
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    @Before fun setUp() = rule.completeSetup()

    private fun node(tag: String) = rule.onNodeWithTag(tag)
    /** Waits until result [id] is listed, scrolling the lazy results list to it. */
    private fun results(id: String) = rule.waitUntil(15_000) {
        rule.exists(ElementIds.SEARCH_RESULTS) && runCatching { rule.scrollIn(ElementIds.SEARCH_RESULTS, searchResult(id)) }.isSuccess
    }

    @Test fun Q01_browseGridShowsCategoryTiles() {
        rule.tap(ElementIds.NAV_SEARCH)
        rule.waitForTag(ElementIds.BROWSE_GRID)
        bundledCategoryMap().roots.forEach { root ->
            rule.scrollIn(ElementIds.BROWSE_GRID, ElementIds.browseTile(root.id))
            assertTrue(rule.textOf(ElementIds.browseTile(root.id)).contains(root.name("en")))
            root.children.forEach { rule.scrollIn(ElementIds.BROWSE_GRID, ElementIds.browseTile(it.id)) }
        }
        assertFalse(rule.exists(ElementIds.searchRecent(0)))
    }

    @Test fun Q02_suggestionsAreLocalOnly() {
        rule.search("gunsmoke")
        results("OTRR_Gunsmoke_Singles")
        node(ElementIds.SEARCH_QUERY_FIELD).performTextClearance()
        val before = server.searchRequests().size
        node(ElementIds.SEARCH_QUERY_FIELD).performTextInput("gun")
        rule.waitForTag(ElementIds.searchSuggestion(0))
        assertTrue(rule.textOf(ElementIds.searchSuggestion(0)).contains("gunsmoke", ignoreCase = true))
        Thread.sleep(1_000)
        assertEquals(before, server.searchRequests().size)
    }

    @Test fun Q03_A10_kindFilterAndPopularitySort() {
        rule.search("gunsmoke")
        rule.tap(searchFilterKind("audio"))
        rule.tap(searchSort("popularity"))
        rule.waitUntil(10_000) {
            server.searchRequestsFor("gunsmoke").any {
                val url = it.requestUrl!!
                url.queryParameter("q")!!.contains("mediatype:(audio OR etree)") &&
                    url.queryParameterValues("sort[]").contains("downloads desc")
            }
        }
        node(searchFilterKind("audio")).assertIsSelected()
        node(searchSort("popularity")).assertIsSelected()
        results("OTRR_Gunsmoke_Singles")
        rule.waitForTag(ElementIds.searchSection("items"))
        assertFalse(rule.exists(ElementIds.searchSection("collections")))
        assertFalse(rule.exists(ElementIds.searchSection("playlists")))
    }

    @Test fun Q04_A05_A07_languageFilterExcludesUntagged() {
        rule.search("fixture")
        results("fx_untagged")
        rule.tap(ElementIds.SEARCH_FILTER_LANGUAGE_MORE)
        rule.tap(ElementIds.searchFilterOption("fr"))
        results("fx_french")
        rule.waitForGone(searchResult("fx_untagged"))
        assertFalse(rule.exists(searchResult("fx_english")))
        rule.tap(ElementIds.searchFilterLanguage("any"))
        results("fx_untagged")
    }

    @Test fun Q05_filtersVisibleIndividuallyRemovable() {
        rule.search("fixture")
        rule.tap(searchFilterKind("audio"))
        rule.tap(ElementIds.SEARCH_FILTER_DECADE)
        rule.tap(ElementIds.searchFilterOption("1950"))
        rule.tap(ElementIds.SEARCH_FILTER_DURATION)
        rule.tap(ElementIds.searchFilterOption("short"))
        node(ElementIds.SEARCH_FILTER_DECADE).assertTextContains("1950", substring = true)
        rule.tap(ElementIds.searchFilterRemove(ElementIds.SEARCH_FILTER_DECADE))
        assertFalse(rule.textOf(ElementIds.SEARCH_FILTER_DECADE).contains("1950"))
        node(searchFilterKind("audio")).assertIsSelected()
        rule.tap(ElementIds.SEARCH_FILTER_CLEAR)
        node(searchFilterKind("all")).assertIsSelected()
        assertFalse(rule.exists(ElementIds.searchFilterRemove(ElementIds.SEARCH_FILTER_DURATION)))
    }

    @Test fun Q06_pagingAppendsWithoutDuplicates() {
        rule.search("paging")
        results("fx_page_001")
        rule.scrollIn(ElementIds.SEARCH_RESULTS, searchResult("fx_page_075"))
        assertTrue(server.searchRequestsFor("paging").any { it.requestUrl!!.queryParameter("page") == "2" })
        rule.scrollIn(ElementIds.SEARCH_RESULTS, searchResult("fx_page_001"))
        assertEquals(1, rule.onAllNodesWithTag(searchResult("fx_page_001")).fetchSemanticsNodes().size)
    }

    @Test fun Q07_A35_backOffThenErrorWithRetry() {
        server.failSearch("fixture", 503, times = 1)
        rule.search("fixture")
        results("fx_radio_show")
        server.failSearch("broken", 503)
        rule.search("broken")
        rule.waitForTag(ElementIds.SEARCH_ERROR, 60_000)
        assertTrue("bounded retries", server.searchRequestsFor("broken").size <= 5)
        server.clearRules()
        rule.tap(ElementIds.SEARCH_RETRY_BUTTON)
        results("fx_radio_show")
    }

    @Test fun Q08_emptyResultOffersClearFilters() {
        rule.search("nohits")
        rule.waitForTag(ElementIds.SEARCH_EMPTY, 15_000)
        assertTrue(rule.textOf(ElementIds.SEARCH_EMPTY).contains("nohits"))
        rule.tap(searchFilterKind("audio"))
        rule.waitForTag(ElementIds.SEARCH_FILTER_CLEAR)
    }

    @Test fun Q09_A27_adultItemsHidden() {
        rule.search("fixture")
        results("fx_radio_show")
        rule.scrollIn(ElementIds.SEARCH_RESULTS, searchResult("fx_untagged"))
        assertFalse(rule.exists(searchResult("fx_adult")))
    }

    @Test fun Q10_recentSearchesAddRemoveAndInSettings() {
        rule.search("gunsmoke")
        results("OTRR_Gunsmoke_Singles")
        rule.tap(ElementIds.SEARCH_CLEAR_BUTTON)
        rule.waitForTag(ElementIds.searchRecent(0))
        assertTrue(rule.textOf(ElementIds.searchRecent(0)).contains("gunsmoke"))
        rule.openSettingsRow("search_history")
        rule.waitForTag("settings_search_history_remove_0")
        pressBack(rule); pressBack(rule)
        rule.tap(ElementIds.searchRecentRemove(0))
        rule.waitForGone(ElementIds.searchRecent(0))
    }

    @Test fun Q11_browseTileOpensCategoryWithSubcategories() {
        val root = bundledCategoryMap().roots.first { it.children.isNotEmpty() }
        rule.tap(ElementIds.NAV_SEARCH)
        rule.tapIn(ElementIds.BROWSE_GRID, ElementIds.browseTile(root.id))
        rule.waitForTag(ElementIds.CATEGORY_SCREEN)
        assertTrue(rule.textOf(ElementIds.CATEGORY_TITLE).contains(root.name("en")))
        rule.waitForTag(ElementIds.categorySubcategory(root.children.first().id))
        pressBack(rule)
        rule.waitForTag(ElementIds.BROWSE_GRID)
    }

    @Test fun Q12_stateSurvivesItemAndRotation() {
        rule.search("fixture")
        rule.tap(searchFilterKind("audio"))
        rule.tap(searchSort("newest"))
        rule.tapIn(ElementIds.SEARCH_RESULTS, searchResult("fx_radio_show"))
        rule.waitForTag(ElementIds.ITEM_SCREEN)
        pressBack(rule)
        node(searchFilterKind("audio")).assertIsSelected()
        node(searchSort("newest")).assertIsSelected()
        rule.activityRule.scenario.recreate()
        rule.waitForTag(searchFilterKind("audio"))
        node(searchFilterKind("audio")).assertIsSelected()
        node(searchSort("newest")).assertIsSelected()
        node(ElementIds.SEARCH_QUERY_FIELD).assertTextContains("fixture", substring = true)
    }

    @Test fun Q13_ownPlaylistsInResults() {
        rule.openLibrary()
        rule.tap(ElementIds.LIBRARY_NEW_PLAYLIST_BUTTON)
        node(ElementIds.DIALOG_PLAYLIST_NAME_FIELD).performTextInput("Gunsmoke favourites")
        rule.tap(ElementIds.DIALOG_CONFIRM_BUTTON)
        rule.search("gunsmoke")
        rule.waitForTag(ElementIds.searchSection("playlists"), 15_000)
        assertEquals(1, rule.tagsWithPrefix("search_result_playlist_").size)
    }

    @Test fun Q14_largeFontNarrowScreenChipsFit() = rule.withLargeFontNarrowScreen {
        listOf("en", "da", "de").forEach { iso ->
            rule.openSettingsRow("app_language")
            rule.tap(ElementIds.settingsLanguage(iso))
            pressBack(rule); pressBack(rule)
            rule.search("fixture")
            rule.waitForTag(ElementIds.SEARCH_RESULTS, 15_000)
            rule.assertFitsWidth(ElementIds.SEARCH_QUERY_FIELD)
            rule.assertFitsWidth(searchFilterKind("all"))
        }
    }
}
