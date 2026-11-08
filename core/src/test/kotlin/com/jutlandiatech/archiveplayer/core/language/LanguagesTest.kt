package com.jutlandiatech.archiveplayer.core.language

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguagesTest {
    private val available = listOf("en", "de", "da")

    @Test fun A05_frenchVariantsNormalizeToFr() {
        listOf("fre", "fra", "fr", "FR", "French", "french ", "Français", "francais").forEach {
            assertEquals(it, "fr", LanguageNormalizer.normalize(it))
        }
    }

    @Test fun R13_commonLanguagesNormalize() {
        mapOf("eng" to "en", "English" to "en", "ger" to "de", "deu" to "de", "German" to "de", "Deutsch" to "de",
            "dan" to "da", "Danish" to "da", "Dansk" to "da", "jpn" to "ja", "Japanese" to "ja", "spa" to "es",
            "Spanish" to "es", "swe" to "sv", "nor" to "no", "dut" to "nl", "Dutch" to "nl", "ita" to "it")
            .forEach { (raw, iso) -> assertEquals(raw, iso, LanguageNormalizer.normalize(raw)) }
    }

    @Test fun R13_unmappedValuesAreIgnored() {
        assertNull(LanguageNormalizer.normalize("Klingon"))
        assertNull(LanguageNormalizer.normalize(""))
        assertEquals(setOf("en", "fr"), LanguageNormalizer.normalizeAll(listOf("English; French", "Unknown", "eng")))
        assertEquals(emptySet<String>(), LanguageNormalizer.normalizeAll(emptyList()))
    }

    @Test fun R13_tableCoversChipsAndMoreLanguages() {
        assertTrue(LanguageNormalizer.knownCodes.containsAll(LanguagePreselection.directContentChips))
        assertTrue(LanguageNormalizer.knownCodes.size >= 30)
    }

    @Test fun R83_nativeNames() {
        assertEquals("Dansk", LanguageNormalizer.nativeName("da"))
        assertEquals("Deutsch", LanguageNormalizer.nativeName("de"))
        assertEquals("English", LanguageNormalizer.nativeName("en"))
        assertEquals("Français", LanguageNormalizer.nativeName("fr"))
        assertEquals("日本語", LanguageNormalizer.nativeName("ja"))
    }

    @Test fun A06_untaggedItemExcludedWhileFiltering() {
        assertFalse(LanguageFilter.matches(emptySet(), setOf("fr")))
    }

    @Test fun A07_untaggedItemShownWithoutFilter() {
        assertTrue(LanguageFilter.matches(emptySet(), emptySet()))
    }

    @Test fun R28_matchesAnyFilterLanguage() {
        assertTrue(LanguageFilter.matches(setOf("fr", "en"), setOf("fr")))
        assertTrue(LanguageFilter.matches(setOf("de"), setOf("fr", "de")))
        assertFalse(LanguageFilter.matches(setOf("en"), setOf("fr")))
    }

    @Test fun A01_germanPhonePreselectsDeutsch() {
        assertEquals("de", LanguagePreselection.appLanguage("de-DE", available))
    }

    @Test fun A02_unsupportedPhoneLanguageFallsBackToEnglish() {
        assertEquals("en", LanguagePreselection.appLanguage("es-ES", available))
        assertEquals("en", LanguagePreselection.appLanguage("nb-NO", available))
    }

    @Test fun U01_supportedLanguagesPreselected() {
        assertEquals("da", LanguagePreselection.appLanguage("da", available))
        assertEquals("da", LanguagePreselection.appLanguage("da-DK", available))
        assertEquals("en", LanguagePreselection.appLanguage("en-US", available))
    }

    @Test fun SO2_directContentChips() {
        assertEquals(listOf("en", "de", "da", "fr", "es", "it", "sv", "no", "nl", "ja"), LanguagePreselection.directContentChips)
    }

    @Test fun SO1_contentLanguagesFromPhonePlusEnglish() {
        assertEquals(setOf("de", "en"), LanguagePreselection.contentLanguages("de-DE"))
        assertEquals(setOf("es", "en"), LanguagePreselection.contentLanguages("es-ES"))
        assertEquals(setOf("en"), LanguagePreselection.contentLanguages("en-GB"))
        assertEquals(setOf("en"), LanguagePreselection.contentLanguages("xx-YY"))
    }
}
