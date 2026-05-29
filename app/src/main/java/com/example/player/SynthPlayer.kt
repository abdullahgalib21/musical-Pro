package com.example.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioFormat.CHANNEL_OUT_MONO
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.sin

class SynthPlayer {
    private val synthLock = Any()
    private var activeTrack: AudioTrack? = null
    private var synthJob: Job? = null
    private val sampleRate = 44100

    fun playSequence(title: String, preset: String = "Normal", onProgressUpdate: (Long) -> Unit) {
        stop()

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            CHANNEL_OUT_MONO,
            ENCODING_PCM_16BIT
        ).let { if (it <= 0) 4096 else it }

        synthJob = CoroutineScope(Dispatchers.Default).launch {
            var localTrack: AudioTrack? = null
            try {
                localTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                synchronized(synthLock) {
                    if (!isActive) {
                        return@launch
                    }
                    activeTrack = localTrack
                }

                localTrack.play()

                var sampleIndex = 0L
                var elapsedMs = 0L

                // Beautiful tranquil pentatonic-adjacent solfeggio chords or sequences
                val frequencies = when {
                    title.contains("Solitude", ignoreCase = true) -> doubleArrayOf(528.00, 396.00, 440.00, 528.00, 639.00) // Solfeggio 528Hz and heart tones
                    title.contains("Zen", ignoreCase = true) -> doubleArrayOf(293.66, 329.63, 392.00, 440.00, 587.33) // D, E, G, A, D - tranquil Japanese scale
                    title.contains("Sleep", ignoreCase = true) -> doubleArrayOf(220.00, 261.63, 329.63, 392.00, 329.63) // Slumber harmonies
                    else -> doubleArrayOf(349.23, 440.00, 523.25, 587.33, 659.25) // Dreamy custom synthwave-y minor-seventh arpeggio
                }

                val buffer = ShortArray(1024)
                while (isActive) {
                    val seconds = sampleIndex / sampleRate.toDouble()
                    val noteDuration = 2.5 // Hold each note longer for an ambient synth feel
                    val noteIndex = (seconds / noteDuration).toInt() % frequencies.size
                    
                    // Primary melody frequency
                    val frequency = frequencies[noteIndex]
                    
                    // Equalizer wave shaping configs
                    var harmonyFreq = frequency * 0.75
                    var volumeScale = 1.0
                    var primaryWeight = 0.65
                    var harmonyWeight = 0.35
                    var useExtra = false
                    var extraHarmonyFreq = 0.0
                    var extraHarmonyWeight = 0.0

                    when (preset) {
                        "Bass Boost" -> {
                            harmonyFreq = frequency * 0.5
                            volumeScale = 1.4
                            primaryWeight = 0.50
                            harmonyWeight = 0.50
                        }
                        "Acoustic" -> {
                            harmonyFreq = frequency * 0.75
                            volumeScale = 1.0
                            primaryWeight = 0.50
                            harmonyWeight = 0.25
                            useExtra = true
                            extraHarmonyFreq = frequency * 1.5
                            extraHarmonyWeight = 0.25
                        }
                        "Vocal Booster" -> {
                            harmonyFreq = frequency * 0.75
                            volumeScale = 1.1
                            primaryWeight = 0.85
                            harmonyWeight = 0.15
                        }
                        "Classical" -> {
                            harmonyFreq = frequency * 0.75
                            volumeScale = 0.9
                            primaryWeight = 0.60
                            harmonyWeight = 0.40
                        }
                    }
                    
                    // LFO (Low Frequency Oscillator) to model fluid ambient wave oscillations
                    val lfoSpeed = 0.4 // slow swell
                    val lfoVal = 0.4 + 0.6 * sin(2.0 * Math.PI * lfoSpeed * seconds)
                    val amplitude = 12000.0 * lfoVal

                    for (i in buffer.indices) {
                        val currentSampleIndex = sampleIndex + i
                        val angle = 2.0 * Math.PI * frequency * currentSampleIndex / sampleRate
                        val angleHarmony = 2.0 * Math.PI * harmonyFreq * currentSampleIndex / sampleRate
                        
                        var synthValue = (sin(angle) * primaryWeight) + (sin(angleHarmony) * harmonyWeight)
                        if (useExtra) {
                            val angleExtra = 2.0 * Math.PI * extraHarmonyFreq * currentSampleIndex / sampleRate
                            synthValue += sin(angleExtra) * extraHarmonyWeight
                        }
                        buffer[i] = (synthValue * amplitude * volumeScale).toInt().toShort()
                    }

                    sampleIndex += buffer.size
                    
                    if (localTrack.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        try {
                            localTrack.write(buffer, 0, buffer.size)
                        } catch (e: Exception) {
                            break
                        }
                    } else {
                        break
                    }

                    val currentElapsed = (sampleIndex * 1000) / sampleRate
                    if (currentElapsed > elapsedMs + 500) {
                        elapsedMs = currentElapsed
                        withContext(Dispatchers.Main) {
                            onProgressUpdate(elapsedMs)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(NonCancellable) {
                    synchronized(synthLock) {
                        if (activeTrack == localTrack) {
                            activeTrack = null
                        }
                    }
                    try {
                        localTrack?.let {
                            if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                                it.stop()
                            }
                            it.release()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun stop() {
        synchronized(synthLock) {
            synthJob?.cancel()
            synthJob = null
            try {
                activeTrack?.let {
                    if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        it.stop()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
