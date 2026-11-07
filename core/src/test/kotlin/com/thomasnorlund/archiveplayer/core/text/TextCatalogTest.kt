package com.thomasnorlund.archiveplayer.core.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class TextCatalogTest {
    private val eng = """{"language.self":"English","language.code":"en","app.name":"ArchivePlayer",
        "setup.progress":"Step {current} of {total}","action.start":"Start"}"""

    private fun catalog(code: String, json: String) = TextCatalog.parse(code, json)

    @Test fun R65_resolvesTextWithNamedPlaceholders() {
        assertEquals("Step 1 of 3", catalog("ENG", eng).text("setup.progress", mapOf("current" to 1, "total" to 3)))
    }

    @Test fun R65_plainTextWithoutArguments() {
        assertEquals("Start", catalog("ENG", eng).text("action.start"))
    }

    @Test fun R65_missingPlaceholderArgumentThrows() {
        assertThrows(PlaceholderException::class.java) { catalog("ENG", eng).text("setup.progress", mapOf("current" to 1)) }
    }

    @Test fun R65_unexpectedArgumentThrows() {
        assertThrows(PlaceholderException::class.java) { catalog("ENG", eng).text("action.start", mapOf("x" to 1)) }
    }

    @Test fun R65_unknownKeyThrows() {
        assertThrows(MissingTextException::class.java) { catalog("ENG", eng).text("no.such.key") }
    }

    @Test fun R66_catalogNamesItself() {
        val den = catalog("DEN", """{"language.self":"Dansk","language.code":"da"}""")
        assertEquals("Dansk", den.selfName)
        assertEquals("da", den.isoCode)
        assertEquals("DEN", den.fileCode)
    }

    @Test fun R66_A03_codeFromFileName() {
        assertEquals("DEN", TextCatalog.codeFromFileName("TextCatalog_DEN.json"))
        assertEquals("FRA", TextCatalog.codeFromFileName("TextCatalog_FRA.json"))
        assertNull(TextCatalog.codeFromFileName("TextCatalog_.json"))
        assertNull(TextCatalog.codeFromFileName("notes.txt"))
        assertNull(TextCatalog.codeFromFileName("TextCatalog_DEN.json.bak"))
    }

    @Test fun R66_invalidCatalogsRejected() {
        assertThrows(InvalidCatalogException::class.java) { catalog("ENG", "{not json") }
        assertThrows(InvalidCatalogException::class.java) { catalog("ENG", """{"language.self":"English","language.code":"en","a":1}""") }
        assertThrows(InvalidCatalogException::class.java) { catalog("ENG", """{"language.code":"en"}""") }
        assertThrows(InvalidCatalogException::class.java) { catalog("ENG", """{"language.self":"English"}""") }
    }

    @Test fun A04_missingKeyIsReported() {
        val ref = catalog("ENG", eng)
        val ger = catalog("GER", """{"language.self":"Deutsch","language.code":"de","app.name":"ArchivePlayer",
            "setup.progress":"Schritt {current} von {total}"}""")
        val problems = CatalogParity.problems(ref, ger)
        assertTrue(problems.toString(), problems.any { "action.start" in it })
    }

    @Test fun R66_extraKeyBlankValueAndPlaceholderMismatchReported() {
        val ref = catalog("ENG", eng)
        val bad = catalog("GER", """{"language.self":"Deutsch","language.code":"de","app.name":" ",
            "setup.progress":"Schritt {current}","action.start":"Start","extra.key":"x"}""")
        val problems = CatalogParity.problems(ref, bad)
        assertTrue(problems.toString(), problems.any { "extra.key" in it })
        assertTrue(problems.toString(), problems.any { "app.name" in it })
        assertTrue(problems.toString(), problems.any { "setup.progress" in it })
    }

    @Test fun R66_consistentCatalogHasNoProblems() {
        val ger = catalog("GER", """{"language.self":"Deutsch","language.code":"de","app.name":"ArchivePlayer",
            "setup.progress":"Schritt {current} von {total}","action.start":"Start"}""")
        assertEquals(emptyList<String>(), CatalogParity.problems(catalog("ENG", eng), ger))
    }
}
