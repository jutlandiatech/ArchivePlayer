package com.thomasnorlund.archiveplayer.core.search

import com.thomasnorlund.archiveplayer.core.catalog.MediaKind
import com.thomasnorlund.archiveplayer.core.catalog.SearchHit
import com.thomasnorlund.archiveplayer.core.category.CategoryQuery
import com.thomasnorlund.archiveplayer.core.filter.AdultFilter
import com.thomasnorlund.archiveplayer.core.filter.KeywordList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.net.URLDecoder

class SearchTest {
    private val adult = AdultFilter(KeywordList("1", listOf("porn")))

    private fun params(url: String): Map<String, List<String>> =
        URI(url).rawQuery.split("&").map { it.split("=", limit = 2) }
            .groupBy({ URLDecoder.decode(it[0], "UTF-8") }, { URLDecoder.decode(it.getOrElse(1) { "" }, "UTF-8") })

    private fun q(url: String) = params(url).getValue("q").single()

    private fun hit(id: String, langs: Set<String> = emptySet(), kind: MediaKind? = MediaKind.AUDIO, title: String = id) =
        SearchHit(id, title, null, kind, langs, emptyList(), emptyList(), 0, null)

    @Test fun A10_audioPopularityQuery() {
        val url = AdvancedSearchUrl.build(SearchRequest("gunsmoke", kind = KindFilter.AUDIO, sort = SortOrder.POPULARITY), adult)
        assertTrue(url.startsWith("https://archive.org/advancedsearch.php?"))
        assertFalse(url.contains(" "))
        val p = params(url)
        assertTrue(q(url).contains("gunsmoke"))
        assertTrue(q(url).contains("mediatype:(audio OR etree)"))
        assertEquals(listOf("downloads desc"), p["sort[]"])
        assertEquals(listOf("json"), p["output"])
        assertEquals(listOf("50"), p["rows"])
        assertEquals(listOf("1"), p["page"])
        assertTrue(p.getValue("fl[]").containsAll(listOf("identifier", "title", "creator", "mediatype", "language", "collection", "downloads", "date", "subject")))
    }

    @Test fun R07_mediaKindClauses() {
        assertTrue(q(AdvancedSearchUrl.build(SearchRequest("x"), null)).contains("mediatype:(movies OR audio OR etree)"))
        assertTrue(q(AdvancedSearchUrl.build(SearchRequest("x", kind = KindFilter.VIDEO), null)).contains("mediatype:(movies)"))
    }

    @Test fun R71_adultExclusionOnlyWhileHidden() {
        assertTrue(q(AdvancedSearchUrl.build(SearchRequest("x"), adult)).contains("-collection:no-preview"))
        assertFalse(q(AdvancedSearchUrl.build(SearchRequest("x"), null)).contains("no-preview"))
    }

    @Test fun R23_filtersCombine() {
        val query = q(AdvancedSearchUrl.build(SearchRequest("x", languages = setOf("fr"), decade = 1950,
            category = CategoryQuery(listOf("Film_Noir"), listOf("film noir"), null)), null))
        assertTrue(query, query.contains("language:"))
        assertTrue(query, query.contains("French"))
        assertTrue(query, query.contains("year:[1950 TO 1959]"))
        assertTrue(query, query.contains("Film_Noir"))
        assertTrue(query, query.contains("\"film noir\""))
    }

    @Test fun R24_sortOrders() {
        assertEquals(listOf("addeddate desc"), params(AdvancedSearchUrl.build(SearchRequest("x", sort = SortOrder.NEWEST), null))["sort[]"])
        assertTrue(params(AdvancedSearchUrl.build(SearchRequest("x", sort = SortOrder.DATE), null)).getValue("sort[]").single().startsWith("date "))
        assertEquals(null, params(AdvancedSearchUrl.build(SearchRequest("x"), null))["sort[]"])
    }

    @Test fun R25_paging() {
        val p = params(AdvancedSearchUrl.build(SearchRequest("x", page = 3, rows = 20), null))
        assertEquals(listOf("3"), p["page"]); assertEquals(listOf("20"), p["rows"])
    }

    @Test fun R26_browseWithoutTextUsesCategoryOnly() {
        val query = q(AdvancedSearchUrl.build(SearchRequest("", category = CategoryQuery(listOf("oldtimeradio"), emptyList(), null)), null))
        assertTrue(query.contains("oldtimeradio"))
    }

    @Test fun A06_A07_clientSideLanguageRule() {
        val hits = listOf(hit("untagged"), hit("french", setOf("fr")), hit("english", setOf("en")))
        assertEquals(listOf("french"), ResultFilter.apply(hits, SearchRequest("x", languages = setOf("fr")), null).map { it.identifier })
        assertEquals(3, ResultFilter.apply(hits, SearchRequest("x"), null).size)
    }

    @Test fun A10_resultsGrouped() {
        val hits = listOf(hit("gunsmoke_collection", kind = null), hit("ep1"), hit("ep2"))
        val g = ResultGrouper.group(hits, "gunsmoke", listOf(PlaylistSummary("p1", "My Gunsmoke favourites"), PlaylistSummary("p2", "Jazz")))
        assertEquals(listOf("gunsmoke_collection"), g.collections.map { it.identifier })
        assertEquals(listOf("ep1", "ep2"), g.items.map { it.identifier })
        assertEquals(listOf("p1"), g.playlists.map { it.id })
    }

    @Test fun R22_debounceIs300ms() = assertEquals(300L, SuggestionEngine.debounceMillis)

    @Test fun R22_localSuggestions() {
        val s = SuggestionEngine.suggest("gun", history = listOf("Gunsmoke", "jazz"), followed = listOf("gunsmoke", "Gunfighters"),
            categoryNames = listOf("Westerns", "Gun stories"))
        assertEquals("Gunsmoke", s.first())
        assertEquals(1, s.count { it.equals("gunsmoke", ignoreCase = true) })
        assertTrue(s.containsAll(listOf("Gunfighters", "Gun stories")))
        assertFalse(s.contains("jazz"))
        assertEquals(emptyList<String>(), SuggestionEngine.suggest(" ", listOf("a"), emptyList(), emptyList()))
        assertEquals(2, SuggestionEngine.suggest("g", listOf("g1", "g2", "g3"), emptyList(), emptyList(), limit = 2).size)
    }

    @Test fun R29_recentSearches() {
        val r = RecentSearches(max = 3)
        r.add("jazz"); r.add("gunsmoke"); r.add("Jazz"); r.add("  ")
        assertEquals(listOf("Jazz", "gunsmoke"), r.items)
        r.add("a"); r.add("b")
        assertEquals(listOf("b", "a", "Jazz"), r.items)
        r.remove("a")
        assertEquals(listOf("b", "Jazz"), r.items)
        r.clear()
        assertTrue(r.items.isEmpty())
    }
}
