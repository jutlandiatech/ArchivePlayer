package com.thomasnorlund.archiveplayer.core.home

import com.thomasnorlund.archiveplayer.core.catalog.SearchHit
import com.thomasnorlund.archiveplayer.core.playback.ResumePolicy
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
    fun build(input: HomeInput): List<HomeRow> = buildList {
        if (input.continueCount > 0) add(HomeRow(HomeRowType.CONTINUE))
        if (input.recentCount > 0) add(HomeRow(HomeRowType.RECENTLY_PLAYED))
        if (input.playlistCount > 0) add(HomeRow(HomeRowType.PLAYLISTS))
        if (input.downloadCount > 0) add(HomeRow(HomeRowType.DOWNLOADS))
        if (input.newInFollowedCount > 0) add(HomeRow(HomeRowType.NEW_IN_FOLLOWED))
        if (input.becauseYouPlayedCount > 0) add(HomeRow(HomeRowType.BECAUSE_YOU_PLAYED))
        input.curatedCategoryIds.forEach { add(HomeRow(HomeRowType.CURATED, it)) }
    }
}

data class Recommendation(val seedTitle: String, val items: List<SearchHit>)

/** On-device "Because you played X" (R19). */
object Recommender {
    /** The most recent meaningful play (over 30 s) used as seed. */
    fun seed(history: List<HistoryEvent>): HistoryEvent? =
        history.filter { it.positionMs > MIN_SEED_MS }.maxByOrNull { it.playedAtMs }

    fun becauseYouPlayed(history: List<HistoryEvent>, candidates: List<SearchHit>, limit: Int = 20): Recommendation? {
        val seed = seed(history) ?: return null
        val played = history.map { it.itemIdentifier }.toSet()
        val items = candidates.filter { hit ->
            hit.identifier !in played &&
                (hit.collections.any { it in seed.collections } || hit.subjects.any { it in seed.subjects })
        }.distinctBy { it.identifier }.take(limit)
        return if (items.isEmpty()) null else Recommendation(seed.itemTitle, items)
    }

    private const val MIN_SEED_MS = 30_000L
}

/** "New in collections you follow" refresh rules (R21). */
object FollowRefreshPolicy {
    val minIntervalMs: Long get() = 6 * 3_600_000L
    fun shouldRefresh(lastCheckMs: Long?, nowMs: Long, manual: Boolean): Boolean =
        manual || lastCheckMs == null || nowMs - lastCheckMs >= minIntervalMs
    fun newSince(items: List<Pair<SearchHit, Long>>, lastCheckMs: Long?): List<SearchHit> =
        items.filter { (_, addedAt) -> lastCheckMs == null || addedAt > lastCheckMs }.map { it.first }
}

/** Curated rows favour preferred languages without hiding untagged items (R20). */
object CuratedRanking {
    fun rank(hits: List<SearchHit>, preferred: Set<String>): List<SearchHit> =
        if (preferred.isEmpty()) hits else hits.sortedBy { hit ->
            when {
                hit.languages.any { it in preferred } -> 0
                hit.languages.isNotEmpty() -> 1
                else -> 2
            }
        }
}

/** History recording and Continue rows (R36, R44). */
object HistoryRules {
    fun record(history: List<HistoryEvent>, event: HistoryEvent, paused: Boolean): List<HistoryEvent> =
        if (paused) history else history + event
    fun continueRow(history: List<HistoryEvent>): List<HistoryEvent> =
        history.groupBy { it.ref }.values.map { events -> events.maxBy { it.playedAtMs } }
            .filter { ResumePolicy.shouldStore(it.positionMs, it.durationMs) }
            .sortedByDescending { it.playedAtMs }
}
