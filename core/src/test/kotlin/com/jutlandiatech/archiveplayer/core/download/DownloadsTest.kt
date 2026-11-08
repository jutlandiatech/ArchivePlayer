package com.jutlandiatech.archiveplayer.core.download

import com.jutlandiatech.archiveplayer.core.catalog.MediaKind
import com.jutlandiatech.archiveplayer.core.playlist.EntryRef
import com.jutlandiatech.archiveplayer.core.settings.DownloadLocation
import com.jutlandiatech.archiveplayer.core.settings.DownloadLocation.INTERNAL
import com.jutlandiatech.archiveplayer.core.settings.DownloadLocation.SD_CARD
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadsTest {
    private val mb = 1_000_000L
    private fun c(id: String, kind: MediaKind) = DownloadCandidate(EntryRef(id, "$id.mp3"), kind, 10 * mb)
    private val mobile = NetworkState(connected = true, unmetered = false)
    private val wifi = NetworkState(connected = true, unmetered = true)
    private val offline = NetworkState(connected = false, unmetered = false)

    @Test fun A25_mixedPlaylistOnMobileDataWithWifiOnly() {
        val plan = DownloadPolicy.plan(listOf(c("a1", MediaKind.AUDIO), c("v1", MediaKind.VIDEO), c("a2", MediaKind.AUDIO)), wifiOnly = true, network = mobile)
        assertEquals(listOf("a1", "a2"), plan.queued.map { it.ref.identifier })
        assertEquals(1, plan.skippedVideoCount)
        assertTrue(plan.waitingForWifi)
        assertTrue(plan.offerMobileDataOverride)
    }

    @Test fun R60_onWifiNoWaiting() {
        val plan = DownloadPolicy.plan(listOf(c("a1", MediaKind.AUDIO)), wifiOnly = true, network = wifi)
        assertFalse(plan.waitingForWifi)
        assertFalse(plan.offerMobileDataOverride)
        assertFalse(DownloadPolicy.plan(listOf(c("a1", MediaKind.AUDIO)), wifiOnly = false, network = mobile).waitingForWifi)
    }

    @Test fun A39_mobileDataOverrideStartsOneDownload() {
        assertFalse(DownloadPolicy.canStart(wifiOnly = true, network = mobile, mobileDataOverride = false))
        assertTrue(DownloadPolicy.canStart(wifiOnly = true, network = mobile, mobileDataOverride = true))
        assertTrue(DownloadPolicy.canStart(wifiOnly = false, network = mobile, mobileDataOverride = false))
        assertFalse(DownloadPolicy.canStart(wifiOnly = false, network = offline, mobileDataOverride = true))
    }

    private val internal = Volume(INTERNAL, mounted = true, freeBytes = 20_000 * mb, usedByAppBytes = 416 * mb)
    private val sd = Volume(SD_CARD, mounted = true, freeBytes = 50_000 * mb, usedByAppBytes = 0)
    private val sdRemoved = sd.copy(mounted = false)
    private fun d(id: String, loc: DownloadLocation, size: Long = 208 * mb) = StoredDownload(EntryRef(id, "$id.mp3"), loc, size)

    @Test fun R85_sdCardOfferedOnlyWhenMounted() {
        assertEquals(listOf(INTERNAL, SD_CARD), StoragePolicy.offeredLocations(listOf(internal, sd)))
        assertEquals(listOf(INTERNAL), StoragePolicy.offeredLocations(listOf(internal, sdRemoved)))
        assertEquals(listOf(INTERNAL), StoragePolicy.offeredLocations(listOf(internal)))
    }

    @Test fun A43_newDownloadsGoToChosenSdCard() = assertEquals(SD_CARD, StoragePolicy.effectiveLocation(SD_CARD, listOf(internal, sd)))

    @Test fun R85_missingCardFallsBackToInternal() = assertEquals(INTERNAL, StoragePolicy.effectiveLocation(SD_CARD, listOf(internal, sdRemoved)))

    @Test fun A44_switchLocationOffersMove() {
        val prompt = StoragePolicy.movePrompt(listOf(d("a", INTERNAL), d("b", INTERNAL)), SD_CARD)
        assertEquals(MovePrompt(2, 416 * mb, SD_CARD), prompt)
        assertNull(StoragePolicy.movePrompt(listOf(d("a", SD_CARD)), SD_CARD))
        assertNull(StoragePolicy.movePrompt(emptyList(), SD_CARD))
    }

    @Test fun A45_removedCardMarksDownloadsAndStreams() {
        val onCard = d("a", SD_CARD)
        assertEquals(Availability.SD_CARD_REMOVED, StoragePolicy.availability(onCard, listOf(internal, sdRemoved)))
        assertEquals(PlaybackSource.STREAM, StoragePolicy.playbackSource(onCard, listOf(internal, sdRemoved), casting = false))
        assertEquals(Availability.OFFLINE, StoragePolicy.availability(onCard, listOf(internal, sd)))
        assertEquals(PlaybackSource.LOCAL_FILE, StoragePolicy.playbackSource(onCard, listOf(internal, sd), casting = false))
    }

    @Test fun R61_castAndMissingDownloadsStream() {
        assertEquals(PlaybackSource.STREAM, StoragePolicy.playbackSource(d("a", INTERNAL), listOf(internal), casting = true))
        assertEquals(PlaybackSource.STREAM, StoragePolicy.playbackSource(null, listOf(internal), casting = false))
    }

    @Test fun R61_lowSpaceWarningBelow500MbPerLocation() {
        assertEquals(500 * mb, StoragePolicy.lowSpaceThresholdBytes)
        assertTrue(StoragePolicy.isLowSpace(sd.copy(freeBytes = 500 * mb - 1)))
        assertFalse(StoragePolicy.isLowSpace(sd.copy(freeBytes = 500 * mb)))
    }

    private class FakeOps(val verifyOk: Set<String>) : DownloadFileOps {
        val copied = mutableListOf<String>(); val deleted = mutableListOf<String>()
        override fun copy(download: StoredDownload, target: DownloadLocation): Boolean { copied += download.ref.identifier; return true }
        override fun verifyCopy(download: StoredDownload, target: DownloadLocation) = download.ref.identifier in verifyOk
        override fun delete(download: StoredDownload) { deleted += download.ref.identifier }
    }

    @Test fun A44_moveDeletesSourceOnlyAfterVerifiedCopy() {
        val ops = FakeOps(verifyOk = setOf("a"))
        val outcome = DownloadMover(ops).move(listOf(d("a", INTERNAL), d("b", INTERNAL)), SD_CARD)
        assertEquals(listOf("a"), outcome.moved.map { it.ref.identifier })
        assertTrue(outcome.moved.all { it.location == SD_CARD })
        assertEquals(listOf("b"), outcome.failed.map { it.ref.identifier })
        assertEquals(listOf("a"), ops.deleted)
    }

    @Test fun R85_moveIsResumable() {
        val ops = FakeOps(verifyOk = setOf("a", "b"))
        val outcome = DownloadMover(ops).move(listOf(d("a", SD_CARD), d("b", INTERNAL)), SD_CARD)
        assertEquals(listOf("b"), ops.copied)
        assertEquals(setOf("a", "b"), outcome.moved.map { it.ref.identifier }.toSet())
    }
}
