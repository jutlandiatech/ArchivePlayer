package com.thomasnorlund.archiveplayer.core.search

import com.thomasnorlund.archiveplayer.core.catalog.DurationClass
import com.thomasnorlund.archiveplayer.core.catalog.SearchHit
import com.thomasnorlund.archiveplayer.core.category.CategoryQuery
import com.thomasnorlund.archiveplayer.core.filter.AdultFilter

enum class SortOrder { RELEVANCE, POPULARITY, NEWEST, DATE }
enum class KindFilter { ALL, AUDIO, VIDEO }

data class SearchRequest(
    val text: String,
    val kind: KindFilter = KindFilter.ALL,
    val category: CategoryQuery? = null,
    val languages: Set<String> = emptySet(),
    val decade: Int? = null,
    val duration: DurationClass? = null,
    val sort: SortOrder = SortOrder.RELEVANCE,
    val page: Int = 1,
    val rows: Int = 50,
)

/** Builds `https://archive.org/advancedsearch.php` URLs (R22–R26, R28, R71). */
object AdvancedSearchUrl {
    fun build(request: SearchRequest, adultFilter: AdultFilter?): String = TODO()
}

/** Client-side result rules applied after parsing (R28, R71). */
object ResultFilter {
    fun apply(hits: List<SearchHit>, request: SearchRequest, adultFilter: AdultFilter?): List<SearchHit> = TODO()
}

data class PlaylistSummary(val id: String, val title: String)

data class GroupedResults(val collections: List<SearchHit>, val items: List<SearchHit>, val playlists: List<PlaylistSummary>)

/** Groups results into Collections / Items / own playlists (R25). */
object ResultGrouper {
    fun group(hits: List<SearchHit>, query: String, playlists: List<PlaylistSummary>): GroupedResults = TODO()
}

/** Local as-you-type suggestions (R22). */
object SuggestionEngine {
    val debounceMillis: Long get() = TODO()
    fun suggest(prefix: String, history: List<String>, followed: List<String>, categoryNames: List<String>, limit: Int = 8): List<String> = TODO()
}

/** Recent searches, newest first, de-duplicated case-insensitively (R29). */
class RecentSearches(private val max: Int = 20) {
    val items: List<String> get() = TODO()
    fun add(query: String): Unit = TODO()
    fun remove(query: String): Unit = TODO()
    fun clear(): Unit = TODO()
}
