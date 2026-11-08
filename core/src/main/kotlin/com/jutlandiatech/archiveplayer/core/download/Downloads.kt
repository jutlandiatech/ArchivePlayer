package com.jutlandiatech.archiveplayer.core.download

import com.jutlandiatech.archiveplayer.core.catalog.MediaKind
import com.jutlandiatech.archiveplayer.core.playlist.EntryRef
import com.jutlandiatech.archiveplayer.core.settings.DownloadLocation

data class DownloadCandidate(val ref: EntryRef, val kind: MediaKind, val sizeBytes: Long)

data class NetworkState(val connected: Boolean, val unmetered: Boolean)

data class DownloadPlan(
    val queued: List<DownloadCandidate>,
    val skippedVideoCount: Int,
    val waitingForWifi: Boolean,
    val offerMobileDataOverride: Boolean,
)

/** Audio-only, Wi-Fi-only-by-default downloads (R59, R60, A25, A39). */
object DownloadPolicy {
    fun plan(candidates: List<DownloadCandidate>, wifiOnly: Boolean, network: NetworkState): DownloadPlan {
        val audio = candidates.filter { it.kind == MediaKind.AUDIO }
        val waiting = audio.isNotEmpty() && wifiOnly && !network.unmetered
        return DownloadPlan(
            queued = audio,
            skippedVideoCount = candidates.size - audio.size,
            waitingForWifi = waiting,
            offerMobileDataOverride = waiting && network.connected,
        )
    }
    fun canStart(wifiOnly: Boolean, network: NetworkState, mobileDataOverride: Boolean): Boolean =
        network.connected && (network.unmetered || !wifiOnly || mobileDataOverride)
}

data class Volume(val location: DownloadLocation, val mounted: Boolean, val freeBytes: Long, val usedByAppBytes: Long)

data class StoredDownload(val ref: EntryRef, val location: DownloadLocation, val sizeBytes: Long)

enum class Availability { OFFLINE, SD_CARD_REMOVED }
enum class PlaybackSource { LOCAL_FILE, STREAM }

data class MovePrompt(val count: Int, val totalBytes: Long, val target: DownloadLocation)

/** Download location rules (R61, R85, A43–A45). */
object StoragePolicy {
    val lowSpaceThresholdBytes: Long get() = 500_000_000L
    fun offeredLocations(volumes: List<Volume>): List<DownloadLocation> =
        DownloadLocation.entries.filter { loc -> loc == DownloadLocation.INTERNAL || volumes.any { it.location == loc && it.mounted } }
    fun effectiveLocation(selected: DownloadLocation, volumes: List<Volume>): DownloadLocation =
        if (selected in offeredLocations(volumes)) selected else DownloadLocation.INTERNAL
    fun availability(download: StoredDownload, volumes: List<Volume>): Availability =
        if (download.location == DownloadLocation.INTERNAL || volumes.any { it.location == download.location && it.mounted }) {
            Availability.OFFLINE
        } else Availability.SD_CARD_REMOVED
    fun playbackSource(download: StoredDownload?, volumes: List<Volume>, casting: Boolean): PlaybackSource =
        if (download != null && !casting && availability(download, volumes) == Availability.OFFLINE) PlaybackSource.LOCAL_FILE
        else PlaybackSource.STREAM
    fun movePrompt(downloads: List<StoredDownload>, newLocation: DownloadLocation): MovePrompt? {
        val toMove = downloads.filter { it.location != newLocation }
        return if (toMove.isEmpty()) null else MovePrompt(toMove.size, toMove.sumOf { it.sizeBytes }, newLocation)
    }
    fun isLowSpace(volume: Volume): Boolean = volume.freeBytes < lowSpaceThresholdBytes
}

/** File operations on download volumes (implemented by the Android adapter). */
interface DownloadFileOps {
    fun copy(download: StoredDownload, target: DownloadLocation): Boolean
    fun verifyCopy(download: StoredDownload, target: DownloadLocation): Boolean
    fun delete(download: StoredDownload)
}

data class MoveOutcome(val moved: List<StoredDownload>, val failed: List<StoredDownload>)

/** Resumable move: a source is deleted only after its copy is verified (R85). */
class DownloadMover(private val ops: DownloadFileOps) {
    fun move(downloads: List<StoredDownload>, target: DownloadLocation): MoveOutcome {
        val moved = mutableListOf<StoredDownload>()
        val failed = mutableListOf<StoredDownload>()
        for (download in downloads) {
            if (download.location == target) { moved += download; continue }
            if (ops.copy(download, target) && ops.verifyCopy(download, target)) {
                ops.delete(download)
                moved += download.copy(location = target)
            } else failed += download
        }
        return MoveOutcome(moved, failed)
    }
}
