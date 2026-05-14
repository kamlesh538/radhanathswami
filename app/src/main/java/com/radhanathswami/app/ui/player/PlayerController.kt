package com.radhanathswami.app.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.radhanathswami.app.data.local.HistoryDao
import com.radhanathswami.app.data.local.HistoryEntity
import com.radhanathswami.app.data.model.AudioItem
import com.radhanathswami.app.service.AudioPlayerService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    @ApplicationContext private val context: Context,
    private val historyDao: HistoryDao,
    private val prefs: SharedPreferences
) {
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var positionJob: Job? = null

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val wantsToPlay = controller?.playWhenReady ?: false
            _playerState.value = _playerState.value.copy(
                isPlaying = isPlaying,
                // buffering: wants to play but not playing yet
                isLoading = !isPlaying && wantsToPlay
            )
            if (isPlaying) {
                startPositionUpdates()
            } else {
                stopPositionUpdates()
                persistProgress()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            // Only update duration here; isLoading is driven by onIsPlayingChanged + play()
            val durationMs = controller?.duration?.takeIf { it > 0 } ?: 0L
            if (durationMs > 0) {
                _playerState.value = _playerState.value.copy(durationMs = durationMs)
            }
            if (playbackState == Player.STATE_READY && durationMs > 0) {
                _playerState.value.currentAudio?.let { audio ->
                    scope.launch {
                        historyDao.updateProgress(
                            audio.id,
                            controller?.currentPosition ?: 0L,
                            durationMs,
                            System.currentTimeMillis()
                        )
                    }
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            // Only update while playing; paused seeks go through seekTo() which updates state directly.
            // Ignoring here prevents the async setMediaItem reset from wiping the restored position.
            if (_playerState.value.isPlaying) {
                _playerState.value = _playerState.value.copy(currentPositionMs = newPosition.positionMs)
            }
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
            restoreLastSession()
        }, { it.run() })
    }

    fun disconnect() {
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
    }

    fun play(audio: AudioItem, localPath: String? = null) {
        val uri = (localPath ?: audio.localPath)?.let { "file://$it" } ?: audio.url
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
            isPlaying = false,
            isLoading = true,
            currentPositionMs = 0L
        )
        saveAudioToPrefs(audio, 0L)
        scope.launch {
            historyDao.upsert(
                HistoryEntity(
                    id = audio.id,
                    title = audio.title,
                    url = audio.url,
                    category = audio.category,
                    date = audio.date,
                    localPath = localPath ?: audio.localPath,
                    lastPositionMs = 0L,
                    lastPlayedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun playFromPosition(audio: AudioItem, positionMs: Long) {
        val uri = audio.localPath?.let { "file://$it" } ?: audio.url
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
            setMediaItem(mediaItem, positionMs)
            prepare()
            play()
        }
        _playerState.value = _playerState.value.copy(
            currentAudio = audio,
            isPlaying = false,
            isLoading = true,
            currentPositionMs = positionMs
        )
        saveAudioToPrefs(audio, positionMs)
        scope.launch {
            historyDao.upsert(
                HistoryEntity(
                    id = audio.id,
                    title = audio.title,
                    url = audio.url,
                    category = audio.category,
                    date = audio.date,
                    localPath = audio.localPath,
                    lastPositionMs = positionMs,
                    lastPlayedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun playPause() {
        val c = controller ?: return
        // Use playWhenReady so pause works correctly even during buffering
        if (c.playWhenReady) c.pause() else c.play()
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

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch {
            var saveTickCounter = 0
            while (true) {
                val pos = controller?.currentPosition ?: 0L
                val dur = controller?.duration?.takeIf { it > 0 } ?: 0L
                _playerState.value = _playerState.value.copy(
                    currentPositionMs = pos,
                    durationMs = dur
                )
                if (++saveTickCounter >= 10) {
                    saveTickCounter = 0
                    persistProgress()
                }
                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    private fun persistProgress() {
        val audio = _playerState.value.currentAudio ?: return
        val pos = controller?.currentPosition ?: _playerState.value.currentPositionMs
        val dur = controller?.duration?.takeIf { it > 0 } ?: _playerState.value.durationMs
        prefs.edit()
            .putLong(PREF_POSITION, pos)
            .putLong(PREF_DURATION, dur)
            .apply()
        scope.launch {
            historyDao.updateProgress(audio.id, pos, dur, System.currentTimeMillis())
        }
    }

    private fun restoreLastSession() {
        val id = prefs.getString(PREF_AUDIO_ID, null) ?: return
        val title = prefs.getString(PREF_AUDIO_TITLE, null) ?: return
        val url = prefs.getString(PREF_AUDIO_URL, null) ?: return
        val positionMs = prefs.getLong(PREF_POSITION, 0L)

        val audio = AudioItem(
            id = id,
            title = title,
            url = url,
            category = prefs.getString(PREF_AUDIO_CATEGORY, "") ?: "",
            date = prefs.getString(PREF_AUDIO_DATE, "") ?: "",
            localPath = prefs.getString(PREF_AUDIO_LOCAL_PATH, null)
        )

        val uri = audio.localPath?.let { "file://$it" } ?: audio.url
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
            setMediaItem(mediaItem, positionMs)
            prepare()
            // Don't call play() — show MiniPlayer, let user resume manually
        }
        _playerState.value = _playerState.value.copy(
            currentAudio = audio,
            isPlaying = false,
            currentPositionMs = positionMs,
            durationMs = prefs.getLong(PREF_DURATION, 0L)
        )
    }

    private fun saveAudioToPrefs(audio: AudioItem, positionMs: Long) {
        prefs.edit()
            .putString(PREF_AUDIO_ID, audio.id)
            .putString(PREF_AUDIO_TITLE, audio.title)
            .putString(PREF_AUDIO_URL, audio.url)
            .putString(PREF_AUDIO_CATEGORY, audio.category)
            .putString(PREF_AUDIO_DATE, audio.date)
            .putString(PREF_AUDIO_LOCAL_PATH, audio.localPath)
            .putLong(PREF_POSITION, positionMs)
            .apply()
    }

    companion object {
        private const val PREF_AUDIO_ID = "last_audio_id"
        private const val PREF_AUDIO_TITLE = "last_audio_title"
        private const val PREF_AUDIO_URL = "last_audio_url"
        private const val PREF_AUDIO_CATEGORY = "last_audio_category"
        private const val PREF_AUDIO_DATE = "last_audio_date"
        private const val PREF_AUDIO_LOCAL_PATH = "last_audio_local_path"
        private const val PREF_POSITION = "last_position_ms"
        private const val PREF_DURATION = "last_duration_ms"
    }
}
