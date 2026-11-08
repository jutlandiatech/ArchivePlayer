package com.jutlandiatech.archiveplayer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jutlandiatech.archiveplayer.support.catalog
import com.jutlandiatech.archiveplayer.support.completeSetup
import com.jutlandiatech.archiveplayer.ui.ElementIds
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI_SETTINGS_SPEC.md scenarios S01–S03, S10 by element ID. */
@RunWith(AndroidJUnit4::class)
class SettingsUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun setUp() = rule.completeSetup()

    private fun node(tag: String) = rule.onNodeWithTag(tag)

    private fun pressBack() {
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        rule.waitForIdle()
    }

    @Test fun S01_settingsFromEveryTabRootWithAllRows() {
        listOf(ElementIds.NAV_HOME, ElementIds.NAV_SEARCH, ElementIds.NAV_LIBRARY).forEach { tab ->
            node(tab).performClick()
            node(ElementIds.TOP_BAR_SETTINGS_BUTTON).performClick()
            node(ElementIds.SETTINGS_SCREEN).assertIsDisplayed()
            ElementIds.SETTINGS_ROWS.forEach { node(ElementIds.settingsRow(it)).performScrollTo().assertIsDisplayed() }
            node(ElementIds.SETTINGS_BACK_BUTTON).performClick()
            node(tab).assertIsSelected()
        }
    }

    @Test fun S02_detailAndAndroidBackReturnToList() {
        node(ElementIds.TOP_BAR_SETTINGS_BUTTON).performClick()
        node(ElementIds.settingsRow("appearance")).performClick()
        node(ElementIds.settingsAppearance("light")).assertIsSelected()
        pressBack()
        node(ElementIds.SETTINGS_SCREEN).assertIsDisplayed()
        pressBack()
        node(ElementIds.HOME_SCREEN).assertIsDisplayed()
    }

    @Test fun S03_A42_appLanguageChangeKeepsContentLanguages() {
        node(ElementIds.TOP_BAR_SETTINGS_BUTTON).performClick()
        node(ElementIds.settingsRow("app_language")).performClick()
        node(ElementIds.settingsLanguage("da")).performClick()
        node(ElementIds.settingsLanguage("da")).assertIsSelected()
        node(ElementIds.SETTINGS_BACK_BUTTON).performClick()
        node(ElementIds.settingsRow("app_language")).assertTextContains(catalog("DEN").text("settings.row.app_language"), substring = true)
        node(ElementIds.settingsRow("content_languages")).performScrollTo().performClick()
        node(ElementIds.settingsContentLanguage("en")).assertIsOn()
        node(ElementIds.settingsContentLanguage("da")).assertIsOff()
    }

    @Test fun S10_aboutShowsFlavour() {
        node(ElementIds.TOP_BAR_SETTINGS_BUTTON).performClick()
        node(ElementIds.settingsRow("about")).performScrollTo().performClick()
        node(ElementIds.SETTINGS_ABOUT_FLAVOUR).assertTextContains(BuildConfig.FLAVOR, substring = true)
        node(ElementIds.SETTINGS_ABOUT_NOTICES).assertIsDisplayed()
        node(ElementIds.SETTINGS_ABOUT_SOURCE).assertIsDisplayed()
    }
}
