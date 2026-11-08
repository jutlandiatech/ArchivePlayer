package com.thomasnorlund.archiveplayer.core.format

import com.thomasnorlund.archiveplayer.core.catalog.Entry
import com.thomasnorlund.archiveplayer.core.catalog.MediaFormat
import com.thomasnorlund.archiveplayer.core.catalog.MediaKind

data class PlaybackConditions(val metered: Boolean, val dataSaver: Boolean, val forCast: Boolean)

/** Automatic format choice, quality menu and fallback (R40–R43, R49). */
object FormatSelector {
    private val VIDEO_BEST = listOf("h.264", "h.264 ia", "h.264 hd", "mpeg4", "512kb mpeg4")
    private val VIDEO_SAVER = listOf("512kb mpeg4", "mpeg4", "h.264", "h.264 ia", "h.264 hd")
    private val AUDIO_BEST = listOf("vbr mp3", "128kbps mp3", "mp3", "64kbps mp3", "ogg vorbis")
    private val AUDIO_SAVER = listOf("64kbps mp3", "128kbps mp3", "vbr mp3", "mp3", "ogg vorbis")
    private val CASTABLE = (VIDEO_BEST + AUDIO_BEST + "320kbps mp3" + "256kbps mp3").toSet() - "ogg vorbis"
    private const val OGG = "ogg vorbis"
    private const val FLAC = "flac"

    private fun key(format: MediaFormat) = format.format.trim().lowercase()

    private fun order(entry: Entry, conditions: PlaybackConditions): List<String> {
        val saver = conditions.metered && conditions.dataSaver
        val base = when (entry.kind) {
            MediaKind.VIDEO -> if (saver) VIDEO_SAVER else VIDEO_BEST
            MediaKind.AUDIO -> if (saver) AUDIO_SAVER else AUDIO_BEST
        }
        val withAudio = if (entry.kind == MediaKind.VIDEO) base + AUDIO_BEST else base
        return if (conditions.forCast) withAudio.filter { it != OGG } else withAudio
    }

    private fun candidates(entry: Entry, conditions: PlaybackConditions): List<MediaFormat> =
        order(entry, conditions).flatMap { wanted -> entry.formats.filter { key(it) == wanted } }

    fun select(entry: Entry, conditions: PlaybackConditions): MediaFormat? = candidates(entry, conditions).firstOrNull()

    /** Playable alternatives for the quality menu, best first; FLAC is listed for local playback, Ogg never for Cast. */
    fun alternatives(entry: Entry, forCast: Boolean): List<MediaFormat> {
        val best = candidates(entry, PlaybackConditions(metered = false, dataSaver = false, forCast = forCast))
        val flac = if (forCast) emptyList() else entry.formats.filter { key(it) == FLAC }
        return (best + flac).distinct()
    }

    /** Next suitable format after [failed] failed, or null. */
    fun fallback(entry: Entry, failed: MediaFormat, conditions: PlaybackConditions): MediaFormat? {
        val list = candidates(entry, conditions)
        val i = list.indexOf(failed)
        return if (i < 0) list.firstOrNull { it != failed } else list.getOrNull(i + 1)
    }

    /** True if [format] can be played both locally and on the Default Media Receiver (R09). */
    fun isPlayable(format: MediaFormat): Boolean = key(format) in CASTABLE

    /** True if the app can play [format] on the phone (adds Ogg Vorbis and FLAC). */
    fun isLocallyPlayable(format: MediaFormat): Boolean = isPlayable(format) || key(format) == OGG || key(format) == FLAC
}
