package com.thomasnorlund.archiveplayer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thomasnorlund.archiveplayer.support.catalog
import com.thomasnorlund.archiveplayer.ui.ElementIds
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI_SETUP_SPEC.md scenarios U01–U07 by element ID; the emulator runs with phone language en-US. */
@RunWith(AndroidJUnit4::class)
class SetupUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val eng by lazy { catalog("ENG") }

    private fun node(tag: String) = rule.onNodeWithTag(tag)

    @Test fun U01_firstLaunchShowsLanguageStepWithPreselection() {
        node(ElementIds.SETUP_ROOT).assertIsDisplayed()
        node(ElementIds.SETUP_LANGUAGE_SCREEN).assertIsDisplayed()
        node(ElementIds.SETUP_PROGRESS).assertTextEquals(eng.text("setup.progress", mapOf("current" to 1, "total" to 3)))
        node(ElementIds.setupLanguage("en")).assertIsSelected()
        node(ElementIds.setupLanguage("de")).assertIsNotSelected()
        node(ElementIds.setupLanguage("da")).assertIsNotSelected()
        node(ElementIds.SETUP_BACK_BUTTON).assertDoesNotExist()
    }

    @Test fun U02_selectingLanguageSwitchesTextImmediately() {
        node(ElementIds.setupLanguage("de")).performClick()
        node(ElementIds.setupLanguage("de")).assertIsSelected()
        node(ElementIds.SETUP_LANGUAGE_TITLE).assertTextEquals(catalog("GER").text("setup.language.title"))
        node(ElementIds.setupLanguage("da")).performClick()
        node(ElementIds.SETUP_LANGUAGE_TITLE).assertTextEquals(catalog("DEN").text("setup.language.title"))
        node(ElementIds.SETUP_LANGUAGE_SCREEN).assertIsDisplayed()
    }

    @Test fun U03_continueThroughStepsOneRootAtATime() {
        node(ElementIds.SETUP_CONTINUE_BUTTON).performClick()
        node(ElementIds.SETUP_CONTENT_LANGUAGES_SCREEN).assertIsDisplayed()
        node(ElementIds.SETUP_LANGUAGE_SCREEN).assertDoesNotExist()
        node(ElementIds.SETUP_BACK_BUTTON).assertIsDisplayed()
        node(ElementIds.SETUP_PROGRESS).assertTextEquals(eng.text("setup.progress", mapOf("current" to 2, "total" to 3)))
        node(ElementIds.SETUP_CONTINUE_BUTTON).performClick()
        node(ElementIds.SETUP_READY_SCREEN).assertIsDisplayed()
        node(ElementIds.SETUP_READY_PRIVACY).assertTextEquals(eng.text("setup.ready.privacy"))
        node(ElementIds.SETUP_READY_ADULT_CONTENT).assertTextEquals(eng.text("setup.ready.adultContent"))
        node(ElementIds.SETUP_START_BUTTON).assertIsDisplayed()
    }

    @Test fun U05_A42_contentLanguagesIndependentOfAppLanguage() {
        node(ElementIds.SETUP_CONTINUE_BUTTON).performClick()
        node(ElementIds.setupContentLanguage("en")).assertIsOn()
        node(ElementIds.setupContentLanguage("fr")).assertIsOff()
        node(ElementIds.setupContentLanguage("fr")).performClick()
        node(ElementIds.setupContentLanguage("fr")).assertIsOn()
        node(ElementIds.SETUP_BACK_BUTTON).performClick()
        node(ElementIds.setupLanguage("da")).performClick()
        node(ElementIds.SETUP_CONTINUE_BUTTON).performClick()
        node(ElementIds.setupContentLanguage("en")).assertIsOn()
        node(ElementIds.setupContentLanguage("fr")).assertIsOn()
        node(ElementIds.setupContentLanguage("da")).assertIsOff()
    }

    @Test fun U06_recreationKeepsStepAndSelections() {
        node(ElementIds.SETUP_CONTINUE_BUTTON).performClick()
        node(ElementIds.setupContentLanguage("ja")).performClick()
        rule.activityRule.scenario.recreate()
        node(ElementIds.SETUP_CONTENT_LANGUAGES_SCREEN).assertIsDisplayed()
        node(ElementIds.setupContentLanguage("ja")).assertIsOn()
    }

    @Test fun U07_startOpensHomeAndRelaunchSkipsSetup() {
        node(ElementIds.SETUP_CONTINUE_BUTTON).performClick()
        node(ElementIds.SETUP_CONTINUE_BUTTON).performClick()
        node(ElementIds.SETUP_START_BUTTON).performClick()
        node(ElementIds.APP_ROOT).assertIsDisplayed()
        node(ElementIds.HOME_SCREEN).assertIsDisplayed()
        rule.activityRule.scenario.recreate()
        node(ElementIds.HOME_SCREEN).assertIsDisplayed()
        node(ElementIds.SETUP_ROOT).assertDoesNotExist()
    }
}
