package com.thomasnorlund.archiveplayer.support

import android.content.ComponentName
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit

/**
 * Reads the app's playback state through a Media3 [MediaController] connected to the app's
 * `MediaSessionService` (R34). Contract: the service is `com.thomasnorlund.archiveplayer.playback.PlaybackService`
 * and every MediaItem carries the entry title in `mediaMetadata.title`.
 */
class PlayerProbe private constructor(private val controller: MediaController) : AutoCloseable {
    private fun <T> onMain(block: (Player) -> T): T {
        var result: Result<T>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync { result = runCatching { block(controller) } }
        return result!!.getOrThrow()
    }

    val isPlaying: Boolean get() = onMain { it.isPlaying }
    val positionMs: Long get() = onMain { it.currentPosition }
    val speed: Float get() = onMain { it.playbackParameters.speed }
    val queueSize: Int get() = onMain { it.mediaItemCount }
    val currentIndex: Int get() = onMain { it.currentMediaItemIndex }
    val currentTitle: String? get() = onMain { it.mediaMetadata.title?.toString() }
    val queueTitles: List<String?> get() = onMain { p -> (0 until p.mediaItemCount).map { p.getMediaItemAt(it).mediaMetadata.title?.toString() } }
    val playbackState: Int get() = onMain { it.playbackState }

    fun pause() = onMain { it.pause() }
    fun play() = onMain { it.play() }
    fun seekTo(ms: Long) = onMain { it.seekTo(ms) }
    fun seekToEndMinus(ms: Long) = onMain { it.seekTo((it.duration - ms).coerceAtLeast(0)) }

    /** Polls until [condition] holds or [timeoutMs] passes; returns whether it held. */
    fun await(timeoutMs: Long = 10_000, condition: PlayerProbe.() -> Boolean): Boolean {
        val end = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < end) {
            if (runCatching { condition() }.getOrDefault(false)) return true
            Thread.sleep(200)
        }
        return false
    }

    override fun close() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.release() }
    }

    companion object {
        const val SERVICE = "com.thomasnorlund.archiveplayer.playback.PlaybackService"

        fun connect(timeoutSeconds: Long = 10): PlayerProbe {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val token = SessionToken(context, ComponentName(context.packageName, SERVICE))
            var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
            InstrumentationRegistry.getInstrumentation().runOnMainSync { future = MediaController.Builder(context, token).buildAsync() }
            return PlayerProbe(future!!.get(timeoutSeconds, TimeUnit.SECONDS))
        }
    }
}
