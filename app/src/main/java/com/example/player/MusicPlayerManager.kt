package com.example.player

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.example.data.TrackEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val synthPlayer = SynthPlayer()
    var activePreset: String = "Normal"

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private var progressJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var onTrackCompletedListener: (() -> Unit)? = null

    fun play(track: TrackEntity, preset: String = "Normal") {
        this.activePreset = preset
        stopCurrentPlayback()
        _currentTrack.value = track

        if (track.isDemo) {
            _duration.value = track.duration
            _isPlaying.value = true
            synthPlayer.playSequence(track.title, activePreset) { pos ->
                if (pos >= track.duration) {
                    synthPlayer.stop()
                    _isPlaying.value = false
                    _currentPosition.value = 0L
                    onTrackCompletedListener?.invoke()
                } else {
                    _currentPosition.value = pos
                }
            }
        } else {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, Uri.parse(track.uriString))
                    prepare()
                    start()
                    _duration.value = duration.toLong()
                    _isPlaying.value = true

                    setOnCompletionListener {
                        _isPlaying.value = false
                        stopProgressTracking()
                        onTrackCompletedListener?.invoke()
                    }
                }
                startProgressTracking()
            } catch (e: Exception) {
                e.printStackTrace()
                // If local playback fails, fallback or stop
                _isPlaying.value = false
            }
        }
    }

    fun pause() {
        val track = _currentTrack.value ?: return
        if (track.isDemo) {
            synthPlayer.stop()
            _isPlaying.value = false
        } else {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying.value = false
                    stopProgressTracking()
                }
            }
        }
    }

    fun resume() {
        val track = _currentTrack.value ?: return
        if (_isPlaying.value) return

        if (track.isDemo) {
            _isPlaying.value = true
            synthPlayer.playSequence(track.title, activePreset) { pos ->
                if (pos >= track.duration) {
                    synthPlayer.stop()
                    _isPlaying.value = false
                    _currentPosition.value = 0L
                    onTrackCompletedListener?.invoke()
                } else {
                    _currentPosition.value = pos
                }
            }
        } else {
            mediaPlayer?.let {
                try {
                    it.start()
                    _isPlaying.value = true
                    startProgressTracking()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stop() {
        stopCurrentPlayback()
        _currentTrack.value = null
    }

    fun seekTo(positionMs: Long) {
        val track = _currentTrack.value ?: return
        if (track.isDemo) {
            // Synth player doesn't easily seek mid-buffer, so we update the position holder
            _currentPosition.value = positionMs
        } else {
            mediaPlayer?.let {
                try {
                    it.seekTo(positionMs.toInt())
                    _currentPosition.value = positionMs
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun stopCurrentPlayback() {
        stopProgressTracking()
        synthPlayer.stop()
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaPlayer = null
        }
        _isPlaying.value = false
        _currentPosition.value = 0L
        _duration.value = 0L
    }

    private fun startProgressTracking() {
        stopProgressTracking()
        progressJob = coroutineScope.launch {
            while (isActive) {
                try {
                    mediaPlayer?.let {
                        if (it.isPlaying) {
                            _currentPosition.value = it.currentPosition.toLong()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore exceptions during state transitions
                }
                delay(250)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }
}
