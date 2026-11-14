package com.jutlandiatech.archiveplayer.core.search

import com.jutlandiatech.archiveplayer.core.catalog.MediaKind
import com.jutlandiatech.archiveplayer.core.catalog.WebSearchResponseParser
import com.jutlandiatech.archiveplayer.core.category.CategoryQuery
import com.jutlandiatech.archiveplayer.core.support.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.net.URLDecoder

/** archive.org website search with advancedsearch fallback (R87, A47). */
class WebSearchTest {
    private fun params(url: String): Map<String, String> = URI(url).rawQuery.split('&').associate {
        val (k, v) = it.split('=', limit = 2)
        URLDecoder.decode(k, "UTF-8") to URLDecoder.decode(v, "UTF-8")
    }

    @Test fun R87_urlUsesWebsiteSearchWithMediaKinds() {
        val url = WebSearchUrl.build(SearchRequest("his girl friday", page = 2, rows = 30), "https://archive.org")
        assertTrue(url.startsWith("https://archive.org/services/search/beta/page_production/?"))
        val p = params(url)
        assertEquals("his girl friday", p["user_query"])
        assertEquals("metadata", p["service_backend"])
        assertEquals("30", p["hits_per_page"])
        assertEquals("2", p["page"])
        val kinds = p.getValue("filter_map")
        listOf("movies", "audio", "etree").forEach { assertTrue(kinds, kinds.contains("\"$it\":\"inc\"")) }
    }

    @Test fun R87_kindFilterNarrowsMediaTypes() {
        val video = params(WebSearchUrl.build(SearchRequest("x", kind = KindFilter.VIDEO), "https://archive.org")).getValue("filter_map")
        assertTrue(video.contains("\"movies\":\"inc\""))
        assertFalse(video.contains("\"audio\""))
        val audio = params(WebSearchUrl.build(SearchRequest("x", kind = KindFilter.AUDIO), "https://archive.org")).getValue("filter_map")
        assertTrue(audio.contains("\"audio\":\"inc\"") && audio.contains("\"etree\":\"inc\""))
        assertFalse(audio.contains("\"movies\""))
    }

    @Test fun R87_usedForRelevanceTextSearchOnly() {
        assertTrue(WebSearchUrl.applies(SearchRequest("marlowe")))
        assertFalse(WebSearchUrl.applies(SearchRequest("   ")))
        assertFalse(WebSearchUrl.applies(SearchRequest("marlowe", sort = SortOrder.POPULARITY)))
        assertFalse(WebSearchUrl.applies(SearchRequest("marlowe", category = CategoryQuery(listOf("oldtimeradio"), emptyList(), null))))
    }

    @Test fun A47_parsesWebsiteSearchResponseInRankOrder() {
        val page = WebSearchResponseParser.parse(Fixtures.archive("page_production_his_girl_friday.json"))!!
        assertEquals(6299, page.total)
        val first = page.hits.first()
        assertEquals("HisGirlFriday-1940", first.identifier)
        assertEquals("His Girl Friday - 1940", first.title)
        assertEquals(MediaKind.VIDEO, first.kind)
        assertEquals(147904L, first.downloads)
        assertTrue(first.collections.contains("feature_films"))
        assertEquals(5, page.hits.size)
    }

    @Test fun R87_unparseableResponseMeansFallback() {
        assertNull(WebSearchResponseParser.parse("<html>busy</html>"))
        assertNull(WebSearchResponseParser.parse("{\"response\":{}}"))
    }
}
