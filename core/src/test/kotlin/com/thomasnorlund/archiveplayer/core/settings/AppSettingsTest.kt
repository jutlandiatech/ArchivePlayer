package com.thomasnorlund.archiveplayer.core.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    private val available = listOf("en", "de", "da")
    private val defaults = AppSettings.defaults("en-US", available)

    @Test fun A42_appLanguageChangeKeepsContentLanguages() {
        val s = defaults.withContentLanguages(setOf("en", "fr")).withAppLanguage("da")
        assertEquals("da", s.appLanguage)
        assertEquals(setOf("en", "fr"), s.contentLanguages)
    }

    @Test fun A42_contentLanguageChangeKeepsAppLanguage() {
        val s = defaults.withAppLanguage("de").withContentLanguages(setOf("ja"))
        assertEquals("de", s.appLanguage)
        assertEquals(setOf("ja"), s.contentLanguages)
    }

    @Test fun R27_contentLanguagesMayBeEmpty() {
        assertEquals(emptySet<String>(), defaults.withContentLanguages(emptySet()).contentLanguages)
    }

    @Test fun D30_defaultsComeFromPhoneNotAppLanguageChoice() {
        val s = AppSettings.defaults("es-ES", available)
        assertEquals("en", s.appLanguage)
        assertEquals(setOf("es", "en"), s.contentLanguages)
    }

    @Test fun R72_freshInstallDefaults() {
        assertEquals(ThemeMode.LIGHT, defaults.theme)                 // R63, A38
        assertTrue(defaults.dataSaver)                                // R41
        assertTrue(defaults.wifiOnly)                                 // R60
        assertFalse(defaults.showAdultContent)                        // R70
        assertEquals(DownloadLocation.INTERNAL, defaults.downloadLocation) // R85
        assertEquals(SubtitleDefault.OFF, defaults.subtitleDefault)   // R45
        assertTrue(defaults.includeAutoSubtitles)                     // R45, A40
        assertFalse(defaults.historyPaused)                           // R44
    }

    @Test fun A38_lightEvenWhenPhoneIsDark() {
        assertFalse(ThemeResolver.isDark(ThemeMode.LIGHT, systemDark = true))
        assertTrue(ThemeResolver.isDark(ThemeMode.NOIR, systemDark = false))
    }

    @Test fun R82_followSystem() {
        assertTrue(ThemeResolver.isDark(ThemeMode.SYSTEM, systemDark = true))
        assertFalse(ThemeResolver.isDark(ThemeMode.SYSTEM, systemDark = false))
    }

    @Test fun R82_toggleFlipsWhatIsShown() {
        assertEquals(ThemeMode.NOIR, ThemeResolver.toggle(ThemeMode.LIGHT, systemDark = true))
        assertEquals(ThemeMode.LIGHT, ThemeResolver.toggle(ThemeMode.NOIR, systemDark = false))
        assertEquals(ThemeMode.LIGHT, ThemeResolver.toggle(ThemeMode.SYSTEM, systemDark = true))
        assertEquals(ThemeMode.NOIR, ThemeResolver.toggle(ThemeMode.SYSTEM, systemDark = false))
    }
}
