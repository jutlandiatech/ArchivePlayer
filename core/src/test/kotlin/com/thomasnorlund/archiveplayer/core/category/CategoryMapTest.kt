package com.thomasnorlund.archiveplayer.core.category

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CategoryMapTest {
    private fun map(schema: Int = 1, extra: String = "") = """{"schemaVersion":$schema,"version":"2026-11-07","categories":[
        {"id":"video","names":{"en":"Video","de":"Video","da":"Video"},"kind":"video","children":[
          {"id":"video.movies.noir","names":{"en":"Film Noir","de":"Film noir"},"kind":"video",
           "query":{"collections":["Film_Noir"],"subjects":["film noir"]}}$extra]}]}"""

    @Test fun R14_parsesCategoriesWithQueries() {
        val m = CategoryMapParser.parse(map())
        assertEquals(1, m.schemaVersion)
        assertEquals(2, m.all().size)
        val noir = m.find("video.movies.noir")!!
        assertEquals(listOf("Film_Noir"), noir.query!!.collections)
        assertEquals(listOf("film noir"), noir.query!!.subjects)
    }

    @Test fun R68_localizedNamesFallBackToEnglish() {
        val noir = CategoryMapParser.parse(map()).find("video.movies.noir")!!
        assertEquals("Film noir", noir.name("de"))
        assertEquals("Film Noir", noir.name("da"))
    }

    @Test fun R15_invalidMapsRejected() {
        assertThrows(InvalidCategoryMapException::class.java) { CategoryMapParser.parse(map(schema = 2)) }
        assertThrows(InvalidCategoryMapException::class.java) { CategoryMapParser.parse("{broken") }
        assertThrows(InvalidCategoryMapException::class.java) {
            CategoryMapParser.parse(map(extra = """,{"id":"video.movies.noir","names":{"en":"Dup"},"query":{"collections":["x"]}}"""))
        }
        assertThrows(InvalidCategoryMapException::class.java) {
            CategoryMapParser.parse(map(extra = """,{"id":"nameless","names":{"de":"Nur Deutsch"},"query":{"collections":["x"]}}"""))
        }
        assertThrows(InvalidCategoryMapException::class.java) {
            CategoryMapParser.parse(map(extra = """,{"id":"leaf.without.query","names":{"en":"Empty"}}"""))
        }
    }

    @Test fun A08_unreachableRemoteUsesBundled() {
        assertEquals(MapSource.BUNDLED, CategoryMapSelector.choose(remote = null, cached = null, bundled = map()).source)
    }

    @Test fun R15_validRemoteWins() {
        assertEquals(MapSource.REMOTE, CategoryMapSelector.choose(remote = map(), cached = map(), bundled = map()).source)
    }

    @Test fun A09_unsupportedOrInvalidRemoteFallsBack() {
        assertEquals(MapSource.CACHED, CategoryMapSelector.choose(remote = map(schema = 2), cached = map(), bundled = map()).source)
        assertEquals(MapSource.BUNDLED, CategoryMapSelector.choose(remote = "{bad", cached = "{bad", bundled = map()).source)
    }
}
