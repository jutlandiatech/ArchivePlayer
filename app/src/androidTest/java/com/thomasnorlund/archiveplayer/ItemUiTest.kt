package com.thomasnorlund.archiveplayer

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thomasnorlund.archiveplayer.support.FixtureServer
import com.thomasnorlund.archiveplayer.support.PlayerProbe
import com.thomasnorlund.archiveplayer.support.*
import com.thomasnorlund.archiveplayer.ui.ElementIds
import com.thomasnorlund.archiveplayer.ui.ElementIds.itemEntryRow
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI_ITEM_SPEC.md scenarios I01–I14, I16 by element ID against FixtureServer (I15 is in LibraryUiTest). */
@RunWith(AndroidJUnit4::class)
class ItemUiTest {
    @get:Rule(order = 0) val server = FixtureServer()
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    @Before fun setUp() = rule.completeSetup()

    private fun episodes(n: Int) = (1..n).map { "Episode %02d".format(it) }

    @Test fun I01_A11_headerAndGroupedOrderedEntries() {
        rule.openItem("fx_radio_show")
        assertEquals("Fixture Radio Show", rule.textOf(ElementIds.ITEM_TITLE))
        assertTrue(rule.textOf(ElementIds.ITEM_CREATOR).contains("Fixture Radio"))
        assertTrue(rule.textOf(ElementIds.ITEM_YEAR).contains("1950"))
        assertTrue(rule.textOf(ElementIds.ITEM_LANGUAGES).contains("en"))
        rule.waitForTag(ElementIds.ITEM_KIND)
        episodes(6).forEachIndexed { i, title ->
            rule.scrollIn(ElementIds.ITEM_ENTRIES, itemEntryRow(i))
            assertTrue(rule.textOf(itemEntryRow(i)).contains(title))
        }
        assertFalse(rule.exists(itemEntryRow(6)))
    }

    @Test fun I02_playAllQueuesItemInOrder() {
        rule.openItem("fx_radio_show")
        rule.tap(ElementIds.ITEM_PLAY_ALL_BUTTON)
        rule.waitForTag(ElementIds.MINI_PLAYER, 15_000)
        PlayerProbe.connect().use { p ->
            assertTrue(p.await { isPlaying })
            assertEquals(episodes(6), p.queueTitles)
            assertEquals(0, p.currentIndex)
        }
        assertTrue(rule.textOf(ElementIds.MINI_PLAYER_TITLE).contains("Episode 01"))
    }

    @Test fun I03_entryTapStartsFromThatEntry() {
        rule.play("fx_radio_show", entry = 3)
        PlayerProbe.connect().use { p ->
            assertTrue(p.await { isPlaying })
            assertEquals(6, p.queueSize)
            assertEquals(3, p.currentIndex)
            assertEquals("Episode 04", p.currentTitle)
        }
    }

    @Test fun I04_shuffleQueuesAllInOtherOrder() {
        rule.openItem("fx_series_12")
        rule.tap(ElementIds.ITEM_SHUFFLE_BUTTON)
        PlayerProbe.connect().use { p ->
            assertTrue(p.await { isPlaying && queueSize == 12 })
            assertEquals(episodes(12).toSet(), p.queueTitles.toSet())
            assertNotEquals(episodes(12), p.queueTitles)
        }
    }

    @Test fun I05_likeItemListsEntriesInLiked() {
        rule.openItem("fx_radio_show")
        rule.tap(ElementIds.ITEM_LIKE_BUTTON)
        rule.onNodeWithTag(ElementIds.ITEM_LIKE_BUTTON).assertIsOn()
        rule.openLibrary("liked")
        rule.waitForTag(ElementIds.playlistEntryRow(5))
        assertFalse(rule.exists(ElementIds.playlistEntryRow(6)))
        rule.openItem("fx_radio_show")
        rule.onNodeWithTag(ElementIds.ITEM_LIKE_BUTTON).assertIsOn()
    }

    @Test fun I06_A21_followCollectionListedInLibrary() {
        rule.openItem("fx_radio_show")
        rule.tap(ElementIds.ITEM_FOLLOW_BUTTON)
        rule.onNodeWithTag(ElementIds.ITEM_FOLLOW_BUTTON).assertIsOn()
        rule.openLibrary("following")
        rule.waitForTag(ElementIds.libraryFollowing("fx_collection"))
    }

    @Test fun I07_A13_playNextAndAddToQueue() {
        rule.play("fx_radio_show", entry = 0)
        rule.tapIn(ElementIds.ITEM_ENTRIES, ElementIds.itemEntryMenu(4))
        rule.tap(ElementIds.entryMenu("play_next"))
        rule.tapIn(ElementIds.ITEM_ENTRIES, ElementIds.itemEntryMenu(2))
        rule.tap(ElementIds.entryMenu("add_to_queue"))
        PlayerProbe.connect().use { p ->
            assertTrue(p.await { queueSize == 8 })
            assertEquals("Episode 05", p.queueTitles[1])
            assertEquals("Episode 03", p.queueTitles.last())
        }
    }

    @Test fun I08_addToNewPlaylistThenPickerListsIt() {
        rule.openItem("fx_radio_show")
        rule.addEntryToNewPlaylist(0, "Noir")
        rule.tapIn(ElementIds.ITEM_ENTRIES, ElementIds.itemEntryMenu(1))
        rule.tap(ElementIds.entryMenu("add_to_playlist"))
        rule.waitForTag(ElementIds.SHEET_PLAYLIST_PICKER)
        val pickers = rule.tagsWithPrefix("playlist_picker_") - ElementIds.PLAYLIST_PICKER_NEW
        assertTrue(pickers.any { rule.textOf(it).contains("Noir") })
    }

    @Test fun I09_R32_entryFilterOnlyAbove30Entries() {
        rule.openItem("fx_series_40")
        rule.waitForTag(ElementIds.ITEM_ENTRY_FILTER)
        rule.onNodeWithTag(ElementIds.ITEM_ENTRY_FILTER).performTextInput("07")
        rule.waitForGone(itemEntryRow(0))
        rule.waitForTag(itemEntryRow(6))
        rule.openItem("fx_series_12")
        rule.waitForTag(itemEntryRow(0))
        assertFalse(rule.exists(ElementIds.ITEM_ENTRY_FILTER))
    }

    @Test fun I10_R59_videoHasNoDownload() {
        rule.openItem("fx_film")
        rule.waitForTag(ElementIds.ITEM_PLAY_ALL_BUTTON)
        assertFalse(rule.exists(ElementIds.ITEM_DOWNLOAD_BUTTON))
        rule.tapIn(ElementIds.ITEM_ENTRIES, ElementIds.itemEntryMenu(0))
        rule.waitForTag(ElementIds.SHEET_ENTRY)
        assertFalse(rule.exists(ElementIds.entryMenu("download")))
    }

    @Test fun I11_A26_downloadedAudioPlaysOffline() {
        rule.openItem("fx_series_12")
        rule.tap(ElementIds.ITEM_DOWNLOAD_BUTTON)
        rule.waitUntil(90_000) { rule.textOf(ElementIds.ITEM_DOWNLOAD_BUTTON).contains("Downloaded") }
        AppEnvironment.networkOverride = NetworkKind.OFFLINE
        server.fail("^/download/")
        rule.tapIn(ElementIds.ITEM_ENTRIES, itemEntryRow(0))
        PlayerProbe.connect().use { p -> assertTrue(p.await(15_000) { isPlaying }) }
    }

    @Test fun I12_A12_nothingPlayableState() {
        rule.openItem("fx_nothing")
        rule.waitForTag(ElementIds.ITEM_NOTHING_PLAYABLE)
        rule.onNodeWithTag(ElementIds.ITEM_PLAY_ALL_BUTTON).assertIsNotEnabled()
        rule.onNodeWithTag(ElementIds.ITEM_SHUFFLE_BUTTON).assertIsNotEnabled()
        rule.waitForTag(ElementIds.ITEM_OPEN_ARCHIVE_BUTTON)
    }

    @Test fun I13_errorThenRetry() {
        server.fail("^/metadata/fx_radio_show", 500)
        rule.openItem("fx_radio_show")
        rule.waitForTag(ElementIds.ITEM_ERROR, 60_000)
        server.clearRules()
        rule.tap(ElementIds.ITEM_RETRY_BUTTON)
        rule.waitForTag(ElementIds.ITEM_TITLE, 15_000)
    }

    @Test fun I14_R33_collectionPageWithFollowAndFilters() {
        rule.openItem("fx_radio_show")
        rule.tap(ElementIds.ITEM_COLLECTION_LINK)
        rule.waitForTag(ElementIds.COLLECTION_SCREEN)
        assertTrue(rule.textOf(ElementIds.COLLECTION_TITLE).contains("Fixture Collection"))
        rule.waitForTag(ElementIds.COLLECTION_FOLLOW_BUTTON)
        rule.scrollIn(ElementIds.COLLECTION_ITEMS, ElementIds.collectionItem("fx_radio_show"))
        rule.tap(ElementIds.searchFilterKind("video"))
        rule.waitForGone(ElementIds.collectionItem("fx_radio_show"))
        rule.waitForTag(ElementIds.collectionItem("fx_film"))
    }

    @Test fun I16_shareSendsArchiveUrl() {
        Intents.init()
        try {
            Intents.intending(not(isInternal())).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
            rule.openItem("fx_radio_show")
            rule.tap(ElementIds.ITEM_SHARE_BUTTON)
            rule.waitUntil(5_000) { Intents.getIntents().any { it.action == Intent.ACTION_CHOOSER } }
            val chooser = Intents.getIntents().last { it.action == Intent.ACTION_CHOOSER }
            val inner = chooser.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
            assertTrue(inner.getStringExtra(Intent.EXTRA_TEXT)!!.contains("https://archive.org/details/fx_radio_show"))
        } finally {
            Intents.release()
        }
    }
}
