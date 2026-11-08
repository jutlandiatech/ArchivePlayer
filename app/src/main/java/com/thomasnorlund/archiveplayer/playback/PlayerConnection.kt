package com.thomasnorlund.archiveplayer.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

/** One queued file: identifies the archive entry and how to fetch it. */
data class QueueEntry(
    val identifier: String,
    val fileName: String,
    val title: String,
    val itemTitle: String,
    val creator: String?,
    val url: String,
    val artworkUrl: String?,
    val video: Boolean,
    val subtitles: List<MediaItem.SubtitleConfiguration> = emptyList(),
) {
    fun toMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId("$identifier/$fileName")
        .setUri(url)
        .setSubtitleConfigurations(subtitles)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(creator)
                .setAlbumTitle(itemTitle)
                .setArtworkUri(artworkUrl?.let(Uri::parse))
                .setMediaType(if (video) MediaMetadata.MEDIA_TYPE_VIDEO else MediaMetadata.MEDIA_TYPE_MUSIC)
                .build(),
        )
        .build()
}

/** App-side MediaController to [PlaybackService] with snapshot state for Compose. */
class PlayerConnection(context: Context) {
    private val app = context.applicationContext
    private var controller: MediaController? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()
    private var stopped: Triple<List<MediaItem>, Int, Long>? = null

    var hasQueue by mutableStateOf(false); private set
    var isPlaying by mutableStateOf(false); private set
    var title by mutableStateOf<String?>(null); private set
    var itemTitle by mutableStateOf<String?>(null); private set
    var creator by mutableStateOf<String?>(null); private set
    var currentId by mutableStateOf<String?>(null); private set
    var isVideo by mutableStateOf(false); private set
    var index by mutableIntStateOf(0); private set
    var queue by mutableStateOf<List<MediaItem>>(emptyList()); private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF); private set
    var shuffle by mutableStateOf(false); private set
    var speed by mutableFloatStateOf(1f); private set
    var error by mutableStateOf<PlaybackException?>(null); private set
    var canUndoStop by mutableStateOf(false); private set
    var onItemEnded: ((Player) -> Unit)? = null
    var onStarted: ((String) -> Unit)? = null
    var onError: ((Player, PlaybackException) -> Boolean)? = null

    val player: Player? get() = controller
    val positionMs: Long get() = controller?.currentPosition ?: 0L
    val durationMs: Long get() = controller?.duration?.takeIf { it > 0 } ?: 0L

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.mediaId?.let { onStarted?.invoke(it) }
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) controller?.let { onItemEnded?.invoke(it) }
        }
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) controller?.let { onItemEnded?.invoke(it) }
        }
        override fun onPlayerError(e: PlaybackException) {
            val c = controller ?: return
            if (onError?.invoke(c, e) != true) error = e
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            sync(c)
            pending.toList().forEach { it(c) }
            pending.clear()
        }, ContextCompat.getMainExecutor(app))
    }

    fun withPlayer(block: (MediaController) -> Unit) {
        controller?.let(block) ?: pending.add(block)
    }

    private fun sync(p: Player) {
        hasQueue = p.mediaItemCount > 0
        isPlaying = p.isPlaying || (p.playWhenReady && p.playbackState == Player.STATE_BUFFERING)
        val meta = p.mediaMetadata
        title = meta.title?.toString()
        itemTitle = meta.albumTitle?.toString()
        creator = meta.artist?.toString()
        currentId = p.currentMediaItem?.mediaId
        isVideo = meta.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO
        index = p.currentMediaItemIndex
        queue = (0 until p.mediaItemCount).map { p.getMediaItemAt(it) }
        repeatMode = p.repeatMode
        shuffle = p.shuffleModeEnabled
        speed = p.playbackParameters.speed
        if (p.playerError == null && p.playbackState != Player.STATE_IDLE) error = null
    }

    fun setQueue(entries: List<MediaItem>, start: Int, startMs: Long = 0L) = withPlayer { c ->
        error = null
        canUndoStop = false
        c.setMediaItems(entries, start.coerceIn(0, (entries.size - 1).coerceAtLeast(0)), startMs)
        c.prepare()
        c.play()
    }

    fun add(entries: List<MediaItem>, next: Boolean) = withPlayer { c ->
        when {
            c.mediaItemCount == 0 -> { c.setMediaItems(entries); c.prepare(); c.play() }
            next -> c.addMediaItems(c.currentMediaItemIndex + 1, entries)
            else -> c.addMediaItems(entries)
        }
    }

    fun togglePlay() = withPlayer { if (it.isPlaying || it.playWhenReady && it.playbackState == Player.STATE_BUFFERING) it.pause() else { if (it.playbackState == Player.STATE_ENDED) it.seekTo(0); it.play() } }
    fun pause() = withPlayer { it.pause() }
    fun next() = withPlayer { it.seekToNextMediaItem() }
    /** Restarts the entry after more than 3 s, otherwise goes to the previous entry (UI_PLAYER_SPEC.md). */
    fun previous() = withPlayer { if (it.currentPosition > 3_000 || !it.hasPreviousMediaItem()) it.seekTo(0) else it.seekToPreviousMediaItem() }
    fun skip(deltaMs: Long) = withPlayer { c ->
        val end = c.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        c.seekTo((c.currentPosition + deltaMs).coerceIn(0, end))
    }
    fun seekTo(ms: Long) = withPlayer { it.seekTo(ms) }
    fun cycleRepeat() = withPlayer {
        it.repeatMode = when (it.repeatMode) { Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL; Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE; else -> Player.REPEAT_MODE_OFF }
    }
    fun toggleShuffle() = withPlayer { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun changeSpeed(value: Float) = withPlayer { it.playbackParameters = PlaybackParameters(value) }
    fun jump(i: Int) = withPlayer { it.seekTo(i, 0); it.play() }
    fun move(from: Int, to: Int) = withPlayer { it.moveMediaItem(from, to) }
    fun remove(i: Int) = withPlayer { it.removeMediaItem(i) }
    fun replaceCurrent(item: MediaItem, positionMs: Long) = withPlayer { c ->
        val i = c.currentMediaItemIndex
        c.replaceMediaItem(i, item)
        c.seekTo(i, positionMs)
        c.prepare()
        c.play()
    }
    fun setPauseAtEnd(on: Boolean) = withPlayer {
        it.sendCustomCommand(
            androidx.media3.session.SessionCommand(PlaybackService.CMD_PAUSE_AT_END, android.os.Bundle.EMPTY),
            android.os.Bundle().apply { putBoolean(PlaybackService.ARG_ON, on) },
        )
    }
    fun retry() = withPlayer { error = null; it.prepare(); it.play() }

    /** Swipe-away stop (P18): keeps a snapshot so Undo restores queue and position. */
    fun stop() = withPlayer { c ->
        stopped = Triple(queue, c.currentMediaItemIndex, c.currentPosition)
        c.stop()
        c.clearMediaItems()
        canUndoStop = true
    }

    fun undoStop() {
        val (items, i, pos) = stopped ?: return
        stopped = null
        canUndoStop = false
        setQueue(items, i, pos)
    }

    fun dismissUndo() { stopped = null; canUndoStop = false }
}
