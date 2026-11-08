package com.jutlandiatech.archiveplayer.core.category

import com.jutlandiatech.archiveplayer.core.catalog.SearchResponseParser
import com.jutlandiatech.archiveplayer.core.search.AdvancedSearchUrl
import com.jutlandiatech.archiveplayer.core.search.SearchRequest
import com.jutlandiatech.archiveplayer.core.support.Fixtures
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.URI

/** Opt-in live check (R17): run with `-Darchiveplayer.live=true`; not part of the default build. */
class LiveCategoryMapCheck {
    @Test fun R17_everyQueryReturnsPlayableResults() {
        assumeTrue(System.getProperty("archiveplayer.live") == "true")
        val map = CategoryMapParser.parse(Fixtures.appAsset("catalog/category-map.json").readText())
        val thin = map.all().filter { it.children.isEmpty() }.mapNotNull { leaf ->
            val url = AdvancedSearchUrl.build(SearchRequest(text = "", category = leaf.query, rows = 10), adultFilter = null)
            val total = SearchResponseParser.parse(URI(url).toURL().readText()).total
            Thread.sleep(500)
            if (total < 10) "${leaf.id}=$total" else null
        }
        assertTrue("Categories with fewer than 10 results: $thin", thin.isEmpty())
    }
}
