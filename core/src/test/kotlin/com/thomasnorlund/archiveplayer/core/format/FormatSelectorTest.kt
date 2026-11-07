package com.thomasnorlund.archiveplayer.core.format

import com.thomasnorlund.archiveplayer.core.catalog.Entry
import com.thomasnorlund.archiveplayer.core.catalog.MediaFormat
import com.thomasnorlund.archiveplayer.core.catalog.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatSelectorTest {
    private fun f(name: String, format: String) = MediaFormat(name, format, null)
    private fun entry(kind: MediaKind, vararg formats: MediaFormat) = Entry("id", 0, "t", kind, 600.0, formats.toList(), emptyList())

    private val h264 = f("a.mp4", "h.264")
    private val mp4Low = f("a_512kb.mp4", "512Kb MPEG4")
    private val video = entry(MediaKind.VIDEO, f("a.mpeg", "MPEG2"), mp4Low, f("a.ogv", "Ogg Video"), h264)

    private val vbr = f("t.mp3", "VBR MP3")
    private val mp3128 = f("t_128kb.mp3", "128Kbps MP3")
    private val mp364 = f("t_64kb.mp3", "64Kbps MP3")
    private val ogg = f("t.ogg", "Ogg Vorbis")
    private val flac = f("t.flac", "Flac")
    private val audio = entry(MediaKind.AUDIO, flac, ogg, mp3128, vbr)

    private val wifi = PlaybackConditions(metered = false, dataSaver = true, forCast = false)
    private val meteredSaver = PlaybackConditions(metered = true, dataSaver = true, forCast = false)

    @Test fun A14_wifiPicksH264() = assertEquals(h264, FormatSelector.select(video, wifi))

    @Test fun A13_meteredWithDataSaverPicksLowBitrateVideo() = assertEquals(mp4Low, FormatSelector.select(video, meteredSaver))

    @Test fun R41_dataSaverOffOnMeteredKeepsBestFormat() =
        assertEquals(h264, FormatSelector.select(video, PlaybackConditions(metered = true, dataSaver = false, forCast = false)))

    @Test fun A13_qualityMenuListsBothVideoFormats() {
        val alternatives = FormatSelector.alternatives(video, forCast = false)
        assertEquals(listOf(h264, mp4Low), alternatives.take(2))
        assertFalse(alternatives.any { it.format == "MPEG2" })
    }

    @Test fun A15_vbrMp3SelectedAndFlacNeverAuto() {
        assertEquals(vbr, FormatSelector.select(audio, wifi))
        assertTrue(flac in FormatSelector.alternatives(audio, forCast = false))
        assertEquals(ogg, FormatSelector.select(entry(MediaKind.AUDIO, flac, ogg), wifi))
    }

    @Test fun R40_audioPreferenceOrder() {
        assertEquals(mp3128, FormatSelector.select(entry(MediaKind.AUDIO, ogg, mp3128), wifi))
    }

    @Test fun R41_meteredDataSaverPicksLowestMp3() =
        assertEquals(mp364, FormatSelector.select(entry(MediaKind.AUDIO, vbr, mp3128, mp364, ogg), meteredSaver))

    @Test fun R49_oggNeverSentToCast() {
        val cast = PlaybackConditions(metered = false, dataSaver = true, forCast = true)
        assertNull(FormatSelector.select(entry(MediaKind.AUDIO, flac, ogg), cast))
        assertEquals(mp3128, FormatSelector.select(entry(MediaKind.AUDIO, ogg, mp3128), cast))
        assertFalse(FormatSelector.alternatives(audio, forCast = true).contains(ogg))
    }

    @Test fun R43_fallbackToNextSuitableFormatOnce() {
        assertEquals(mp4Low, FormatSelector.fallback(video, h264, wifi))
        assertNull(FormatSelector.fallback(entry(MediaKind.VIDEO, h264), h264, wifi))
        assertEquals(mp3128, FormatSelector.fallback(audio, vbr, wifi))
    }

    @Test fun R09_playableFormats() {
        listOf(h264, mp4Low, vbr, mp3128).forEach { assertTrue(it.format, FormatSelector.isPlayable(it)) }
        listOf(f("a.mpeg", "MPEG2"), f("a.avi", "Cinepack"), f("a.png", "PNG"), f("a.txt", "Text"))
            .forEach { assertFalse(it.format, FormatSelector.isPlayable(it)) }
    }
}
