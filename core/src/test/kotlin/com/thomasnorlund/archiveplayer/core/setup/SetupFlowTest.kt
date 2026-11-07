package com.thomasnorlund.archiveplayer.core.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupFlowTest {
    private val available = listOf("en", "de", "da")

    @Test fun U01_startsOnLanguageStepWithPreselection() {
        val flow = SetupFlow("es-ES", available)
        assertEquals(SetupStep.LANGUAGE, flow.step)
        assertEquals("en", flow.appLanguage)
        assertEquals(1, flow.stepNumber)
        assertEquals(3, flow.stepCount)
        assertFalse(flow.showsBack)
        assertEquals("de", SetupFlow("de-AT", available).appLanguage)
    }

    @Test fun U02_selectingLanguageKeepsStep() {
        val flow = SetupFlow("en-US", available)
        flow.selectAppLanguage("da")
        assertEquals("da", flow.appLanguage)
        assertEquals(SetupStep.LANGUAGE, flow.step)
    }

    @Test fun U02_unknownLanguageRejected() {
        assertThrows(IllegalArgumentException::class.java) { SetupFlow("en-US", available).selectAppLanguage("fr") }
    }

    @Test fun U03_continueThroughSteps() {
        val flow = SetupFlow("en-US", available)
        flow.next()
        assertEquals(SetupStep.CONTENT_LANGUAGES, flow.step)
        assertEquals(2, flow.stepNumber)
        assertTrue(flow.showsBack)
        flow.next()
        assertEquals(SetupStep.READY, flow.step)
        assertEquals(3, flow.stepNumber)
        assertTrue(flow.showsBack)
    }

    @Test fun U04_backKeepsSelectionsAndLeavesOnFirstStep() {
        val flow = SetupFlow("de-DE", available)
        flow.selectAppLanguage("da")
        flow.next()
        flow.toggleContentLanguage("fr")
        flow.next()
        assertTrue(flow.back())
        assertEquals(SetupStep.CONTENT_LANGUAGES, flow.step)
        assertTrue("fr" in flow.contentLanguages)
        assertTrue(flow.back())
        assertEquals("da", flow.appLanguage)
        assertFalse(flow.back())
        assertFalse(flow.isCompleted)
    }

    @Test fun U05_contentLanguagesIndependentOfAppLanguage() {
        val flow = SetupFlow("de-DE", available)
        assertEquals(setOf("de", "en"), flow.contentLanguages)
        flow.selectAppLanguage("da")
        assertEquals(setOf("de", "en"), flow.contentLanguages)
        flow.next()
        flow.toggleContentLanguage("fr")
        assertEquals(setOf("de", "en", "fr"), flow.contentLanguages)
        assertEquals("da", flow.appLanguage)
        listOf("de", "en", "fr").forEach(flow::toggleContentLanguage)
        assertEquals(emptySet<String>(), flow.contentLanguages)
    }

    @Test fun U07_startCompletesOnceAndReturnsSettings() {
        val flow = SetupFlow("da-DK", available)
        flow.next(); flow.next()
        val settings = flow.start()
        assertNotNull(settings)
        assertEquals("da", settings!!.appLanguage)
        assertEquals(setOf("da", "en"), settings.contentLanguages)
        assertTrue(flow.isCompleted)
        assertNull(flow.start())
    }

    @Test fun U07_startOnlyOnReadyStep() {
        assertThrows(IllegalStateException::class.java) { SetupFlow("en-US", available).start() }
    }
}
