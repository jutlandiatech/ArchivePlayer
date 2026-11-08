package com.jutlandiatech.archiveplayer.core.filter

import com.jutlandiatech.archiveplayer.core.catalog.MediaKind
import com.jutlandiatech.archiveplayer.core.catalog.SearchHit
import com.jutlandiatech.archiveplayer.core.search.ResultFilter
import com.jutlandiatech.archiveplayer.core.search.SearchRequest
import com.jutlandiatech.archiveplayer.core.support.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AdultFilterTest {
    private fun list(vararg k: String, schema: Int = 1) =
        """{"schemaVersion":$schema,"version":"1","keywords":[${k.joinToString(",") { "\"$it\"" }}]}"""

    private val filter = AdultFilter(KeywordListParser.parse(list("porn", "xxx", "nsfw", "erotic", "adult")))

    private fun hit(id: String, title: String = id, collections: List<String> = emptyList(), subjects: List<String> = emptyList()) =
        SearchHit(id, title, null, MediaKind.VIDEO, emptySet(), collections, subjects, 0, null)

    @Test fun R71_noPreviewCollectionIsAdult() = assertTrue(filter.isAdult(listOf("no-preview"), emptyList(), "Anything"))

    @Test fun R71_keywordInSubjectOrTitle() {
        assertTrue(filter.isAdult(emptyList(), listOf("XXX"), "Film"))
        assertTrue(filter.isAdult(emptyList(), emptyList(), "Erotic tales"))
        assertTrue(filter.isAdult(emptyList(), emptyList(), "NSFW comedy hour"))
    }

    @Test fun R71_wholeWordsOnly() {
        assertFalse(filter.isAdult(emptyList(), listOf("Adulthood studies"), "Growing up"))
        assertFalse(filter.isAdult(listOf("prelinger"), listOf("Agriculture"), "About Bananas"))
    }

    @Test fun R71_searchQueryExcludesNoPreview() = assertTrue(filter.queryExclusion.contains("-collection:no-preview"))

    @Test fun A27_adultHitsRemovedOnlyWhileHidden() {
        val hits = listOf(hit("ok"), hit("np", collections = listOf("no-preview")), hit("kw", title = "porn reel"))
        assertEquals(listOf("ok"), ResultFilter.apply(hits, SearchRequest("x"), filter).map { it.identifier })
        assertEquals(3, ResultFilter.apply(hits, SearchRequest("x"), adultFilter = null).size)
    }

    @Test fun A41_remoteKeywordAddedTakesEffect() {
        val chosen = KeywordListParser.choose(remote = list("porn", "smut"), cached = null, bundled = list("porn"))
        assertTrue(AdultFilter(chosen).isAdult(emptyList(), emptyList(), "Smut collection"))
    }

    @Test fun R71_invalidRemoteListFallsBack() {
        assertThrows(InvalidKeywordListException::class.java) { KeywordListParser.parse(list("x", schema = 9)) }
        assertEquals(listOf("porn"), KeywordListParser.choose(remote = "{bad", cached = null, bundled = list("porn")).keywords)
        assertEquals(listOf("xxx"), KeywordListParser.choose(remote = "{bad", cached = list("xxx"), bundled = list("porn")).keywords)
    }

    @Test fun R71_bundledListCoversFourLanguages() {
        val bundled = KeywordListParser.parse(Fixtures.appAsset("catalog/adult-keywords.json").readText())
        assertTrue(bundled.keywords.containsAll(listOf("porn", "xxx", "nsfw", "erotic", "erotik", "pornografi", "érotique")))
    }
}
