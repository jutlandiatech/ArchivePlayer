package com.thomasnorlund.archiveplayer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thomasnorlund.archiveplayer.support.completeSetup
import com.thomasnorlund.archiveplayer.ui.ElementIds
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A37 / N13: the `foss` build has no Cast button or indicator anywhere. */
@RunWith(AndroidJUnit4::class)
class NoCastFossUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun A37_N13_noCastElements() {
        rule.completeSetup()
        rule.onNodeWithTag(ElementIds.HOME_SCREEN).assertIsDisplayed()
        rule.onNodeWithTag(ElementIds.TOP_BAR_CAST_BUTTON).assertDoesNotExist()
        rule.onNodeWithTag(ElementIds.MINI_PLAYER_CAST_INDICATOR).assertDoesNotExist()
        rule.onNodeWithTag(ElementIds.PLAYER_CAST_BUTTON).assertDoesNotExist()
    }
}
