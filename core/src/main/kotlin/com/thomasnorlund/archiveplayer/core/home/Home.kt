package com.thomasnorlund.archiveplayer.core.home

import com.thomasnorlund.archiveplayer.core.catalog.SearchHit
import com.thomasnorlund.archiveplayer.core.playlist.EntryRef

data class HistoryEvent(
    val ref: EntryRef,
    val itemIdentifier: String,
    val itemTitle: String,
    val creator: String?,
    val collections: List<String>,
    val subjects: List<String>,
    val playedAtMs: Long,
    val positionMs: Long,
    val durationMs: Long,
)

enum class HomeRowType { CONTINUE, RECENTLY_PLAYED, PLAYLISTS, DOWNLOADS, NEW_IN_FOLLOWED, BECAUSE_YOU_PLAYED, CURATED }

data class HomeRow(val type: HomeRowType, val categoryId: String? = null)

data class HomeInput(
    val continueCount: Int,
    val recentCount: Int,
    val playlistCount: Int,
    val downloadCount: Int,
    val newInFollowedCount: Int,
    val becauseYouPlayedCount: Int,
    val curatedCategoryIds: List<String>,
)

/** Home row order and visibility (R18). */
object HomeRows {
    fun build(input: HomeInput): List<HomeRow> = TODO()
}

data class Recommendation(val seedTitle: String, val items: List<SearchHit>)

/** On-device "Because you played X" (R19). */
object Recommender {
    fun becauseYouPlayed(history: List<HistoryEvent>, candidates: List<SearchHit>, limit: Int = 20): Recommendation? = TODO()
}

/** "New in collections you follow" refresh rules (R21). */
object FollowRefreshPolicy {
    val minIntervalMs: Long get() = TODO()
    fun shouldRefresh(lastCheckMs: Long?, nowMs: Long, manual: Boolean): Boolean = TODO()
    fun newSince(items: List<Pair<SearchHit, Long>>, lastCheckMs: Long?): List<SearchHit> = TODO()
}

/** Curated rows favour preferred languages without hiding untagged items (R20). */
object CuratedRanking {
    fun rank(hits: List<SearchHit>, preferred: Set<String>): List<SearchHit> = TODO()
}

/** History recording and Continue rows (R36, R44). */
object HistoryRules {
    fun record(history: List<HistoryEvent>, event: HistoryEvent, paused: Boolean): List<HistoryEvent> = TODO()
    fun continueRow(history: List<HistoryEvent>): List<HistoryEvent> = TODO()
}
