package com.thomasnorlund.archiveplayer.core.download

import com.thomasnorlund.archiveplayer.core.catalog.MediaKind
import com.thomasnorlund.archiveplayer.core.playlist.EntryRef
import com.thomasnorlund.archiveplayer.core.settings.DownloadLocation

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
    fun plan(candidates: List<DownloadCandidate>, wifiOnly: Boolean, network: NetworkState): DownloadPlan = TODO()
    fun canStart(wifiOnly: Boolean, network: NetworkState, mobileDataOverride: Boolean): Boolean = TODO()
}

data class Volume(val location: DownloadLocation, val mounted: Boolean, val freeBytes: Long, val usedByAppBytes: Long)

data class StoredDownload(val ref: EntryRef, val location: DownloadLocation, val sizeBytes: Long)

enum class Availability { OFFLINE, SD_CARD_REMOVED }
enum class PlaybackSource { LOCAL_FILE, STREAM }

data class MovePrompt(val count: Int, val totalBytes: Long, val target: DownloadLocation)

/** Download location rules (R61, R85, A43–A45). */
object StoragePolicy {
    val lowSpaceThresholdBytes: Long get() = TODO()
    fun offeredLocations(volumes: List<Volume>): List<DownloadLocation> = TODO()
    fun effectiveLocation(selected: DownloadLocation, volumes: List<Volume>): DownloadLocation = TODO()
    fun availability(download: StoredDownload, volumes: List<Volume>): Availability = TODO()
    fun playbackSource(download: StoredDownload?, volumes: List<Volume>, casting: Boolean): PlaybackSource = TODO()
    fun movePrompt(downloads: List<StoredDownload>, newLocation: DownloadLocation): MovePrompt? = TODO()
    fun isLowSpace(volume: Volume): Boolean = TODO()
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
    fun move(downloads: List<StoredDownload>, target: DownloadLocation): MoveOutcome = TODO()
}
