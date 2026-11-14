package com.jutlandiatech.archiveplayer

import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.compose.ui.semantics.SemanticsProperties
import com.jutlandiatech.archiveplayer.support.FixtureServer
import com.jutlandiatech.archiveplayer.support.PlayerProbe
import com.jutlandiatech.archiveplayer.support.*
import com.jutlandiatech.archiveplayer.ui.ElementIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/** UI_PLAYER_SPEC.md scenarios P01–P18, P20 by element ID (P19 is in the flavour test sets). */
@RunWith(AndroidJUnit4::class)
class PlayerUiTest {
    @get:Rule(order = 0) val server = FixtureServer()
    @get:Rule(order = 1) val rule = createAndroidComposeRule<MainActivity>()

    @Before fun setUp() = rule.completeSetup()

    private fun openPlayer(identifier: String = "fx_radio_show", entry: Int = 0): PlayerProbe {
        rule.play(identifier, entry)
        rule.tap(ElementIds.MINI_PLAYER)
        rule.waitForTag(ElementIds.PLAYER_SCREEN)
        return PlayerProbe.connect().also { assertTrue(it.await { isPlaying }) }
    }

    private fun state() = rule.stateDescriptionOf(ElementIds.PLAYER_STATE)

    @Test fun P01_miniPlayerOnAllTabsOpensFullPlayer() {
        rule.play("fx_radio_show")
        listOf(ElementIds.NAV_HOME, ElementIds.NAV_SEARCH, ElementIds.NAV_LIBRARY).forEach {
            rule.tap(it); rule.waitForTag(ElementIds.MINI_PLAYER)
        }
        assertTrue(rule.textOf(ElementIds.MINI_PLAYER_TITLE).contains("Episode 01"))
        rule.tap(ElementIds.MINI_PLAYER)
        rule.waitForTag(ElementIds.PLAYER_ARTWORK)
        rule.tap(ElementIds.PLAYER_COLLAPSE_BUTTON)
        rule.waitForTag(ElementIds.LIBRARY_SCREEN)
    }

    @Test fun P02_playPauseOnMiniAndFullPlayer() {
        openPlayer().use { p ->
            rule.waitUntil { state() == "playing" }
            rule.tap(ElementIds.PLAYER_PLAY_PAUSE_BUTTON)
            rule.waitUntil { state() == "paused" }
            assertFalse(p.isPlaying)
            rule.tap(ElementIds.PLAYER_COLLAPSE_BUTTON)
            rule.tap(ElementIds.MINI_PLAYER_PLAY_PAUSE_BUTTON)
            assertTrue(p.await { isPlaying })
        }
    }

    @Test fun P03_skipForward30Back10() {
        openPlayer().use { p ->
            p.pause(); p.seekTo(60_000); p.await { abs(positionMs - 60_000) < 500 }
            rule.tap(ElementIds.PLAYER_SKIP_FORWARD_BUTTON)
            assertTrue(p.await { abs(positionMs - 90_000) < 1_000 })
            rule.tap(ElementIds.PLAYER_SKIP_BACK_BUTTON)
            assertTrue(p.await { abs(positionMs - 80_000) < 1_000 })
        }
    }

    @Test fun P04_nextPreviousAndRestart() {
        openPlayer().use { p ->
            rule.tap(ElementIds.PLAYER_NEXT_BUTTON)
            assertTrue(p.await { currentIndex == 1 })
            p.seekTo(10_000); p.await { positionMs >= 10_000 }
            rule.tap(ElementIds.PLAYER_PREVIOUS_BUTTON)
            assertTrue(p.await { currentIndex == 1 && positionMs < 3_000 })
            rule.tap(ElementIds.PLAYER_PREVIOUS_BUTTON)
            assertTrue(p.await { currentIndex == 0 })
        }
    }

    @Test fun P05_repeatCyclesShuffleToggles() {
        openPlayer().close()
        assertEquals("off", rule.stateDescriptionOf(ElementIds.PLAYER_REPEAT_BUTTON))
        listOf("all", "one", "off").forEach {
            rule.tap(ElementIds.PLAYER_REPEAT_BUTTON)
            assertEquals(it, rule.stateDescriptionOf(ElementIds.PLAYER_REPEAT_BUTTON))
        }
        assertEquals("off", rule.stateDescriptionOf(ElementIds.PLAYER_SHUFFLE_BUTTON))
        rule.tap(ElementIds.PLAYER_SHUFFLE_BUTTON)
        assertEquals("on", rule.stateDescriptionOf(ElementIds.PLAYER_SHUFFLE_BUTTON))
    }

    @Test fun P06_speedSheet() {
        openPlayer().use { p ->
            rule.tap(ElementIds.PLAYER_SPEED_BUTTON)
            rule.waitForTag(ElementIds.SHEET_SPEED)
            rule.onNodeWithTag(ElementIds.speedOption("1_0")).assertIsSelected()
            rule.tap(ElementIds.speedOption("1_5"))
            assertTrue(p.await { speed == 1.5f })
            rule.tap(ElementIds.PLAYER_SPEED_BUTTON)
            rule.onNodeWithTag(ElementIds.speedOption("1_5")).assertIsSelected()
        }
    }

    @Test fun P07_A34_sleepAtEndOfEntry() {
        openPlayer().use { p ->
            rule.tap(ElementIds.PLAYER_SLEEP_TIMER_BUTTON)
            rule.tap(ElementIds.sleepOption("end"))
            p.seekToEndMinus(3_000)
            // Seeking briefly reports !isPlaying while buffering; wait for the settled pause instead.
            assertTrue(p.await(15_000) { !isPlaying && playbackState == androidx.media3.common.Player.STATE_READY })
            assertTrue(p.currentIndex == 0 || p.positionMs < 1_000)
            rule.waitUntil { state() == "paused" }
        }
    }

    @Test fun P08_sleepTimerShowsRemainingAndCancels() {
        openPlayer().close()
        rule.tap(ElementIds.PLAYER_SLEEP_TIMER_BUTTON)
        rule.tap(ElementIds.sleepOption("15"))
        rule.waitForTag(ElementIds.PLAYER_SLEEP_REMAINING)
        assertTrue(rule.textOf(ElementIds.PLAYER_SLEEP_REMAINING).contains("1"))
        rule.tap(ElementIds.PLAYER_SLEEP_TIMER_BUTTON)
        rule.tap(ElementIds.sleepOption("off"))
        rule.waitForGone(ElementIds.PLAYER_SLEEP_REMAINING)
    }

    @Test fun P09_A13_dataSaverMeteredPicks512kb() {
        AppEnvironment.networkOverride = NetworkKind.METERED
        openPlayer("fx_film").close()
        rule.tap(ElementIds.PLAYER_QUALITY_BUTTON)
        rule.waitForTag(ElementIds.SHEET_QUALITY)
        assertTrue(rule.textOf(ElementIds.QUALITY_OPTION_AUTO).contains("512Kb MPEG4"))
        rule.waitForTag(ElementIds.qualityOption("h_264"))
        rule.tap(ElementIds.qualityOption("h_264"))
        rule.waitUntil(10_000) { server.requests.any { it.path.orEmpty().endsWith("/fx_film.mp4") } }
    }

    @Test fun P10_A15_audioAutoVbrFlacListed() {
        openPlayer().close()
        rule.tap(ElementIds.PLAYER_QUALITY_BUTTON)
        assertTrue(rule.textOf(ElementIds.QUALITY_OPTION_AUTO).contains("VBR MP3"))
        listOf("128kbps_mp3", "ogg_vorbis", "flac").forEach { rule.waitForTag(ElementIds.qualityOption(it)) }
        assertTrue(server.requests.none { it.path.orEmpty().endsWith(".flac") })
    }

    @Test fun P11_A16_mixedQueueSwitchesLayout() {
        rule.openItem("fx_film")
        rule.addEntryToNewPlaylist(0, "Mixed")
        rule.openLibrary()
        val playlist = rule.tagsWithPrefix("library_playlist_").first { it != ElementIds.libraryPlaylist(ElementIds.LIKED_PLAYLIST_ID) }
        rule.openItem("fx_radio_show")
        rule.addEntryToPlaylist(0, playlist.removePrefix("library_playlist_"))
        rule.openLibrary()
        rule.tap(playlist)
        rule.tap(ElementIds.PLAYLIST_PLAY_BUTTON)
        rule.tap(ElementIds.MINI_PLAYER)
        rule.waitForTag(ElementIds.PLAYER_VIDEO_SURFACE, 15_000)
        PlayerProbe.connect().use { p ->
            assertEquals(listOf("Fixture Film", "Episode 01"), p.queueTitles)
            rule.tap(ElementIds.PLAYER_NEXT_BUTTON)
            assertTrue(p.await { currentIndex == 1 })
        }
        rule.waitForTag(ElementIds.PLAYER_ARTWORK)
        assertFalse(rule.exists(ElementIds.PLAYER_VIDEO_SURFACE))
    }

    @Test fun P12_queueReorderRemoveJump() {
        openPlayer().use { p ->
            rule.tap(ElementIds.PLAYER_QUEUE_BUTTON)
            rule.waitForTag(ElementIds.SHEET_QUEUE)
            rule.onNodeWithTag(ElementIds.queueDrag(2)).performTouchInput { swipeUp() }
            rule.waitUntil { p.queueTitles.indexOf("Episode 03") < 2 }
            val before = p.queueSize
            rule.tap(ElementIds.queueRemove(1))
            assertTrue(p.await { queueSize == before - 1 })
            rule.tap(ElementIds.queueRow(3))
            assertTrue(p.await { currentIndex == 3 })
        }
    }

    @Test fun P13_A33_subtitleMenu() {
        openPlayer("fx_film").close()
        rule.tap(ElementIds.PLAYER_SUBTITLES_BUTTON)
        rule.waitForTag(ElementIds.SHEET_SUBTITLES)
        rule.onNodeWithTag(ElementIds.SUBTITLE_OPTION_OFF).assertIsSelected()
        val labels = listOf(0, 1).map { rule.textOf(ElementIds.subtitleOption(it)) }
        assertTrue(labels.toString(), labels.any { it.contains("English") })
        assertTrue(labels.toString(), labels.any { it.contains("Auto-generated") })
    }

    @Test fun P14_formatFallbackThenError() {
        server.fail("/fx_radio_show_e01\\.mp3$", 404)
        openPlayer().close()
        assertFalse(rule.exists(ElementIds.PLAYER_ERROR))
        server.fail("^/download/", 404)
        // The player pre-buffers the next entry, so skip past it to one that must be fetched after the failure rule.
        rule.tap(ElementIds.PLAYER_NEXT_BUTTON)
        rule.tap(ElementIds.PLAYER_NEXT_BUTTON)
        rule.waitForTag(ElementIds.PLAYER_ERROR, 30_000)
        server.clearRules()
        rule.tap(ElementIds.PLAYER_RETRY_BUTTON)
        rule.waitForGone(ElementIds.PLAYER_ERROR, 15_000)
    }

    @Test fun P15_A17_resumeAfterRelaunch() {
        openPlayer("fx_film").use { p ->
            p.seekTo(80_000); p.await { positionMs >= 80_000 }; p.pause()
        }
        // Collapse first: the expanded player covers the navigation bar and stays expanded across recreation.
        rule.tap(ElementIds.PLAYER_COLLAPSE_BUTTON)
        rule.activityRule.scenario.recreate()
        rule.tap(ElementIds.NAV_HOME)
        rule.tapIn(ElementIds.HOME_LIST, ElementIds.homeCard(ElementIds.HOME_ROW_CONTINUE, "fx_film"))
        PlayerProbe.connect().use { p ->
            assertTrue(p.await { isPlaying })
            assertTrue("position ${p.positionMs}", abs(p.positionMs - 80_000) < 3_000)
        }
    }

    @Test fun P16_A32_videoEntersPictureInPicture() {
        openPlayer("fx_film").close()
        rule.waitForTag(ElementIds.PLAYER_VIDEO_SURFACE)
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome()
        var pip = false
        val end = System.currentTimeMillis() + 10_000
        while (!pip && System.currentTimeMillis() < end) {
            rule.activityRule.scenario.onActivity { pip = it.isInPictureInPictureMode }
            if (!pip) Thread.sleep(300)
        }
        assertTrue(pip)
    }

    @Test fun P17_R34_mediaSessionExposesQueueAndControls() {
        rule.play("fx_radio_show")
        PlayerProbe.connect().use { p ->
            assertTrue(p.await { isPlaying })
            assertEquals(6, p.queueSize)
            p.pause()
            rule.waitUntil { rule.textOf(ElementIds.MINI_PLAYER_TITLE).isNotEmpty() }
            assertFalse(p.isPlaying)
            p.play()
            assertTrue(p.await { isPlaying })
        }
    }

    @Test fun P18_swipeMiniPlayerStopsWithUndo() {
        rule.play("fx_radio_show")
        PlayerProbe.connect().use { p ->
            assertTrue(p.await { isPlaying })
            rule.onNodeWithTag(ElementIds.MINI_PLAYER).performTouchInput { swipeDown() }
            rule.waitForGone(ElementIds.MINI_PLAYER)
            assertFalse(p.isPlaying)
            rule.tap(ElementIds.PLAYER_UNDO_STOP)
            rule.waitForTag(ElementIds.MINI_PLAYER)
            assertEquals(6, p.queueSize)
        }
    }

    @Test fun P20_R64_controlsLabelledAllLanguages() {
        val controls = listOf(
            ElementIds.PLAYER_PLAY_PAUSE_BUTTON, ElementIds.PLAYER_PREVIOUS_BUTTON, ElementIds.PLAYER_NEXT_BUTTON,
            ElementIds.PLAYER_SKIP_BACK_BUTTON, ElementIds.PLAYER_SKIP_FORWARD_BUTTON, ElementIds.PLAYER_LIKE_BUTTON,
            ElementIds.PLAYER_QUEUE_BUTTON, ElementIds.PLAYER_QUALITY_BUTTON, ElementIds.PLAYER_SPEED_BUTTON,
            ElementIds.PLAYER_SLEEP_TIMER_BUTTON, ElementIds.PLAYER_SHUFFLE_BUTTON, ElementIds.PLAYER_REPEAT_BUTTON,
        )
        openPlayer().close()
        listOf("en", "da", "de").forEach { iso ->
            rule.tap(ElementIds.PLAYER_COLLAPSE_BUTTON)
            // Settings lives on tab roots (UI_SHELL_SPEC); the player was started from an item page.
            rule.tap(ElementIds.NAV_HOME)
            rule.openSettingsRow("app_language")
            rule.tap(ElementIds.settingsLanguage(iso))
            pressBack(rule); pressBack(rule)
            rule.tap(ElementIds.MINI_PLAYER)
            controls.forEach { tag ->
                val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
                val label = node.config.getOrElseNullable(SemanticsProperties.ContentDescription) { null }.orEmpty().joinToString()
                assertTrue("$tag label in $iso", label.isNotBlank())
                val density = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density
                assertTrue("$tag ≥ 48 dp", node.size.width >= 48 * density - 1 && node.size.height >= 48 * density - 1)
            }
        }
    }

    private fun requestedOrientation(): Int {
        var o = 0
        rule.activityRule.scenario.onActivity { o = it.requestedOrientation }
        return o
    }

    private fun statusBarVisible(): Boolean {
        var v = true
        rule.activityRule.scenario.onActivity {
            v = androidx.core.view.ViewCompat.getRootWindowInsets(it.window.decorView)
                ?.isVisible(androidx.core.view.WindowInsetsCompat.Type.statusBars()) ?: true
        }
        return v
    }

    private fun eventually(timeoutMs: Long = 10_000, check: () -> Boolean): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (check()) return true
            Thread.sleep(200)
        }
        return check()
    }

    private fun maximize(identifier: String) {
        openPlayer(identifier).close()
        rule.waitForTag(ElementIds.PLAYER_FULLSCREEN_BUTTON)
        rule.onNodeWithTag(ElementIds.PLAYER_FULLSCREEN_BUTTON).performClick()
        rule.waitForTag(ElementIds.PLAYER_FULLSCREEN)
    }

    @Test fun P21_R86_wideVideoMaximizesToLandscape() {
        maximize("fx_film")
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, requestedOrientation())
        assertTrue(eventually { !statusBarVisible() })
    }

    @Test fun P22_R86_backLeavesFullScreen() {
        maximize("fx_film")
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        assertTrue(eventually { rule.onAllNodesWithTag(ElementIds.PLAYER_FULLSCREEN).fetchSemanticsNodes().isEmpty() })
        rule.waitForTag(ElementIds.PLAYER_SCREEN)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, requestedOrientation())
        assertTrue(eventually { statusBarVisible() })
    }

    @Test fun P23_R86_portraitVideoStaysPortrait() {
        maximize("fx_vertical")
        assertFalse(requestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
    }

    @Test fun P24_R86_audioHasNoMaximize() {
        openPlayer("fx_radio_show").close()
        rule.waitForTag(ElementIds.PLAYER_ARTWORK)
        assertTrue(rule.onAllNodesWithTag(ElementIds.PLAYER_FULLSCREEN_BUTTON).fetchSemanticsNodes().isEmpty())
    }
}
