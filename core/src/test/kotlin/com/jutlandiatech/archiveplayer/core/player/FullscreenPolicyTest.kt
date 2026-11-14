package com.jutlandiatech.archiveplayer.core.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Full-screen orientation rules (R86, P21–P25). */
class FullscreenPolicyTest {
    @Test fun R86_fourByThreeOrWiderIsLandscape() {
        assertEquals(FullscreenOrientation.LANDSCAPE, FullscreenPolicy.orientation(1920, 1080, casting = false))
        assertEquals(FullscreenOrientation.LANDSCAPE, FullscreenPolicy.orientation(640, 480, casting = false))
    }

    @Test fun R86_narrowerThanFourByThreeIsPortrait() {
        assertEquals(FullscreenOrientation.PORTRAIT, FullscreenPolicy.orientation(1080, 1920, casting = false))
        assertEquals(FullscreenOrientation.PORTRAIT, FullscreenPolicy.orientation(500, 400, casting = false))
        assertEquals(FullscreenOrientation.PORTRAIT, FullscreenPolicy.orientation(0, 0, casting = false))
    }

    @Test fun P25_castingNeverChangesOrientation() {
        assertEquals(FullscreenOrientation.NONE, FullscreenPolicy.orientation(1920, 1080, casting = true))
        assertFalse(FullscreenPolicy.offersFullscreen(video = true, casting = true))
        assertFalse(FullscreenPolicy.enterOnLandscapeRotation(1920, 1080, casting = true))
    }

    @Test fun R86_rotationEntersOnlyForWideLocalVideo() {
        assertTrue(FullscreenPolicy.enterOnLandscapeRotation(1920, 1080, casting = false))
        assertFalse(FullscreenPolicy.enterOnLandscapeRotation(1080, 1920, casting = false))
        assertTrue(FullscreenPolicy.offersFullscreen(video = true, casting = false))
        assertFalse(FullscreenPolicy.offersFullscreen(video = false, casting = false))
    }
}
