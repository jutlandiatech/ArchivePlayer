package com.jutlandiatech.archiveplayer.core.search

import com.jutlandiatech.archiveplayer.core.catalog.DurationClass
import com.jutlandiatech.archiveplayer.core.catalog.MediaKind
import com.jutlandiatech.archiveplayer.core.catalog.SearchHit
import com.jutlandiatech.archiveplayer.core.category.CategoryQuery
import com.jutlandiatech.archiveplayer.core.filter.AdultFilter
import com.jutlandiatech.archiveplayer.core.language.LanguageFilter
import com.jutlandiatech.archiveplayer.core.language.LanguageNormalizer
import java.net.URLEncoder

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
    val FIELDS = listOf("identifier", "title", "creator", "mediatype", "language", "collection", "downloads", "date", "subject")

    private fun quote(value: String): String =
        if (value.any { it.isWhitespace() || it in "():\"" }) "\"" + value.replace("\"", "") + "\"" else value

    private fun anyOf(field: String, values: List<String>): String = "$field:(" + values.joinToString(" OR ") { quote(it) } + ")"

    fun categoryClause(category: CategoryQuery): String? {
        val parts = buildList {
            if (category.collections.isNotEmpty()) add(anyOf("collection", category.collections))
            if (category.subjects.isNotEmpty()) add(anyOf("subject", category.subjects))
        }
        val base = if (parts.isEmpty()) null else "(" + parts.joinToString(" OR ") + ")"
        val extra = category.extra?.takeIf { it.isNotBlank() }?.let { "($it)" }
        return listOfNotNull(base, extra).takeIf { it.isNotEmpty() }?.joinToString(" AND ")
    }

    fun query(request: SearchRequest, adultFilter: AdultFilter?): String {
        val clauses = mutableListOf<String>()
        request.text.trim().takeIf { it.isNotEmpty() }?.let { clauses += "($it)" }
        clauses += when (request.kind) {
            KindFilter.ALL -> "mediatype:(movies OR audio OR etree)"
            KindFilter.AUDIO -> "mediatype:(audio OR etree)"
            KindFilter.VIDEO -> "mediatype:(movies)"
        }
        request.category?.let { categoryClause(it) }?.let { clauses += it }
        if (request.languages.isNotEmpty()) {
            clauses += anyOf("language", request.languages.sorted().flatMap { LanguageNormalizer.queryValues(it) })
        }
        request.decade?.let { clauses += "year:[$it TO ${it + 9}]" }
        var q = clauses.joinToString(" AND ")
        if (adultFilter != null) q += " " + adultFilter.queryExclusion
        return q
    }

    fun build(request: SearchRequest, adultFilter: AdultFilter?, baseUrl: String = "https://archive.org"): String {
        val params = buildList {
            add("q" to query(request, adultFilter))
            FIELDS.forEach { add("fl[]" to it) }
            when (request.sort) {
                SortOrder.RELEVANCE -> Unit
                SortOrder.POPULARITY -> add("sort[]" to "downloads desc")
                SortOrder.NEWEST -> add("sort[]" to "addeddate desc")
                SortOrder.DATE -> add("sort[]" to "date desc")
            }
            add("rows" to request.rows.toString())
            add("page" to request.page.toString())
            add("output" to "json")
        }
        return "${baseUrl.trimEnd('/')}/advancedsearch.php?" + params.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
    }
}

/** archive.org's own website search, relevance-ranked like archive.org/search (R87); undocumented, so callers fall back to [AdvancedSearchUrl]. */
object WebSearchUrl {
    fun applies(request: SearchRequest): Boolean =
        request.text.isNotBlank() && request.sort == SortOrder.RELEVANCE && request.category == null

    fun build(request: SearchRequest, baseUrl: String = "https://archive.org"): String {
        val kinds = when (request.kind) {
            KindFilter.ALL -> listOf("movies", "audio", "etree")
            KindFilter.AUDIO -> listOf("audio", "etree")
            KindFilter.VIDEO -> listOf("movies")
        }
        val filter = "{\"mediatype\":{" + kinds.joinToString(",") { "\"$it\":\"inc\"" } + "}}"
        val params = listOf(
            "user_query" to request.text.trim(),
            "service_backend" to "metadata",
            "hits_per_page" to request.rows.toString(),
            "page" to request.page.toString(),
            "filter_map" to filter,
        )
        return "${baseUrl.trimEnd('/')}/services/search/beta/page_production/?" + params.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
    }
}

/** Client-side result rules applied after parsing (R28, R71). */
object ResultFilter {
    fun apply(hits: List<SearchHit>, request: SearchRequest, adultFilter: AdultFilter?): List<SearchHit> = hits.filter { hit ->
        (adultFilter == null || !adultFilter.isAdult(hit)) &&
            (hit.kind == null || LanguageFilter.matches(hit.languages, request.languages)) &&
            when (request.kind) {
                KindFilter.ALL -> true
                KindFilter.AUDIO -> hit.kind == null || hit.kind == MediaKind.AUDIO
                KindFilter.VIDEO -> hit.kind == null || hit.kind == MediaKind.VIDEO
            }
    }
}

data class PlaylistSummary(val id: String, val title: String)

data class GroupedResults(val collections: List<SearchHit>, val items: List<SearchHit>, val playlists: List<PlaylistSummary>)

/** Groups results into Collections / Items / own playlists (R25). */
object ResultGrouper {
    fun group(hits: List<SearchHit>, query: String, playlists: List<PlaylistSummary>): GroupedResults {
        val (collections, items) = hits.partition { it.kind == null }
        val q = query.trim()
        val matching = if (q.isEmpty()) emptyList() else playlists.filter { it.title.contains(q, ignoreCase = true) }
        return GroupedResults(collections, items, matching)
    }
}

/** Local as-you-type suggestions (R22). */
object SuggestionEngine {
    val debounceMillis: Long get() = 300L

    fun suggest(prefix: String, history: List<String>, followed: List<String>, categoryNames: List<String>, limit: Int = 8): List<String> {
        val p = prefix.trim()
        if (p.isEmpty()) return emptyList()
        val seen = mutableSetOf<String>()
        return (history + followed + categoryNames).filter { candidate ->
            candidate.split(' ', '-', '_').any { it.startsWith(p, ignoreCase = true) } ||
                candidate.startsWith(p, ignoreCase = true)
        }.filter { seen.add(it.lowercase()) }.take(limit)
    }
}

/** Recent searches, newest first, de-duplicated case-insensitively (R29). */
class RecentSearches(private val max: Int = 20, initial: List<String> = emptyList()) {
    private val list = initial.toMutableList()
    val items: List<String> get() = list.toList()

    fun add(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        list.removeAll { it.equals(q, ignoreCase = true) }
        list.add(0, q)
        while (list.size > max) list.removeAt(list.lastIndex)
    }

    fun remove(query: String) {
        list.removeAll { it.equals(query.trim(), ignoreCase = true) }
    }

    fun clear() = list.clear()
}
