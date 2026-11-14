package com.jutlandiatech.archiveplayer

import androidx.media3.common.MimeTypes
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jutlandiatech.archiveplayer.cast.ArchiveCastConverter
import com.jutlandiatech.archiveplayer.playback.QueueEntry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** A48: the receiver gets https URLs with MIME types and only WebVTT subtitles (R46). */
@RunWith(AndroidJUnit4::class)
class CastConverterTest {
    private fun subtitle(mime: String) = androidx.media3.common.MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse("https://archive.org/download/x/s"))
        .setMimeType(mime).build()

    @Test fun A48_downloadedMp3IsStreamedWithMime() {
        val entry = QueueEntry("fx_radio_show", "e01.mp3", "E01", "Show", null, "file:///data/x/e01.mp3", null, false,
            mimeType = MimeTypes.AUDIO_MPEG, castUrl = "https://archive.org/download/fx_radio_show/e01.mp3")
        val item = ArchiveCastConverter.forReceiver(entry.toMediaItem())
        assertEquals("https://archive.org/download/fx_radio_show/e01.mp3", item.localConfiguration!!.uri.toString())
        assertEquals(MimeTypes.AUDIO_MPEG, item.localConfiguration!!.mimeType)
        ArchiveCastConverter().toMediaQueueItem(entry.toMediaItem()).media!!.let {
            assertEquals("https://archive.org/download/fx_radio_show/e01.mp3", it.contentId.ifEmpty { it.contentUrl })
            assertEquals(MimeTypes.AUDIO_MPEG, it.contentType)
        }
    }

    @Test fun A48_streamedMp4KeepsOnlyWebVtt() {
        val entry = QueueEntry("fx_film", "fx_film.mp4", "Film", "Film", null, "https://archive.org/download/fx_film/fx_film.mp4", null, true,
            listOf(subtitle(MimeTypes.APPLICATION_SUBRIP), subtitle(MimeTypes.TEXT_VTT)), MimeTypes.VIDEO_MP4, "https://archive.org/download/fx_film/fx_film.mp4")
        val item = ArchiveCastConverter.forReceiver(entry.toMediaItem())
        assertEquals(MimeTypes.VIDEO_MP4, item.localConfiguration!!.mimeType)
        assertEquals(listOf(MimeTypes.TEXT_VTT), item.localConfiguration!!.subtitleConfigurations.map { it.mimeType })
    }
}
