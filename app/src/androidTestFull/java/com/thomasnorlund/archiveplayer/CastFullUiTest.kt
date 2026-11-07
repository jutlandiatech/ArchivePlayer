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

/** R46: the `full` build shows the Cast button in the top bar. */
@RunWith(AndroidJUnit4::class)
class CastFullUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun R46_castButtonInTopBar() {
        rule.completeSetup()
        rule.onNodeWithTag(ElementIds.TOP_BAR_CAST_BUTTON).assertIsDisplayed()
    }
}
