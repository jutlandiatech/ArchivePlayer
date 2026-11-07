package com.thomasnorlund.archiveplayer.core.category

import com.thomasnorlund.archiveplayer.core.support.Fixtures
import org.junit.Assert.assertTrue
import org.junit.Test

/** The category map bundled in the APK (R15, R17, R68). */
class BundledCategoryMapTest {
    private val map by lazy { CategoryMapParser.parse(Fixtures.appAsset("catalog/category-map.json").readText()) }

    @Test fun R17_initialMapContainsApprovedCategories() {
        val names = map.all().map { it.name("en") }.toSet()
        val expected = listOf("Video", "Audio", "Movies", "Comedy", "Drama", "Film Noir", "Sci-Fi & Horror", "Westerns",
            "Silent films", "Animation & cartoons", "TV", "1940s", "1950s", "1960s", "1970s", "1980s", "1990s",
            "Documentaries", "Newsreels & historical footage", "Music", "Rock", "Pop", "Jazz", "Classical", "Folk & World",
            "Hip-hop", "Electronic/Netlabels", "78rpm records", "Live concerts", "Old Time Radio", "Mystery & Suspense",
            "Sci-Fi", "Podcasts", "Audiobooks & spoken word")
        val missing = expected.filterNot { it in names }
        assertTrue("Missing: $missing", missing.isEmpty())
    }

    @Test fun R17_everyLeafHasAConcreteQuery() {
        val bad = map.all().filter { it.children.isEmpty() && (it.query == null || (it.query!!.collections + it.query!!.subjects).isEmpty() && it.query!!.extra.isNullOrBlank()) }
        assertTrue("Leaves without query: ${bad.map { it.id }}", bad.isEmpty())
    }

    @Test fun R68_everyCategoryTranslatedToGermanAndDanish() {
        val untranslated = map.all().filterNot { it.names.keys.containsAll(setOf("en", "de", "da")) }
        assertTrue("Untranslated: ${untranslated.map { it.id }}", untranslated.isEmpty())
    }
}
