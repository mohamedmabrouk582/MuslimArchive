package com.mabrouk.core.utils

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri

@AndroidEntryPoint
class PlayerMediaSession : MediaSessionService() {

    private val serviceJob = SupervisorJob()
    val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    @Inject
    lateinit var player: ExoPlayer

    @Inject
    lateinit var dataStore: DataStorePreferences
    var mediaSession: MediaSession? = null

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            mediaItem?.let {
                serviceScope.launch {
                    dataStore.setString(MEDIA_URL, it.localConfiguration?.uri.toString())

                }
            }


        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            if (!isPlaying) {
                serviceScope.launch {
                    dataStore.setLong(MEDIA_POSITION, player.currentPosition)
                }
            }
        }
    }


    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val pendingIntent =
            PendingIntent.getActivity(
                this,
                123,
                Intent(this, getClassFromMetaData(this, "MAIN_ACTIVITY")),
                PendingIntent.FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
            )

        player.addListener(listener)

        mediaSession =
            MediaSession.Builder(this, player)
                .setSessionActivity(
                    pendingIntent
                )
                .setPeriodicPositionUpdateEnabled(true)
                .setCallback(object : MediaSession.Callback {
                    override fun onPlaybackResumption(
                        mediaSession: MediaSession,
                        controller: MediaSession.ControllerInfo
                    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {

                        val settable =
                            SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
                        serviceScope.launch {
                            val url = dataStore.getString(MEDIA_URL)

                            Log.d("efeegefefe",url)

                            val position =
                                dataStore.getLong(MEDIA_POSITION)
                            val mediaItem = MediaItem.Builder().setUri(url.toUri()).build()
                            serviceScope.launch {
                                settable.set(
                                    MediaSession.MediaItemsWithStartPosition(
                                        listOf(mediaItem),
                                        0,
                                        position
                                    )
                                )
                            }
                            mediaSession.player.prepare()
                            mediaSession.player.play()
                        }

                        return settable
                    }
                })
                .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun unbindService(conn: ServiceConnection) {
        super.unbindService(conn)
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player?.playWhenReady == true) {
            player.pause()
        }
        if (player?.playWhenReady == false || player?.mediaItemCount == 0 || player?.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.removeListener(listener)
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

const val MEDIA_URL = "MEDIA_URL"
const val MEDIA_POSITION = "MEDIA_POSITION"