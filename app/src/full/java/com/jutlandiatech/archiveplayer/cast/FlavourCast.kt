package com.jutlandiatech.archiveplayer.cast

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

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
        CastPlayer.Builder(application)
            .setLocalPlayer(localPlayer)
            .setRemotePlayer(androidx.media3.cast.RemoteCastPlayer.Builder(application).setMediaItemConverter(ArchiveCastConverter()).build())
            .build()

    /**
     * Own route button: media3-cast's composable removes its MediaRouter callback from a coroutine that can resume
     * off the main thread when the composition is torn down, which MediaRouter rejects.
     */
    @Composable
    override fun RouteButton(modifier: Modifier) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val router = androidx.compose.runtime.remember { androidx.mediarouter.media.MediaRouter.getInstance(context) }
        val selector = androidx.compose.runtime.remember {
            androidx.mediarouter.media.MediaRouteSelector.Builder()
                .addControlCategory(com.google.android.gms.cast.CastMediaControlIntent.categoryForCast(
                    com.google.android.gms.cast.CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
                .build()
        }
        var connected by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(!router.selectedRoute.isDefault) }
        androidx.compose.runtime.DisposableEffect(router) {
            val callback = object : androidx.mediarouter.media.MediaRouter.Callback() {
                override fun onRouteSelected(r: androidx.mediarouter.media.MediaRouter, route: androidx.mediarouter.media.MediaRouter.RouteInfo, reason: Int) { connected = !route.isDefault }
                override fun onRouteUnselected(r: androidx.mediarouter.media.MediaRouter, route: androidx.mediarouter.media.MediaRouter.RouteInfo, reason: Int) { connected = false }
            }
            router.addCallback(selector, callback)
            onDispose { android.os.Handler(android.os.Looper.getMainLooper()).post { router.removeCallback(callback) } }
        }
        val label = com.jutlandiatech.archiveplayer.ui.t("nav.cast")
        androidx.compose.material3.IconButton(
            onClick = {
                val themed = android.view.ContextThemeWrapper(context, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog)
                if (router.selectedRoute.isDefault) androidx.mediarouter.app.MediaRouteChooserDialog(themed).apply { routeSelector = selector }.show()
                else androidx.mediarouter.app.MediaRouteControllerDialog(themed).show()
            },
            modifier = modifier.androidxSemantics(label),
        ) { androidx.compose.material3.Text(if (connected) "📡" else "⎚") }
    }
}

private fun Modifier.androidxSemantics(label: String): Modifier =
    this.then(Modifier.semantics { contentDescription = label })

/**
 * Receiver-ready items (R46): the https archive.org URL instead of a downloaded local file, the MIME type set by the app,
 * and only WebVTT subtitles, which is all the Default Media Receiver can render.
 */
@OptIn(UnstableApi::class)
class ArchiveCastConverter(private val delegate: androidx.media3.cast.DefaultMediaItemConverter = androidx.media3.cast.DefaultMediaItemConverter()) :
    androidx.media3.cast.MediaItemConverter {
    override fun toMediaQueueItem(mediaItem: androidx.media3.common.MediaItem): com.google.android.gms.cast.MediaQueueItem =
        delegate.toMediaQueueItem(forReceiver(mediaItem))

    override fun toMediaItem(mediaQueueItem: com.google.android.gms.cast.MediaQueueItem): androidx.media3.common.MediaItem =
        delegate.toMediaItem(mediaQueueItem)

    companion object {
        fun forReceiver(item: androidx.media3.common.MediaItem): androidx.media3.common.MediaItem {
            val config = item.localConfiguration ?: return item
            val castUrl = item.mediaMetadata.extras?.getString(com.jutlandiatech.archiveplayer.playback.CAST_URL)
            return item.buildUpon()
                .setUri(castUrl ?: config.uri.toString())
                .setMimeType(config.mimeType)
                .setSubtitleConfigurations(config.subtitleConfigurations.filter { it.mimeType == androidx.media3.common.MimeTypes.TEXT_VTT })
                .build()
        }
    }
}
