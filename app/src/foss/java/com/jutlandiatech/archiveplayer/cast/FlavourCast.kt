package com.jutlandiatech.archiveplayer.cast

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/** No Cast in the F-Droid build (R81): local playback only, no route button. */
object FlavourCast : CastSupport {
    override val isAvailable = false
    override fun initialize(application: Application) = Unit
    override fun wrapPlayer(application: Application, localPlayer: ExoPlayer): Player = localPlayer

    @Composable
    override fun RouteButton(modifier: Modifier) = Unit
}
