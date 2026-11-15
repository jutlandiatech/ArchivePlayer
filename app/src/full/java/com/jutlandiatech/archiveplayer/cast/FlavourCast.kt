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
            .setRemotePlayer(androidx.media3.cast.RemoteCastPlayer.Builder(application)
                .setMediaItemConverter(ArchiveCastConverter())
                .setTrackSelector(ArchiveCastTrackSelector())
                .build())
            .build()

    override fun castDeviceName(context: android.content.Context): String? =
        runCatching { Cast.getSingletonInstance(context).currentCastSession?.castDevice?.friendlyName }.getOrNull()

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
    override fun toMediaQueueItem(mediaItem: androidx.media3.common.MediaItem): com.google.android.gms.cast.MediaQueueItem {
        val item = forReceiver(mediaItem)
        val queueItem = delegate.toMediaQueueItem(item)
        val info = queueItem.media ?: return queueItem
        // The default converter drops subtitle configurations; the receiver needs them as MediaTracks (R49).
        val tracks = item.localConfiguration?.subtitleConfigurations.orEmpty().mapIndexed { i, s ->
            com.google.android.gms.cast.MediaTrack.Builder((i + 1).toLong(), com.google.android.gms.cast.MediaTrack.TYPE_TEXT)
                .setName(s.label?.toString() ?: s.language ?: "Subtitle ${i + 1}")
                .setSubtype(com.google.android.gms.cast.MediaTrack.SUBTYPE_SUBTITLES)
                .setContentId(s.uri.toString())
                .setContentType(s.mimeType)
                .setLanguage(s.language)
                .build()
        }
        if (tracks.isEmpty()) return queueItem
        val infoBuilder = com.google.android.gms.cast.MediaInfo.Builder(info.contentId ?: return queueItem)
            .setContentType(info.contentType)
            .setStreamType(info.streamType)
            .setStreamDuration(info.streamDuration)
            .setMetadata(info.metadata)
            .setMediaTracks(tracks)
        info.contentUrl?.let { infoBuilder.setContentUrl(it) }
        info.customData?.let { infoBuilder.setCustomData(it) }
        val withTracks = infoBuilder.build()
        val itemBuilder = com.google.android.gms.cast.MediaQueueItem.Builder(withTracks)
            .setAutoplay(queueItem.autoplay)
            .setStartTime(queueItem.startTime)
            .setPlaybackDuration(queueItem.playbackDuration)
            .setPreloadTime(queueItem.preloadTime)
        queueItem.customData?.let { itemBuilder.setCustomData(it) }
        return itemBuilder.build()
    }

    override fun toMediaItem(mediaQueueItem: com.google.android.gms.cast.MediaQueueItem): androidx.media3.common.MediaItem {
        val item = delegate.toMediaItem(mediaQueueItem)
        val subs = mediaQueueItem.media?.mediaTracks.orEmpty()
            .filter { it.type == com.google.android.gms.cast.MediaTrack.TYPE_TEXT && it.subtype == com.google.android.gms.cast.MediaTrack.SUBTYPE_SUBTITLES }
            .mapNotNull { t ->
                t.contentId?.let {
                    androidx.media3.common.MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(it))
                        .setMimeType(t.contentType)
                        .setLanguage(t.language)
                        .setLabel(t.name)
                        .build()
                }
            }
        return if (subs.isEmpty()) item else item.buildUpon().setSubtitleConfigurations(subs).build()
    }

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

/**
 * Turns the app's text-track parameters (player.subtitles sheet) into `setActiveMediaTracks` calls on the
 * receiver: enabled text tracks pick the preferred language, disabled means none (R49).
 */
@OptIn(UnstableApi::class)
class ArchiveCastTrackSelector : androidx.media3.cast.CastTrackSelector() {
    override fun evaluate(request: androidx.media3.cast.CastTrackSelector.CastTrackSelectorRequest): androidx.media3.cast.CastTrackSelector.CastTrackSelectorResult {
        val keep = request.currentlySelectedTrackGroups.filter { it.type != androidx.media3.common.C.TRACK_TYPE_TEXT }
        val chosen = pickTextTrack(request)
        return request.buildResultUpon()
            .setSelections(com.google.common.collect.ImmutableSet.copyOf(keep + listOfNotNull(chosen)))
            .build()
    }

    private fun pickTextTrack(request: androidx.media3.cast.CastTrackSelector.CastTrackSelectorRequest): androidx.media3.common.TrackGroup? {
        val params = request.trackSelectionParameters
        if (params.disabledTrackTypes.contains(androidx.media3.common.C.TRACK_TYPE_TEXT)) return null
        for (lang in params.preferredTextLanguages) {
            val i = request.mediaTracks.indexOfFirst {
                it.type == com.google.android.gms.cast.MediaTrack.TYPE_TEXT && languageMatches(it.language, lang)
            }
            if (i >= 0) return request.trackGroupList[i]
        }
        val first = request.mediaTracks.indexOfFirst { it.type == com.google.android.gms.cast.MediaTrack.TYPE_TEXT }
        return if (first >= 0) request.trackGroupList[first] else null
    }

    private fun languageMatches(trackLanguage: String?, preferred: String?): Boolean {
        if (trackLanguage.isNullOrBlank() || preferred.isNullOrBlank()) return false
        val t = trackLanguage.lowercase()
        val p = preferred.lowercase()
        return t == p || t.startsWith("$p-") || p.startsWith("$t-") || t.substringBefore('-') == p.substringBefore('-')
    }
}
