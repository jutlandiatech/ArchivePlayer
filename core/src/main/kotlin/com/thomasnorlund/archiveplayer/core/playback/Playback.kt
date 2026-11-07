package com.thomasnorlund.archiveplayer.core.playback

import com.thomasnorlund.archiveplayer.core.catalog.MediaKind
import kotlin.random.Random

/** Resume and finished rules (R36). */
object ResumePolicy {
    fun shouldStore(positionMs: Long, durationMs: Long): Boolean = TODO()
    fun isFinished(positionMs: Long, durationMs: Long): Boolean = TODO()

    /** Start position for an entry with a stored position; 0 when none or finished. */
    fun resumeFrom(storedPositionMs: Long?, durationMs: Long): Long = TODO()
}

/** Skip intervals and speed range (R37). */
object PlayerControls {
    val skipBackMs: Long get() = TODO()
    val skipForwardMs: Long get() = TODO()
    val speedSteps: List<Float> get() = TODO()
    fun clampSpeed(speed: Float): Float = TODO()
}

sealed interface SleepTimerSetting {
    data class Minutes(val minutes: Int) : SleepTimerSetting
    data object EndOfEntry : SleepTimerSetting
}

/** Sleep timer with injected time (R37, A34). */
class SleepTimer {
    fun set(setting: SleepTimerSetting, nowMs: Long): Unit = TODO()
    fun cancel(): Unit = TODO()
    fun remainingMs(nowMs: Long): Long? = TODO()

    /** True once when playback must pause; the timer is then cleared. */
    fun shouldPause(nowMs: Long, entryEnded: Boolean): Boolean = TODO()
}

enum class RepeatMode { OFF, ALL, ONE }

data class QueueItem(val id: String, val kind: MediaKind)

/** Play queue (R35, R37). */
class PlayQueue {
    val items: List<QueueItem> get() = TODO()
    val currentIndex: Int get() = TODO()
    val current: QueueItem? get() = TODO()
    var repeat: RepeatMode
        get() = TODO()
        set(value) = TODO()
    val shuffled: Boolean get() = TODO()

    fun replace(newItems: List<QueueItem>, startIndex: Int = 0): Unit = TODO()
    fun playNext(item: QueueItem): Unit = TODO()
    fun add(item: QueueItem): Unit = TODO()
    fun move(from: Int, to: Int): Unit = TODO()
    fun remove(index: Int): Unit = TODO()

    /** Advances per repeat/shuffle; null at the end with repeat OFF. */
    fun next(): QueueItem? = TODO()
    fun previous(): QueueItem? = TODO()
    fun setShuffle(enabled: Boolean, random: Random): Unit = TODO()
}

enum class PlayerLayout {
    ARTWORK, VIDEO;

    companion object {
        fun forKind(kind: MediaKind): PlayerLayout = TODO()
    }
}
