package com.thomasnorlund.archiveplayer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thomasnorlund.archiveplayer.support.completeSetup
import com.thomasnorlund.archiveplayer.support.stateDescriptionOf
import com.thomasnorlund.archiveplayer.ui.ElementIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** UI_SHELL_SPEC.md scenarios N01, N10 (phone size) by element ID. */
@RunWith(AndroidJUnit4::class)
class ShellUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before fun setUp() = rule.completeSetup()

    private fun node(tag: String) = rule.onNodeWithTag(tag)

    @Test fun N01_bottomBarWithThreeDestinations() {
        node(ElementIds.NAV_BAR).assertIsDisplayed()
        node(ElementIds.NAV_HOME).assertIsSelected()
        node(ElementIds.TOP_BAR_THEME_TOGGLE).assertIsDisplayed()
        node(ElementIds.TOP_BAR_SETTINGS_BUTTON).assertIsDisplayed()
        node(ElementIds.NAV_SEARCH).performClick()
        node(ElementIds.SEARCH_SCREEN).assertIsDisplayed()
        node(ElementIds.NAV_SEARCH).assertIsSelected()
        node(ElementIds.NAV_LIBRARY).performClick()
        node(ElementIds.LIBRARY_SCREEN).assertIsDisplayed()
        node(ElementIds.NAV_HOME).performClick()
        node(ElementIds.HOME_SCREEN).assertIsDisplayed()
    }

    @Test fun N10_A38_themeToggleSwitchesAndPersists() {
        val initial = rule.stateDescriptionOf(ElementIds.TOP_BAR_THEME_TOGGLE)
        assertNotNull(initial)
        node(ElementIds.TOP_BAR_THEME_TOGGLE).performClick()
        val toggled = rule.stateDescriptionOf(ElementIds.TOP_BAR_THEME_TOGGLE)
        assertNotEquals(initial, toggled)
        rule.activityRule.scenario.recreate()
        assertEquals(toggled, rule.stateDescriptionOf(ElementIds.TOP_BAR_THEME_TOGGLE))
    }
}
