package com.thomasnorlund.archiveplayer.cast

import android.app.Application
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.cast.Cast
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.MediaRouteButton
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

@OptIn(UnstableApi::class)
object FlavourCast : CastSupport {
    override val isAvailable = true

    override fun initialize(application: Application) {
        Cast.getSingletonInstance(application).initialize()
    }

    // CastPlayer plays on the local ExoPlayer and hands playback over when a Cast session starts or ends.
    override fun wrapPlayer(application: Application, localPlayer: ExoPlayer): Player =
        CastPlayer.Builder(application).setLocalPlayer(localPlayer).build()

    @Composable
    override fun RouteButton(modifier: Modifier) = MediaRouteButton(modifier)
}
