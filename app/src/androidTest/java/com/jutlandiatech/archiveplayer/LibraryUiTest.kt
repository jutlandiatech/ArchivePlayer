package com.jutlandiatech.archiveplayer

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.net.Uri
import android.os.StrictMode
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jutlandiatech.archiveplayer.core.playlist.EntryRef
import com.jutlandiatech.archiveplayer.core.playlist.PlaylistFileCodec
import com.jutlandiatech.archiveplayer.core.playlist.ShareLinkCodec
import com.jutlandiatech.archiveplayer.core.playlist.SharedPlaylist
import com.jutlandiatech.archiveplayer.support.FixtureServer
import com.jutlandiatech.archiveplayer.support.PlayerProbe
import com.jutlandiatech.archiveplayer.support.*
import com.jutlandiatech.archiveplayer.ui.ElementIds
import com.jutlandiatech.archiveplayer.ui.ElementIds.libraryFilter
import com.jutlandiatech.archiveplayer.ui.ElementIds.playlistEntryRow
import org.hamcrest.Matchers.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** UI_LIBRARY_SPEC.md scenarios L01–L16 and UI_ITEM_SPEC.md I15 by element ID (L17 is in the flavour test sets). */
@RunWith(AndroidJUnit4::class)
class LibraryUiTest {
    @get:Rule(order = 0) val server = FixtureServer(freeBytes = 10_000_000_000)
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    @Before fun setUp() = rule.completeSetup()

    private val liked = ElementIds.libraryPlaylist(ElementIds.LIKED_PLAYLIST_ID)
    private fun userPlaylists() = rule.tagsWithPrefix("library_playlist_") - liked

    private fun newPlaylist(name: String): String {
        rule.openLibrary()
        val before = userPlaylists().toSet()
        rule.tap(ElementIds.LIBRARY_NEW_PLAYLIST_BUTTON)
        rule.onNodeWithTag(ElementIds.DIALOG_PLAYLIST_NAME_FIELD).performTextInput(name)
        rule.tap(ElementIds.DIALOG_CONFIRM_BUTTON)
        rule.waitForTag(ElementIds.PLAYLIST_SCREEN)
        pressBack(rule)
        return (userPlaylists() - before).single().removePrefix("library_playlist_")
    }

    private fun playlistWith(name: String, identifier: String, entries: List<Int>): String {
        val id = newPlaylist(name)
        rule.openItem(identifier)
        entries.forEach { rule.addEntryToPlaylist(it, id) }
        return id
    }

    private fun openPlaylist(id: String) {
        rule.openLibrary()
        rule.tapIn(ElementIds.LIBRARY_LIST, ElementIds.libraryPlaylist(id))
        rule.waitForTag(ElementIds.PLAYLIST_SCREEN)
    }

    private fun entryTitles(count: Int) = (0 until count).map { rule.scrollIn(ElementIds.PLAYLIST_SCREEN, playlistEntryRow(it)); rule.textOf(playlistEntryRow(it)) }

    private fun launchView(uri: Uri, type: String? = null) {
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().build())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, type).setClass(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION))
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        // The running activity is paused while it receives the new intent; its window regains focus asynchronously.
        val deadline = android.os.SystemClock.uptimeMillis() + 15_000
        while (true) {
            try { rule.waitForTag(ElementIds.IMPORT_SCREEN, 15_000); break } catch (e: IllegalStateException) {
                if (android.os.SystemClock.uptimeMillis() > deadline) throw e
                Thread.sleep(200)
            }
        }
    }

    private fun shareLink(title: String, refs: List<EntryRef>) =
        ShareLinkCodec.encode(SharedPlaylist(title, null, refs))

    @Test fun L01_chipsOrderAndLikedPinned() {
        rule.openLibrary()
        val chips = ElementIds.LIBRARY_FILTERS.map(::libraryFilter)
        chips.forEach { rule.waitForTag(it) }
        chips.zipWithNext().forEach { (a, b) -> assertTrue(rule.onNodeWithTag(a).fetchSemanticsNode().positionInRoot.x < rule.onNodeWithTag(b).fetchSemanticsNode().positionInRoot.x) }
        rule.onNodeWithTag(libraryFilter("playlists")).assertIsSelected()
        assertTrue(rule.topOf(ElementIds.LIBRARY_NEW_PLAYLIST_BUTTON) < rule.topOf(liked))
    }

    @Test fun L02_newPlaylistAppearsEmpty() {
        val id = newPlaylist("Noir")
        assertTrue(rule.textOf(ElementIds.libraryPlaylist(id)).contains("Noir"))
        assertTrue(rule.textOf(ElementIds.libraryPlaylist(id)).contains("0"))
    }

    @Test fun L03_reorderAndRemovePersist() {
        val id = playlistWith("Order", "fx_radio_show", listOf(0, 1, 2))
        openPlaylist(id)
        rule.tap(ElementIds.PLAYLIST_EDIT_BUTTON)
        // Drag the handle up by two row heights so entry 3 lands first.
        val rowHeight = rule.onNodeWithTag(playlistEntryRow(0)).fetchSemanticsNode().size.height.toFloat()
        rule.onNodeWithTag(ElementIds.playlistEntryDrag(2)).performTouchInput {
            swipe(center, center - androidx.compose.ui.geometry.Offset(0f, 2 * rowHeight), durationMillis = 800)
        }
        rule.waitUntil { rule.textOf(playlistEntryRow(0)).contains("Episode 03") }
        rule.tap(ElementIds.playlistEntryRemove(1))
        rule.waitForGone(playlistEntryRow(2))
        val expected = entryTitles(2)
        rule.activityRule.scenario.recreate()
        openPlaylist(id)
        assertEquals(expected, entryTitles(2))
    }

    @Test fun L04_renameAndDescribe() {
        val id = newPlaylist("Old name")
        openPlaylist(id)
        rule.tap(ElementIds.PLAYLIST_OVERFLOW_BUTTON)
        rule.tap(ElementIds.playlistMenu("rename"))
        rule.onNodeWithTag(ElementIds.DIALOG_PLAYLIST_NAME_FIELD).performTextClearance()
        rule.onNodeWithTag(ElementIds.DIALOG_PLAYLIST_NAME_FIELD).performTextInput("New name")
        rule.tap(ElementIds.DIALOG_CONFIRM_BUTTON)
        rule.tap(ElementIds.PLAYLIST_OVERFLOW_BUTTON)
        rule.tap(ElementIds.playlistMenu("description"))
        rule.onNodeWithTag(ElementIds.DIALOG_PLAYLIST_NAME_FIELD).performTextInput("Late night noir")
        rule.tap(ElementIds.DIALOG_CONFIRM_BUTTON)
        assertEquals("New name", rule.textOf(ElementIds.PLAYLIST_TITLE))
        assertTrue(rule.textOf(ElementIds.PLAYLIST_DESCRIPTION).contains("Late night noir"))
        pressBack(rule)
        assertTrue(rule.textOf(ElementIds.libraryPlaylist(id)).contains("New name"))
    }

    @Test fun L05_deleteWithUndoLikedNotDeletable() {
        val id = playlistWith("Temp", "fx_radio_show", listOf(0))
        openPlaylist(id)
        rule.tap(ElementIds.PLAYLIST_OVERFLOW_BUTTON)
        rule.tap(ElementIds.playlistMenu("delete"))
        rule.waitForGone(ElementIds.libraryPlaylist(id))
        rule.tap(ElementIds.PLAYLIST_UNDO_DELETE)
        rule.waitForTag(ElementIds.libraryPlaylist(id))
        rule.tapIn(ElementIds.LIBRARY_LIST, liked)
        rule.tap(ElementIds.PLAYLIST_OVERFLOW_BUTTON)
        assertFalse(rule.exists(ElementIds.playlistMenu("delete")))
        assertFalse(rule.exists(ElementIds.playlistMenu("rename")))
    }

    @Test fun L06_R35_playAndShuffleReplaceQueue() {
        val id = playlistWith("Queue", "fx_radio_show", listOf(4, 1, 2))
        openPlaylist(id)
        rule.tap(ElementIds.PLAYLIST_PLAY_BUTTON)
        PlayerProbe.connect().use { p ->
            assertTrue(p.await { isPlaying })
            assertEquals(listOf("Episode 05", "Episode 02", "Episode 03"), p.queueTitles)
            rule.tap(ElementIds.PLAYLIST_SHUFFLE_BUTTON)
            assertTrue(p.await { queueSize == 3 && queueTitles.toSet() == setOf("Episode 05", "Episode 02", "Episode 03") })
        }
    }

    @Test fun L07_R52_likedListFollowsHearts() {
        rule.openItem("fx_radio_show")
        rule.tapIn(ElementIds.ITEM_ENTRIES, ElementIds.itemEntryMenu(2))
        rule.tap(ElementIds.entryMenu("like"))
        rule.openLibrary("liked")
        rule.waitForTag(playlistEntryRow(0))
        assertTrue(rule.textOf(playlistEntryRow(0)).contains("Episode 03"))
        rule.openItem("fx_radio_show")
        rule.tapIn(ElementIds.ITEM_ENTRIES, ElementIds.itemEntryMenu(2))
        rule.tap(ElementIds.entryMenu("like"))
        rule.openLibrary("liked")
        rule.waitForTag(ElementIds.LIBRARY_EMPTY)
    }

    @Test fun L08_R53_unfollow() {
        rule.openItem("fx_radio_show")
        rule.tap(ElementIds.ITEM_FOLLOW_BUTTON)
        rule.openLibrary("following")
        rule.tap(ElementIds.libraryUnfollow("fx_collection"))
        rule.waitForGone(ElementIds.libraryFollowing("fx_collection"))
    }

    private fun captureShare(action: () -> Unit): Intent {
        Intents.init()
        try {
            Intents.intending(not(isInternal())).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
            action()
            rule.waitUntil(5_000) { Intents.getIntents().any { it.action == Intent.ACTION_CHOOSER } }
            return Intents.getIntents().last { it.action == Intent.ACTION_CHOOSER }.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)!!
        } finally {
            Intents.release()
        }
    }

    @Test fun L09_A21_shareLinkEncodesEntries() {
        val id = playlistWith("Twenty", "fx_series_40", (0 until 20).toList())
        openPlaylist(id)
        val text = captureShare { rule.tap(ElementIds.PLAYLIST_SHARE_BUTTON) }.getStringExtra(Intent.EXTRA_TEXT)!!
        val url = Regex("https://\\S+").find(text)!!.value
        assertTrue(url.startsWith("${AppEnvironment.PAGES_BASE_URL}/p#"))
        val decoded = ShareLinkCodec.decode(url)!!
        assertEquals("Twenty", decoded.title)
        assertEquals((1..20).map { "fx_series_40_e%02d.mp3".format(it) }, decoded.entries.map { it.fileName })
    }

    @Test fun L10_A23_tooLongLinkOffersFile() {
        // The 105-entry fixture compresses below 8,000 chars (R57 estimates 300-500 entries fit); lower the limit.
        AppEnvironment.shareLinkMaxLength = 1_000
        val id = playlistWith("Huge", "OTRR_Philip_Marlowe_Singles", emptyList())
        rule.openItem("OTRR_Philip_Marlowe_Singles")
        rule.tap(ElementIds.ITEM_OVERFLOW_BUTTON)
        rule.tap(ElementIds.itemMenu("add_to_playlist"))
        rule.tap(ElementIds.playlistPicker(id))
        openPlaylist(id)
        rule.tap(ElementIds.PLAYLIST_SHARE_BUTTON)
        rule.waitForTag(ElementIds.DIALOG_SHARE_TOO_LONG)
        val send = captureShare { rule.tap(ElementIds.SHARE_AS_FILE_BUTTON) }
        val stream = send.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        assertNotNull(stream)
        assertTrue(stream.toString().endsWith(".archiveplaylist"))
    }

    @Test fun L11_A21_R55_linkImportCreatesNewPlaylistNeverOverwrites() {
        val link = shareLink("Shared noir", (1..3).map { EntryRef("fx_radio_show", "fx_radio_show_e%02d.mp3".format(it)) })
        repeat(2) {
            launchView(Uri.parse(link))
            assertTrue(rule.textOf(ElementIds.IMPORT_TITLE).contains("Shared noir"))
            assertTrue(rule.textOf(ElementIds.IMPORT_COUNT).contains("3"))
            rule.tap(ElementIds.IMPORT_CONFIRM_BUTTON)
        }
        rule.openLibrary()
        val names = userPlaylists().map { rule.textOf(it) }
        assertTrue(names.toString(), names.any { it.contains("Shared noir (2)") })
        assertEquals(2, names.count { it.contains("Shared noir") })
    }

    @Test fun L12_A24_fileImportReportsSkipped() {
        val valid = (1..3).map { EntryRef("fx_radio_show", "fx_radio_show_e%02d.mp3".format(it)) }
        val json = org.json.JSONObject(PlaylistFileCodec.encode(
            com.jutlandiatech.archiveplayer.core.playlist.Playlist("x", "From file", null, valid)))
        val entries = json.getJSONArray("entries")
        entries.put(org.json.JSONObject().put("bogus", true)).put("not an entry")
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "test.archiveplaylist").apply { writeText(json.toString()) }
        launchView(Uri.fromFile(file), "application/json")
        assertTrue(rule.textOf(ElementIds.IMPORT_SKIPPED).contains("2"))
        rule.tap(ElementIds.IMPORT_CONFIRM_BUTTON)
        rule.openLibrary()
        val id = userPlaylists().single().removePrefix("library_playlist_")
        openPlaylist(id)
        rule.waitForTag(playlistEntryRow(2))
        assertFalse(rule.exists(playlistEntryRow(3)))
    }

    @Test fun I15_R70_adultItemFromShareLinkHidden() {
        launchView(Uri.parse(shareLink("Adult", listOf(EntryRef("fx_adult", "fx_adult.mp4")))))
        rule.tap(ElementIds.importEntry(0))
        rule.waitForTag(ElementIds.ITEM_HIDDEN_ADULT, 15_000)
        assertFalse(rule.exists(ElementIds.itemEntryRow(0)))
    }

    @Test fun L13_A25_mixedDownloadOnMobileWaitsForWifi() {
        AppEnvironment.networkOverride = NetworkKind.METERED
        val id = playlistWith("Mixed", "fx_series_12", listOf(0))
        rule.openItem("fx_film")
        rule.addEntryToPlaylist(0, id)
        openPlaylist(id)
        rule.tap(ElementIds.PLAYLIST_DOWNLOAD_BUTTON)
        rule.waitForTag(ElementIds.PLAYLIST_VIDEO_NOTICE)
        rule.openLibrary("downloads")
        rule.waitForTag(ElementIds.libraryDownloadMobileNow("fx_series_12"))
        assertFalse(rule.exists(ElementIds.libraryDownload("fx_film")))
        assertTrue(server.requests.none { it.path.orEmpty().startsWith("/download/fx_series_12/") })
        rule.tap(ElementIds.libraryDownloadMobileNow("fx_series_12"))
        rule.waitUntil(30_000) { server.requests.any { it.path.orEmpty().startsWith("/download/fx_series_12/") } }
    }

    @Test fun L14_R61_storageSummaryWarningRemove() {
        AppEnvironment.freeBytesOverride = 100_000_000
        listOf("fx_series_12", "fx_english").forEach {
            rule.openItem(it)
            rule.tap(ElementIds.ITEM_DOWNLOAD_BUTTON)
            rule.waitUntil(90_000) { rule.textOf(ElementIds.ITEM_DOWNLOAD_BUTTON).contains("Downloaded") }
        }
        rule.openLibrary("downloads")
        rule.waitForTag(ElementIds.LIBRARY_STORAGE_SUMMARY)
        rule.waitForTag(ElementIds.LIBRARY_STORAGE_WARNING)
        rule.tap(ElementIds.libraryDownloadRemove("fx_english"))
        rule.waitForGone(ElementIds.libraryDownload("fx_english"))
        rule.tap(ElementIds.LIBRARY_DOWNLOADS_REMOVE_ALL)
        rule.tap(ElementIds.DIALOG_CONFIRM_BUTTON)
        rule.waitForTag(ElementIds.LIBRARY_EMPTY)
    }

    @Test fun L15_R44_historyListClearAndPaused() {
        listOf(0, 1).forEach { entry ->
            rule.play("fx_radio_show", entry)
            PlayerProbe.connect().use { assertTrue(it.await { isPlaying }) }
        }
        rule.openLibrary("history")
        rule.waitForTag(ElementIds.libraryHistory(1))
        assertTrue(rule.textOf(ElementIds.libraryHistory(0)).contains("Episode 02"))
        rule.tap(ElementIds.LIBRARY_HISTORY_CLEAR)
        rule.tap(ElementIds.DIALOG_CONFIRM_BUTTON)
        rule.waitForTag(ElementIds.LIBRARY_EMPTY)
        rule.openSettingsRow("history")
        rule.tap(ElementIds.settingsSwitch("history_pause"))
        pressBack(rule); pressBack(rule)
        rule.openLibrary("history")
        rule.waitForTag(ElementIds.LIBRARY_HISTORY_PAUSED)
    }

    @Test fun L16_emptyStatesAllLanguages() {
        listOf("ENG" to "en", "DEN" to "da", "GER" to "de").forEach { (file, iso) ->
            rule.openSettingsRow("app_language")
            rule.tap(ElementIds.settingsLanguage(iso))
            pressBack(rule); pressBack(rule)
            listOf("liked", "following", "downloads", "history").forEach { filter ->
                rule.openLibrary(filter)
                rule.waitForTag(ElementIds.LIBRARY_EMPTY)
                assertEquals(catalog(file).text("library.empty.$filter"), rule.textOf(ElementIds.LIBRARY_EMPTY))
            }
        }
    }
}
