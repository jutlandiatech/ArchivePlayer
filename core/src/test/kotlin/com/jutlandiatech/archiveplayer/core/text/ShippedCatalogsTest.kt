package com.jutlandiatech.archiveplayer.core.text

import com.jutlandiatech.archiveplayer.core.support.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks the real catalog files in app/src/main/assets/texts (R65, R66, A04, UI_SETUP_SPEC contract). */
class ShippedCatalogsTest {
    private val dir = Fixtures.appAsset("texts")

    private fun load(): Map<String, TextCatalog> =
        (dir.listFiles() ?: emptyArray()).mapNotNull { f ->
            TextCatalog.codeFromFileName(f.name)?.let { code -> code to TextCatalog.parse(code, f.readText()) }
        }.toMap().also { assertEquals("Catalogs in $dir", setOf("ENG", "GER", "DEN"), it.keys) }

    @Test fun R65_englishGermanDanishCatalogsShipped() {
        val catalogs = load()
        assertEquals(setOf("ENG", "GER", "DEN"), catalogs.keys)
        assertEquals(mapOf("ENG" to "en", "GER" to "de", "DEN" to "da"), catalogs.mapValues { it.value.isoCode })
        assertEquals(mapOf("ENG" to "English", "GER" to "Deutsch", "DEN" to "Dansk"), catalogs.mapValues { it.value.selfName })
    }

    @Test fun R66_A04_allCatalogsMatchEnglishReference() {
        val catalogs = load()
        val eng = catalogs.getValue("ENG")
        catalogs.values.forEach { c -> assertEquals("${c.fileCode}: ", emptyList<String>(), CatalogParity.problems(eng, c)) }
    }

    @Test fun U_setupTextContractInEnglish() {
        val eng = load().getValue("ENG")
        val expected = mapOf(
            "app.name" to "ArchivePlayer",
            "setup.language.title" to "Choose your language",
            "setup.language.hint" to "You can change this later in Settings.",
            "setup.contentLanguages.title" to "Which languages do you watch and listen in?",
            "setup.contentLanguages.more" to "More languages",
            "setup.ready.title" to "You're all set",
            "setup.ready.privacy" to "No account, no tracking. Your playlists and history stay on your phone.",
            "setup.ready.adultContent" to "Adult content is hidden. You can change this in Settings.",
            "action.continue" to "Continue",
            "action.back" to "Back",
            "action.start" to "Start",
        )
        expected.forEach { (k, v) -> assertEquals(k, v, eng.text(k)) }
        assertEquals("Step 2 of 3", eng.text("setup.progress", mapOf("current" to 2, "total" to 3)))
    }

    @Test fun R72_settingsRowLabelsExistInAllCatalogs() {
        val rows = listOf("app_language", "appearance", "content_languages", "data_saver", "wifi_only", "adult_content",
            "storage", "subtitles", "history", "search_history", "export_import", "about")
        load().values.forEach { c ->
            rows.forEach { r -> assertTrue("${c.fileCode} settings.row.$r", c.texts["settings.row.$r"].orEmpty().isNotBlank()) }
        }
    }

    @Test fun R27_appLanguageAndContentLanguagesHaveDistinctLabels() {
        load().values.forEach { c ->
            assertTrue(c.fileCode, c.text("settings.row.app_language") != c.text("settings.row.content_languages"))
        }
    }
}
