package com.jutlandiatech.archiveplayer.core.playlist

import com.jutlandiatech.archiveplayer.core.settings.AppSettings
import com.jutlandiatech.archiveplayer.core.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlaylistsTest {
    private fun refs(n: Int, prefix: String = "item") = (1..n).map { EntryRef("${prefix}_$it", "track_$it.mp3") }
    private fun playlist(n: Int) = Playlist("p1", "Road trip", "Long drive", refs(n))

    @Test fun A21_shareLinkRoundTripKeepsOrder() {
        val result = ShareLinkCodec.share(playlist(20))
        assertTrue(result is ShareResult.Link)
        val url = (result as ShareResult.Link).url
        assertTrue(url.startsWith(ShareLinkCodec.baseUrl))
        assertEquals("https://jutlandiatech.github.io/ArchivePlayer/", ShareLinkCodec.baseUrl)
        assertEquals(SharedPlaylist("Road trip", "Long drive", refs(20)), ShareLinkCodec.decode(url))
    }

    @Test fun R54_payloadOnlyInFragment() {
        val url = (ShareLinkCodec.share(playlist(3)) as ShareResult.Link).url
        assertTrue(url.contains("#"))
        assertFalse(url.substringBefore("#").contains("?"))
    }

    @Test fun R54_unicodeTitleRoundTrip() {
        val shared = SharedPlaylist("Dansk æøå — 日本", null, refs(2))
        assertEquals(shared, ShareLinkCodec.decode(ShareLinkCodec.encode(shared)))
    }

    @Test fun A23_tooLongLinkRequiresFile() {
        val rnd = Random(1)
        val big = Playlist("p", "Huge", null, (1..2000).map { EntryRef("id_${rnd.nextLong()}", "f_${rnd.nextLong()}.mp3") })
        val result = ShareLinkCodec.share(big)
        assertTrue(result is ShareResult.FileRequired)
        assertTrue((result as ShareResult.FileRequired).linkLength > ShareLinkCodec.maxLength)
        assertEquals(8000, ShareLinkCodec.maxLength)
    }

    @Test fun R57_linksStayWithinLimit() {
        val url = (ShareLinkCodec.share(playlist(150)) as ShareResult.Link).url
        assertTrue(url.length <= 8000)
    }

    @Test fun R54_foreignOrCorruptLinksRejected() {
        assertNull(ShareLinkCodec.decode("https://example.com/#abc"))
        assertNull(ShareLinkCodec.decode(ShareLinkCodec.baseUrl + "#v1.%%%corrupt"))
        assertNull(ShareLinkCodec.decode(ShareLinkCodec.baseUrl + "#v99.AAAA"))
    }

    @Test fun A24_playlistFileRoundTripCreatesNewPlaylist() {
        assertEquals("archiveplaylist", PlaylistFileCodec.extension)
        val report = PlaylistFileCodec.decode(PlaylistFileCodec.encode(playlist(5)), newId = "new-1")
        assertEquals("new-1", report.playlist!!.id)
        assertEquals("Road trip", report.playlist!!.title)
        assertEquals(refs(5), report.playlist!!.entries.map { it.copy(title = null) })
        assertTrue(report.skipped.isEmpty())
    }

    @Test fun A24_invalidEntriesReported() {
        val json = """{"format":"archiveplaylist","schemaVersion":1,"title":"T","entries":[
            {"identifier":"ok","fileName":"a.mp3"},{"identifier":"","fileName":"b.mp3"},{"identifier":"x"}]}"""
        val report = PlaylistFileCodec.decode(json, "n")
        assertEquals(listOf(EntryRef("ok", "a.mp3")), report.playlist!!.entries.map { it.copy(title = null) })
        assertEquals(2, report.skipped.size)
    }

    @Test fun R58_invalidOrNewerFilesRejected() {
        assertNull(PlaylistFileCodec.decode("{bad", "n").playlist)
        assertNull(PlaylistFileCodec.decode("""{"format":"archiveplaylist","schemaVersion":99,"title":"T","entries":[]}""", "n").playlist)
    }

    @Test fun R52_likedListCannotBeRenamedOrDeleted() {
        val liked = Playlist("liked", "Liked", null, refs(2), isLiked = true)
        assertThrows(LikedListException::class.java) { PlaylistRules.rename(liked, "Mine") }
        assertFalse(PlaylistRules.canDelete(liked))
        assertTrue(PlaylistRules.canDelete(playlist(1)))
    }

    @Test fun R51_editing() {
        val p = playlist(3)
        assertEquals("New", PlaylistRules.rename(p, "New").title)
        assertEquals(listOf("item_3", "item_1", "item_2"), PlaylistRules.reorder(p, 2, 0).entries.map { it.identifier })
        assertEquals(listOf("item_1", "item_3"), PlaylistRules.remove(p, 1).entries.map { it.identifier })
        assertEquals(5, PlaylistRules.addItemEntries(p, refs(2, "video")).entries.size)
    }

    private val settings = AppSettings.defaults("da-DK", listOf("en", "de", "da")).copy(theme = ThemeMode.NOIR)
    private fun library(vararg playlists: Playlist, s: AppSettings? = settings) =
        Library(playlists.toList(), refs(2, "liked"), listOf("oldtimeradio"), listOf(LibraryHistoryRecord(EntryRef("h", "h.mp3"), 1, 2)), s)

    @Test fun A29_libraryExportImportRoundTrip() {
        val lib = library(playlist(3))
        assertEquals(lib, LibraryCodec.decode(LibraryCodec.encode(lib)))
    }

    @Test fun A29_importIntoClearedAppRestoresEverything() {
        val empty = Library(emptyList(), emptyList(), emptyList(), emptyList(), null)
        val result = LibraryMerger.merge(empty, library(playlist(3)))
        assertEquals(library(playlist(3)), result.library)
        assertTrue(result.conflicts.isEmpty())
    }

    @Test fun R74_mergeNeverSilentlyOverwrites() {
        val local = library(playlist(3))
        val imported = library(playlist(3).copy(title = "Changed"))
        val result = LibraryMerger.merge(local, imported)
        assertEquals(setOf("Road trip", "Changed"), result.library.playlists.map { it.title }.toSet())
        assertEquals(1, result.conflicts.size)
        assertEquals(2, result.library.likes.size)
        assertEquals(listOf("oldtimeradio"), result.library.follows)
    }
}
