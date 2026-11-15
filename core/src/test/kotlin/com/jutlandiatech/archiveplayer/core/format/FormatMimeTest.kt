package com.jutlandiatech.archiveplayer.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Cast receivers need each item's MIME type (R46, A48). */
class FormatMimeTest {
    @Test fun R46_videoFormatsAreMp4() {
        listOf("h.264", "h.264 IA", "h.264 HD", "MPEG4", "512Kb MPEG4").forEach { assertEquals(it, "video/mp4", FormatMime.of(it)) }
    }

    @Test fun R46_audioFormats() {
        listOf("VBR MP3", "128Kbps MP3", "MP3", "64Kbps MP3").forEach { assertEquals(it, "audio/mpeg", FormatMime.of(it)) }
        assertEquals("audio/ogg", FormatMime.of("Ogg Vorbis"))
        assertEquals("audio/flac", FormatMime.of("Flac"))
    }

    @Test fun R46_unknownFormatHasNoMime() {
        assertNull(FormatMime.of("Ogg Video"))
        assertNull(FormatMime.of("Item Tile"))
    }
}
