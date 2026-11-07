package com.thomasnorlund.archiveplayer.core.home

import com.thomasnorlund.archiveplayer.core.catalog.MediaKind
import com.thomasnorlund.archiveplayer.core.catalog.SearchHit
import com.thomasnorlund.archiveplayer.core.playlist.EntryRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeTest {
    private val hour = 3_600_000L
    private val min = 60_000L

    @Test fun R18_personalRowsFirstThenCurated() {
        val rows = HomeRows.build(HomeInput(1, 1, 1, 1, 1, 1, listOf("video.movies", "audio.otr")))
        assertEquals(listOf(HomeRowType.CONTINUE, HomeRowType.RECENTLY_PLAYED, HomeRowType.PLAYLISTS, HomeRowType.DOWNLOADS,
            HomeRowType.NEW_IN_FOLLOWED, HomeRowType.BECAUSE_YOU_PLAYED, HomeRowType.CURATED, HomeRowType.CURATED), rows.map { it.type })
        assertEquals(listOf("video.movies", "audio.otr"), rows.mapNotNull { it.categoryId })
    }

    @Test fun R18_emptyPersonalRowsHidden() {
        assertEquals(listOf(HomeRow(HomeRowType.CURATED, "audio.otr")), HomeRows.build(HomeInput(0, 0, 0, 0, 0, 0, listOf("audio.otr"))))
    }

    private fun event(id: String, collection: String, playedMs: Long, positionMs: Long = 20 * min, durationMs: Long = 30 * min) =
        HistoryEvent(EntryRef(id, "$id.mp3"), id, "Title $id", "Creator", listOf(collection), emptyList(), playedMs, positionMs, durationMs)

    private fun hit(id: String, collection: String, langs: Set<String> = emptySet()) =
        SearchHit(id, id, null, MediaKind.AUDIO, langs, listOf(collection), emptyList(), 0, null)

    @Test fun R19_becauseYouPlayedUsesLocalHistory() {
        val rec = Recommender.becauseYouPlayed(listOf(event("played", "oldtimeradio", 1)),
            listOf(hit("played", "oldtimeradio"), hit("sibling", "oldtimeradio"), hit("other", "jazz")))!!
        assertEquals("Title played", rec.seedTitle)
        assertEquals(listOf("sibling"), rec.items.map { it.identifier })
    }

    @Test fun R19_shortPlaysAreNoSeed() {
        assertNull(Recommender.becauseYouPlayed(listOf(event("p", "c", 1, positionMs = 10_000)), listOf(hit("s", "c"))))
        assertNull(Recommender.becauseYouPlayed(emptyList(), listOf(hit("s", "c"))))
    }

    @Test fun R21_refreshAtMostEverySixHours() {
        assertEquals(6 * hour, FollowRefreshPolicy.minIntervalMs)
        assertTrue(FollowRefreshPolicy.shouldRefresh(null, 0, manual = false))
        assertFalse(FollowRefreshPolicy.shouldRefresh(0, 5 * hour, manual = false))
        assertTrue(FollowRefreshPolicy.shouldRefresh(0, 6 * hour, manual = false))
    }

    @Test fun R21_onlyItemsAddedSinceLastCheck() {
        val items = listOf(hit("old", "c") to 1 * hour, hit("new", "c") to 3 * hour)
        assertEquals(listOf("new"), FollowRefreshPolicy.newSince(items, lastCheckMs = 2 * hour).map { it.identifier })
    }

    @Test fun R20_preferredLanguagesFirstUntaggedKept() {
        val hits = listOf(hit("en", "c", setOf("en")), hit("untagged", "c"), hit("da", "c", setOf("da")))
        assertEquals(listOf("da", "en", "untagged"), CuratedRanking.rank(hits, setOf("da")).map { it.identifier })
        assertEquals(hits, CuratedRanking.rank(hits, emptySet()))
    }

    @Test fun R44_pausedHistoryRecordsNothing() {
        val e = event("a", "c", 1)
        assertEquals(emptyList<HistoryEvent>(), HistoryRules.record(emptyList(), e, paused = true))
        assertEquals(listOf(e), HistoryRules.record(emptyList(), e, paused = false))
    }

    @Test fun A17_continueRowShowsUnfinishedNewestFirstOncePerEntry() {
        val history = listOf(
            event("film", "c", playedMs = 1, positionMs = 10 * min, durationMs = 90 * min),
            event("done", "c", playedMs = 2, positionMs = 89 * min + 50_000, durationMs = 90 * min),
            event("short", "c", playedMs = 3, positionMs = 5_000, durationMs = 90 * min),
            event("film", "c", playedMs = 4, positionMs = 40 * min, durationMs = 90 * min),
        )
        val row = HistoryRules.continueRow(history)
        assertEquals(listOf("film"), row.map { it.itemIdentifier })
        assertEquals(40 * min, row.single().positionMs)
    }
}
