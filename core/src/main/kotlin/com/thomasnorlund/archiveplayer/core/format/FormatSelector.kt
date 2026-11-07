package com.thomasnorlund.archiveplayer.core.format

import com.thomasnorlund.archiveplayer.core.catalog.Entry
import com.thomasnorlund.archiveplayer.core.catalog.MediaFormat

data class PlaybackConditions(val metered: Boolean, val dataSaver: Boolean, val forCast: Boolean)

/** Automatic format choice, quality menu and fallback (R40–R43, R49). */
object FormatSelector {
    fun select(entry: Entry, conditions: PlaybackConditions): MediaFormat? = TODO()

    /** Playable alternatives for the quality menu, best first; FLAC is listed for local playback, Ogg never for Cast. */
    fun alternatives(entry: Entry, forCast: Boolean): List<MediaFormat> = TODO()

    /** Next suitable format after [failed] failed, or null. */
    fun fallback(entry: Entry, failed: MediaFormat, conditions: PlaybackConditions): MediaFormat? = TODO()

    /** True if [format] can be played both locally and on the Default Media Receiver (R09). */
    fun isPlayable(format: MediaFormat): Boolean = TODO()
}
