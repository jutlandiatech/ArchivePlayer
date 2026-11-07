package com.thomasnorlund.archiveplayer.core.playback

import com.thomasnorlund.archiveplayer.core.catalog.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlaybackTest {
    private val min = 60_000L

    @Test fun R36_storedAfter30Seconds() {
        assertFalse(ResumePolicy.shouldStore(29_000, 90 * min))
        assertTrue(ResumePolicy.shouldStore(31_000, 90 * min))
    }

    @Test fun A17_resumesAt40Minutes() = assertEquals(40 * min, ResumePolicy.resumeFrom(40 * min, 90 * min))

    @Test fun R36_finishedRules() {
        assertTrue(ResumePolicy.isFinished(90 * min - 20_000, 90 * min))      // last 30 s
        assertTrue(ResumePolicy.isFinished(9 * min + 30_000, 10 * min))       // 95 %
        assertFalse(ResumePolicy.isFinished(9 * min + 24_000, 10 * min))      // 94 %, 36 s left
        assertEquals(0L, ResumePolicy.resumeFrom(90 * min - 10_000, 90 * min))
        assertEquals(0L, ResumePolicy.resumeFrom(null, 90 * min))
    }

    @Test fun R37_skipAndSpeed() {
        assertEquals(10_000L, PlayerControls.skipBackMs)
        assertEquals(30_000L, PlayerControls.skipForwardMs)
        assertEquals(0.5f, PlayerControls.speedSteps.first())
        assertEquals(2.0f, PlayerControls.speedSteps.last())
        assertTrue(1.0f in PlayerControls.speedSteps)
        assertEquals(2.0f, PlayerControls.clampSpeed(3f))
        assertEquals(0.5f, PlayerControls.clampSpeed(0.1f))
        assertEquals(1.25f, PlayerControls.clampSpeed(1.25f))
    }

    @Test fun R37_sleepTimerMinutes() {
        val t = SleepTimer()
        t.set(SleepTimerSetting.Minutes(15), nowMs = 0)
        assertEquals(15 * min, t.remainingMs(0))
        assertFalse(t.shouldPause(14 * min, entryEnded = false))
        assertTrue(t.shouldPause(15 * min, entryEnded = false))
        assertFalse(t.shouldPause(16 * min, entryEnded = false))
        assertNull(t.remainingMs(16 * min))
    }

    @Test fun A34_sleepTimerEndOfEntry() {
        val t = SleepTimer()
        t.set(SleepTimerSetting.EndOfEntry, nowMs = 0)
        assertFalse(t.shouldPause(60 * min, entryEnded = false))
        assertTrue(t.shouldPause(61 * min, entryEnded = true))
    }

    @Test fun R37_cancelledTimerNeverPauses() {
        val t = SleepTimer()
        t.set(SleepTimerSetting.Minutes(1), 0)
        t.cancel()
        assertFalse(t.shouldPause(2 * min, entryEnded = true))
    }

    private fun items(vararg ids: String) = ids.map { QueueItem(it, if (it.startsWith("v")) MediaKind.VIDEO else MediaKind.AUDIO) }

    @Test fun R35_replaceAndEdit() {
        val q = PlayQueue()
        q.replace(items("a", "b", "c"), startIndex = 1)
        assertEquals("b", q.current!!.id)
        q.playNext(items("x").single())
        q.add(items("y").single())
        assertEquals(listOf("a", "b", "x", "c", "y"), q.items.map { it.id })
        q.move(4, 0)
        assertEquals(listOf("y", "a", "b", "x", "c"), q.items.map { it.id })
        assertEquals("b", q.current!!.id)
        q.remove(0)
        assertEquals("b", q.current!!.id)
    }

    @Test fun R37_repeatModes() {
        val q = PlayQueue()
        q.replace(items("a", "b"))
        assertEquals("b", q.next()!!.id)
        assertNull(q.next())
        q.repeat = RepeatMode.ALL
        assertEquals("a", q.next()!!.id)
        q.repeat = RepeatMode.ONE
        assertEquals("a", q.next()!!.id)
        q.repeat = RepeatMode.OFF
        q.next()
        assertEquals("a", q.previous()!!.id)
    }

    @Test fun R37_shuffleKeepsCurrentAndRestoresOrder() {
        val q = PlayQueue()
        q.replace(items("a", "b", "c", "d", "e"), startIndex = 2)
        q.setShuffle(true, Random(42))
        assertTrue(q.shuffled)
        assertEquals("c", q.current!!.id)
        assertEquals(setOf("a", "b", "c", "d", "e"), q.items.map { it.id }.toSet())
        q.setShuffle(false, Random(42))
        assertEquals(listOf("a", "b", "c", "d", "e"), q.items.map { it.id })
        assertEquals("c", q.current!!.id)
    }

    @Test fun A16_mixedQueueKeepsOrderAndSwitchesLayout() {
        val q = PlayQueue()
        q.replace(items("a1", "v1", "a2"))
        val layouts = mutableListOf(PlayerLayout.forKind(q.current!!.kind))
        while (true) { val n = q.next() ?: break; layouts += PlayerLayout.forKind(n.kind) }
        assertEquals(listOf(PlayerLayout.ARTWORK, PlayerLayout.VIDEO, PlayerLayout.ARTWORK), layouts)
    }
}
