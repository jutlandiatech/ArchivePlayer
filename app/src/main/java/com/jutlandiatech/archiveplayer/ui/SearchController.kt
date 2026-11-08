package com.jutlandiatech.archiveplayer.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jutlandiatech.archiveplayer.core.catalog.DurationClass
import com.jutlandiatech.archiveplayer.core.catalog.SearchHit
import com.jutlandiatech.archiveplayer.core.search.KindFilter
import com.jutlandiatech.archiveplayer.core.search.PlaylistSummary
import com.jutlandiatech.archiveplayer.core.search.ResultFilter
import com.jutlandiatech.archiveplayer.core.search.ResultGrouper
import com.jutlandiatech.archiveplayer.core.search.SearchRequest
import com.jutlandiatech.archiveplayer.core.search.SortOrder
import com.jutlandiatech.archiveplayer.core.search.SuggestionEngine
import com.jutlandiatech.archiveplayer.data.ArchiveRepository
import com.jutlandiatech.archiveplayer.data.LibraryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Search tab state; lives in the ViewModel so it survives item visits and recreation (Q12). */
class SearchController(
    private val scope: CoroutineScope,
    private val repo: ArchiveRepository,
    private val library: LibraryStore,
    private val showAdult: () -> Boolean,
    private val appLanguage: () -> String,
) {
    var query by mutableStateOf("")
    var submitted by mutableStateOf<String?>(null)
        private set
    var kind by mutableStateOf(KindFilter.ALL)
        private set
    var sort by mutableStateOf(SortOrder.RELEVANCE)
        private set
    var languages by mutableStateOf<Set<String>>(emptySet())
        private set
    var decade by mutableStateOf<Int?>(null)
        private set
    var duration by mutableStateOf<DurationClass?>(null)
        private set
    var hits by mutableStateOf<List<SearchHit>>(emptyList())
        private set
    var total by mutableStateOf(0)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf(false)
        private set
    var pagingError by mutableStateOf(false)
        private set
    private var page = 1
    private var job: Job? = null

    val hasFilters: Boolean get() = kind != KindFilter.ALL || languages.isNotEmpty() || decade != null || duration != null

    fun suggestions(): List<String> {
        val q = query.trim()
        if (q.isEmpty() || q == submitted) return emptyList()
        val names = repo.categoryMap.all().map { it.name(appLanguage()) }
        return SuggestionEngine.suggest(q, library.state.recentSearches, library.state.follows, names)
    }

    fun playlistMatches(): List<PlaylistSummary> {
        val q = submitted?.trim().orEmpty()
        return library.state.playlists.filter { !it.isLiked && q.isNotEmpty() && it.title.contains(q, ignoreCase = true) }
            .map { PlaylistSummary(it.id, it.title) }
    }

    fun grouped() = ResultGrouper.group(hits, submitted.orEmpty(), playlistMatches())

    fun submit(text: String = query) {
        val q = text.trim()
        if (q.isEmpty()) return
        query = q
        submitted = q
        library.update { s -> s.copy(recentSearches = (listOf(q) + s.recentSearches.filterNot { it.equals(q, true) }).take(20)) }
        run(reset = true)
    }

    fun clear() {
        query = ""
        submitted = null
        hits = emptyList()
        job?.cancel()
    }

    fun removeRecent(index: Int) = library.update { s -> s.copy(recentSearches = s.recentSearches.filterIndexed { i, _ -> i != index }) }

    fun chooseKind(k: KindFilter) { kind = k; run(true) }
    fun chooseSort(s: SortOrder) { sort = s; run(true) }
    fun chooseLanguage(iso: String?) { languages = if (iso == null) emptySet() else setOf(iso); run(true) }
    fun chooseDecade(d: Int?) { decade = d; run(true) }
    fun chooseDuration(d: DurationClass?) { duration = d; run(true) }
    fun clearFilters() { kind = KindFilter.ALL; languages = emptySet(); decade = null; duration = null; run(true) }
    fun retry() = run(reset = hits.isEmpty())

    private fun request(p: Int) = SearchRequest(submitted.orEmpty(), kind, null, languages, decade, duration, sort, p, ROWS)

    fun loadMore() {
        if (loading || error || pagingError || hits.size >= total || submitted == null) return
        run(reset = false)
    }

    private fun run(reset: Boolean) {
        if (submitted == null) return
        job?.cancel()
        if (reset) { page = 1; hits = emptyList(); total = 0 }
        val target = if (reset) 1 else page + 1
        loading = true
        error = false
        pagingError = false
        job = scope.launch {
            try {
                val result = repo.search(request(target), showAdult())
                val filtered = ResultFilter.apply(result.hits, request(target), if (showAdult()) null else repo.adultFilter)
                val known = hits.map { it.identifier }.toHashSet()
                hits = hits + filtered.filter { known.add(it.identifier) }
                total = result.total
                android.util.Log.i("ArchivePlayer", "search '${submitted}' page $target: ${result.hits.size} raw, ${filtered.size} kept, total ${result.total}")
                page = target
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("ArchivePlayer", "search failed: ${e.message}", e)
                if (target == 1) error = true else pagingError = true
            } finally {
                loading = false
            }
        }
    }

    companion object {
        const val ROWS = 50
    }
}
