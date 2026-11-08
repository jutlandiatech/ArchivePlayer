package com.jutlandiatech.archiveplayer.cast

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/** Cast boundary (R81): `full` wraps the local player in a CastPlayer, `foss` is a no-op. Implemented by `FlavourCast` per flavour. */
interface CastSupport {
    val isAvailable: Boolean
    fun initialize(application: Application)
    fun wrapPlayer(application: Application, localPlayer: ExoPlayer): Player

    @Composable
    fun RouteButton(modifier: Modifier)
}
