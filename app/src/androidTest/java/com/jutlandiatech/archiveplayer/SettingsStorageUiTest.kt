package com.jutlandiatech.archiveplayer

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.jutlandiatech.archiveplayer.core.settings.DownloadLocation
import com.jutlandiatech.archiveplayer.support.*
import com.jutlandiatech.archiveplayer.ui.ElementIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** UI_SETTINGS_SPEC.md S06: Storage & downloads (R61, R85, A43–A45). The SD card is faked with AppEnvironment.sdCard*. */
@RunWith(AndroidJUnit4::class)
class SettingsStorageUiTest {
    @get:Rule(order = 0) val server = FixtureServer(freeBytes = 10_000_000_000)
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val sd by lazy { File(context.cacheDir, "fake-sd").apply { deleteRecursively(); mkdirs() } }
    private val vm get() = ViewModelProvider(rule.activity)[AppViewModel::class.java]

    @Before fun setUp() {
        AppEnvironment.sdCardOverride = true
        AppEnvironment.sdCardDir = null
        rule.completeSetup()
    }

    private fun download(identifier: String) {
        rule.openItem(identifier)
        rule.tap(ElementIds.ITEM_DOWNLOAD_BUTTON)
        rule.waitUntil(90_000) { allText(ElementIds.ITEM_DOWNLOAD_BUTTON).contains("Downloaded") }
    }

    private fun openStorage() {
        rule.tap(ElementIds.NAV_HOME)
        rule.tap(ElementIds.NAV_HOME)
        rule.openSettingsRow("storage")
        rule.waitForTag(ElementIds.SETTINGS_STORAGE_LOCATION_INTERNAL)
    }

    /** Text of a node and all its descendants (location rows and the move prompt put their texts in child nodes). */
    private fun allText(tag: String): String {
        fun collect(n: androidx.compose.ui.semantics.SemanticsNode): List<String> =
            n.config.getOrElseNullable(androidx.compose.ui.semantics.SemanticsProperties.Text) { null }.orEmpty().map { it.text } +
                n.children.flatMap(::collect)
        return collect(rule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()).joinToString(" ")
    }

    private fun filesUnder(dir: File) = dir.walkTopDown().filter { it.isFile }.toList()

    private fun playable(identifier: String) =
        vm.downloads.records.filter { it.identifier == identifier }.all { vm.downloads.playableFile(it.identifier, it.fileName) != null }

    @Test fun S06_A43_A44_A45_locationMovePromptRemovedCardAndDeletes() {
        // No card: internal is selected, SD card is offered but disabled with its note.
        openStorage()
        rule.onNodeWithTag(ElementIds.SETTINGS_STORAGE_LOCATION_INTERNAL).assertIsSelected()
        rule.onNodeWithTag(ElementIds.SETTINGS_STORAGE_LOCATION_SD).assertIsNotEnabled()
        assertTrue(allText(ElementIds.SETTINGS_STORAGE_LOCATION_SD).contains("No SD card inserted"))
        rule.waitForTag(ElementIds.SETTINGS_STORAGE_SUMMARY)
        rule.waitForTag(ElementIds.SETTINGS_STORAGE_UNINSTALL_NOTE)

        download("fx_english")
        val internal = File(context.filesDir, "downloads")
        assertTrue(filesUnder(internal).isNotEmpty())

        // A43/A44: insert a card, choose it, accept the move prompt.
        AppEnvironment.sdCardDir = sd
        openStorage()
        rule.onNodeWithTag(ElementIds.SETTINGS_STORAGE_LOCATION_SD).assertIsEnabled()
        rule.tap(ElementIds.SETTINGS_STORAGE_LOCATION_SD)
        rule.waitForTag(ElementIds.SETTINGS_STORAGE_MOVE_PROMPT)
        assertTrue(allText(ElementIds.SETTINGS_STORAGE_MOVE_PROMPT).contains("SD card"))
        rule.tap(ElementIds.SETTINGS_STORAGE_MOVE_BUTTON)
        rule.waitForGone(ElementIds.SETTINGS_STORAGE_MOVE_PROMPT, 30_000)
        rule.waitUntil(30_000) { vm.downloads.records.all { it.location == DownloadLocation.SD_CARD } }
        assertTrue(filesUnder(File(sd, "downloads")).isNotEmpty())
        assertTrue(filesUnder(internal).isEmpty())
        assertTrue(playable("fx_english"))
        rule.onNodeWithTag(ElementIds.SETTINGS_STORAGE_LOCATION_SD).assertIsSelected()
        assertTrue(allText(ElementIds.settingsStorageItem("fx_english")).contains("SD card"))

        // A45: card removed: marked, streams, nothing deleted; new downloads go internal and the screen says so.
        AppEnvironment.sdCardDir = null
        openStorage()
        assertTrue(allText(ElementIds.settingsStorageItem("fx_english")).contains("SD card removed"))
        rule.waitForTag(ElementIds.SETTINGS_STORAGE_SD_MISSING)
        assertFalse(playable("fx_english"))
        assertTrue(filesUnder(File(sd, "downloads")).isNotEmpty())

        // Card back: offline again.
        AppEnvironment.sdCardDir = sd
        openStorage()
        assertFalse(rule.exists(ElementIds.SETTINGS_STORAGE_SD_MISSING))
        assertTrue(playable("fx_english"))

        // Switching back to internal and keeping: the download stays on the card.
        rule.tap(ElementIds.SETTINGS_STORAGE_LOCATION_INTERNAL)
        rule.waitForTag(ElementIds.SETTINGS_STORAGE_MOVE_PROMPT)
        rule.tap(ElementIds.SETTINGS_STORAGE_KEEP_BUTTON)
        rule.waitForGone(ElementIds.SETTINGS_STORAGE_MOVE_PROMPT)
        rule.onNodeWithTag(ElementIds.SETTINGS_STORAGE_LOCATION_INTERNAL).assertIsSelected()
        rule.onNodeWithTag(ElementIds.SETTINGS_STORAGE_LOCATION_SD).assertIsNotSelected()
        assertEquals(DownloadLocation.INTERNAL, vm.settings?.downloadLocation)
        assertTrue(vm.downloads.records.all { it.location == DownloadLocation.SD_CARD })

        // Delete one, then delete all.
        rule.tap(ElementIds.settingsStorageDelete("fx_english"))
        rule.waitForGone(ElementIds.settingsStorageItem("fx_english"))
        assertTrue(filesUnder(File(sd, "downloads")).isEmpty())
        download("fx_series_12")
        openStorage()
        rule.waitForTag(ElementIds.settingsStorageItem("fx_series_12"))
        rule.tap(ElementIds.SETTINGS_STORAGE_DELETE_ALL)
        rule.tap(ElementIds.DIALOG_CONFIRM_BUTTON)
        rule.waitForGone(ElementIds.settingsStorageItem("fx_series_12"))
        assertTrue(vm.downloads.records.isEmpty())
        assertTrue(filesUnder(internal).isEmpty())
    }

    @Test fun S06_R61_lowSpaceWarningPerLocation() {
        AppEnvironment.freeBytesOverride = 100_000_000
        openStorage()
        rule.waitForTag(ElementIds.SETTINGS_STORAGE_WARNING)
        AppEnvironment.freeBytesOverride = 10_000_000_000
        openStorage()
        assertFalse(rule.exists(ElementIds.SETTINGS_STORAGE_WARNING))
    }
}
