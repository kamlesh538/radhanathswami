package com.radhanathswami.app.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.radhanathswami.app.data.model.AudioItem
import com.radhanathswami.app.service.AudioPlayerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PlayerState(
    val currentAudio: AudioItem? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isLoading: Boolean = false
)

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isLoading = playbackState == Player.STATE_BUFFERING
            val durationMs = controller?.duration?.takeIf { it > 0 } ?: 0L
            _playerState.value = _playerState.value.copy(isLoading = isLoading, durationMs = durationMs)
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _playerState.value = _playerState.value.copy(currentPositionMs = newPosition.positionMs)
        }
    }

    fun connect() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, AudioPlayerService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            controller?.addListener(playerListener)
        }, { it.run() })
    }

    fun disconnect() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }

    fun play(audio: AudioItem, localPath: String? = null) {
        val uri = localPath?.let { "file://$it" } ?: audio.url
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(audio.title)
                    .setArtist("Radhanath Swami")
                    .build()
            )
            .build()
        controller?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
        _playerState.value = _playerState.value.copy(
            currentAudio = audio,
            isPlaying = true,
            currentPositionMs = 0L
        )
    }

    fun playPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
    }

    fun skipForward() {
        controller?.let { seekTo((it.currentPosition + 15_000).coerceAtMost(it.duration)) }
    }

    fun skipBackward() {
        controller?.let { seekTo((it.currentPosition - 15_000).coerceAtLeast(0)) }
    }

    fun getCurrentPosition(): Long = controller?.currentPosition ?: 0L

    fun getDuration(): Long = controller?.duration?.takeIf { it > 0 } ?: 0L
}
