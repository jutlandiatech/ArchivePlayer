package com.jutlandiatech.archiveplayer.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import android.os.Bundle
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.jutlandiatech.archiveplayer.MainActivity
import com.jutlandiatech.archiveplayer.cast.FlavourCast

/** Media3 session service: notification, lock-screen and external controllers (UI_PLAYER_SPEC.md R34). */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val attributes = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build()
        val local = ExoPlayer.Builder(this)
            .setAudioAttributes(attributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        // R46/R81: CastPlayer moves the queue to the receiver when a Cast session starts and stops local playback.
        val player = FlavourCast.wrapPlayer(application, local)
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(open)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult =
                    MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(
                            MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                                .add(SessionCommand(CMD_PAUSE_AT_END, Bundle.EMPTY)).build(),
                        ).build()

                override fun onCustomCommand(
                    session: MediaSession, controller: MediaSession.ControllerInfo, customCommand: SessionCommand, args: Bundle,
                ): ListenableFuture<SessionResult> {
                    if (customCommand.customAction != CMD_PAUSE_AT_END) return super.onCustomCommand(session, controller, customCommand, args)
                    local.pauseAtEndOfMediaItems = args.getBoolean(ARG_ON)
                    android.util.Log.i("ArchivePlayer", "pauseAtEnd=${local.pauseAtEndOfMediaItems}")
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            })
            .build()
    }

    companion object {
        /** Sleep timer "end of entry" (A34): ExoPlayer pauses exactly at the end of the current item. */
        const val CMD_PAUSE_AT_END = "archiveplayer.PAUSE_AT_END"
        const val ARG_ON = "on"
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run { player.release(); release() }
        session = null
        super.onDestroy()
    }
}
