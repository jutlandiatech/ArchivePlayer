package com.jutlandiatech.archiveplayer.core.player

enum class FullscreenOrientation { LANDSCAPE, PORTRAIT, NONE }

/** Full-screen video rules (R86): landscape only for 4:3-or-wider video playing on the phone, never while casting. */
object FullscreenPolicy {
    private fun wide(width: Int, height: Int) = width > 0 && height > 0 && width * 3L >= height * 4L

    fun orientation(width: Int, height: Int, casting: Boolean): FullscreenOrientation = when {
        casting -> FullscreenOrientation.NONE
        wide(width, height) -> FullscreenOrientation.LANDSCAPE
        else -> FullscreenOrientation.PORTRAIT
    }

    fun offersFullscreen(video: Boolean, casting: Boolean): Boolean = video && !casting

    fun enterOnLandscapeRotation(width: Int, height: Int, casting: Boolean): Boolean = !casting && wide(width, height)
}
