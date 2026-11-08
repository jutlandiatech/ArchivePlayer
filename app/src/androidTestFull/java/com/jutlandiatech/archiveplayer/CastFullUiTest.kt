package com.jutlandiatech.archiveplayer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jutlandiatech.archiveplayer.support.completeSetup
import com.jutlandiatech.archiveplayer.support.openLibrary
import com.jutlandiatech.archiveplayer.support.play
import com.jutlandiatech.archiveplayer.support.tap
import com.jutlandiatech.archiveplayer.support.waitForTag
import com.jutlandiatech.archiveplayer.ui.ElementIds
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** R46: the `full` build shows the Cast button in the top bar. */
@RunWith(AndroidJUnit4::class)
class CastFullUiTest {
    @get:Rule(order = 0) val server = com.jutlandiatech.archiveplayer.support.FixtureServer()
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    @Test fun R46_castButtonInTopBar() {
        rule.completeSetup()
        rule.onNodeWithTag(ElementIds.TOP_BAR_CAST_BUTTON).assertIsDisplayed()
    }

    @Test fun P19_playerCastButton() {
        rule.completeSetup()
        rule.play("fx_radio_show")
        rule.tap(ElementIds.MINI_PLAYER)
        rule.waitForTag(ElementIds.PLAYER_SCREEN)
        rule.onNodeWithTag(ElementIds.PLAYER_CAST_BUTTON).assertIsDisplayed()
        rule.onNodeWithTag(ElementIds.PLAYER_CASTING_LABEL).assertDoesNotExist()
    }

    @Test fun L17_playlistCastButton() {
        rule.completeSetup()
        rule.openLibrary()
        rule.tap(ElementIds.libraryPlaylist(ElementIds.LIKED_PLAYLIST_ID))
        rule.waitForTag(ElementIds.PLAYLIST_SCREEN)
        rule.onNodeWithTag(ElementIds.PLAYLIST_CAST_BUTTON).assertIsDisplayed()
    }
}
