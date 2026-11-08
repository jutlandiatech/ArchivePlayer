package com.jutlandiatech.archiveplayer.core.playback

import com.jutlandiatech.archiveplayer.core.catalog.MediaKind
import kotlin.random.Random

/** Resume and finished rules (R36). */
object ResumePolicy {
    private const val MIN_STORED_MS = 30_000L
    private const val END_WINDOW_MS = 30_000L
    private const val FINISHED_FRACTION = 0.95

    fun shouldStore(positionMs: Long, durationMs: Long): Boolean =
        positionMs > MIN_STORED_MS && (durationMs <= 0 || !isFinished(positionMs, durationMs))

    fun isFinished(positionMs: Long, durationMs: Long): Boolean =
        durationMs > 0 && (durationMs - positionMs <= END_WINDOW_MS || positionMs >= durationMs * FINISHED_FRACTION)

    /** Start position for an entry with a stored position; 0 when none or finished. */
    fun resumeFrom(storedPositionMs: Long?, durationMs: Long): Long = when {
        storedPositionMs == null || storedPositionMs <= 0 -> 0L
        isFinished(storedPositionMs, durationMs) -> 0L
        else -> storedPositionMs
    }
}

/** Skip intervals and speed range (R37). */
object PlayerControls {
    val skipBackMs: Long get() = 10_000L
    val skipForwardMs: Long get() = 30_000L
    val speedSteps: List<Float> get() = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    fun clampSpeed(speed: Float): Float = speed.coerceIn(speedSteps.first(), speedSteps.last())
}

sealed interface SleepTimerSetting {
    data class Minutes(val minutes: Int) : SleepTimerSetting
    data object EndOfEntry : SleepTimerSetting
}

/** Sleep timer with injected time (R37, A34). */
class SleepTimer {
    var setting: SleepTimerSetting? = null
        private set
    private var deadlineMs: Long? = null

    fun set(setting: SleepTimerSetting, nowMs: Long) {
        this.setting = setting
        deadlineMs = (setting as? SleepTimerSetting.Minutes)?.let { nowMs + it.minutes * 60_000L }
    }

    fun cancel() {
        setting = null
        deadlineMs = null
    }

    fun remainingMs(nowMs: Long): Long? = deadlineMs?.let { (it - nowMs).coerceAtLeast(0) }

    /** True once when playback must pause; the timer is then cleared. */
    fun shouldPause(nowMs: Long, entryEnded: Boolean): Boolean {
        val due = when (setting) {
            null -> false
            SleepTimerSetting.EndOfEntry -> entryEnded
            is SleepTimerSetting.Minutes -> nowMs >= (deadlineMs ?: Long.MAX_VALUE)
        }
        if (due) cancel()
        return due
    }
}

enum class RepeatMode { OFF, ALL, ONE }

data class QueueItem(val id: String, val kind: MediaKind)

/** Play queue (R35, R37). */
class PlayQueue {
    private class Slot(val item: QueueItem)

    private val slots = mutableListOf<Slot>()
    private var original: MutableList<Slot>? = null
    private var index = 0

    val items: List<QueueItem> get() = slots.map { it.item }
    val currentIndex: Int get() = if (slots.isEmpty()) -1 else index
    val current: QueueItem? get() = slots.getOrNull(index)?.item
    var repeat: RepeatMode = RepeatMode.OFF
    val shuffled: Boolean get() = original != null

    private fun List<Slot>.indexOfSlot(slot: Slot) = indexOfFirst { it === slot }

    fun replace(newItems: List<QueueItem>, startIndex: Int = 0) {
        slots.clear()
        slots += newItems.map { Slot(it) }
        original = null
        index = startIndex.coerceIn(0, (slots.size - 1).coerceAtLeast(0))
    }

    fun jumpTo(position: Int) {
        if (position in slots.indices) index = position
    }

    fun playNext(item: QueueItem) {
        val slot = Slot(item)
        val cur = slots.getOrNull(index)
        slots.add(if (slots.isEmpty()) 0 else index + 1, slot)
        original?.let { o -> o.add(cur?.let { o.indexOfSlot(it) + 1 } ?: o.size, slot) }
    }

    fun add(item: QueueItem) {
        val slot = Slot(item)
        slots += slot
        original?.add(slot)
    }

    fun move(from: Int, to: Int) {
        if (from !in slots.indices || to !in slots.indices || from == to) return
        val cur = slots[index]
        slots.add(to, slots.removeAt(from))
        index = slots.indexOfSlot(cur)
    }

    fun remove(position: Int) {
        if (position !in slots.indices) return
        val removed = slots.removeAt(position)
        original?.let { o -> o.removeAt(o.indexOfSlot(removed)) }
        if (position < index) index--
        index = index.coerceIn(0, (slots.size - 1).coerceAtLeast(0))
    }

    /** Advances per repeat/shuffle; null at the end with repeat OFF. */
    fun next(): QueueItem? {
        if (slots.isEmpty()) return null
        return when {
            repeat == RepeatMode.ONE -> current
            index + 1 < slots.size -> { index++; current }
            repeat == RepeatMode.ALL -> { index = 0; current }
            else -> null
        }
    }

    fun previous(): QueueItem? {
        if (slots.isEmpty()) return null
        when {
            index > 0 -> index--
            repeat == RepeatMode.ALL -> index = slots.lastIndex
        }
        return current
    }

    fun setShuffle(enabled: Boolean, random: Random) {
        if (enabled == shuffled || slots.isEmpty()) {
            if (!enabled) original = null
            return
        }
        val cur = slots[index]
        if (enabled) {
            original = slots.toMutableList()
            val rest = slots.filter { it !== cur }.shuffled(random)
            slots.clear(); slots += cur; slots += rest
            index = 0
        } else {
            val o = original!!
            slots.clear(); slots += o
            original = null
            index = slots.indexOfSlot(cur)
        }
    }
}

enum class PlayerLayout {
    ARTWORK, VIDEO;

    companion object {
        fun forKind(kind: MediaKind): PlayerLayout = if (kind == MediaKind.VIDEO) VIDEO else ARTWORK
    }
}
