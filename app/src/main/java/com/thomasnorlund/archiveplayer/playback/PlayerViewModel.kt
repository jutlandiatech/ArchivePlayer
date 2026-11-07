package com.thomasnorlund.archiveplayer.playback

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.thomasnorlund.archiveplayer.cast.FlavourCast

@OptIn(UnstableApi::class)
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    val player: Player = FlavourCast.wrapPlayer(application, ExoPlayer.Builder(application).build())

    init {
        player.setMediaItem(SAMPLE_ITEM)
        player.prepare()
    }

    override fun onCleared() {
        player.release()
    }

    companion object {
        val SAMPLE_ITEM: MediaItem = MediaItem.Builder()
            .setUri("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4")
            .setMimeType(MimeTypes.VIDEO_MP4)
            .setMediaMetadata(MediaMetadata.Builder().setTitle("Big Buck Bunny").build())
            .build()
    }
}
