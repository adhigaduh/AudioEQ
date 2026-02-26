package com.audioeq.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log

class AudioOutput(
    context: Context,
    private val sampleRate: Int = 44100,
    private val channelConfig: Int = AudioFormat.CHANNEL_OUT_STEREO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    
    private val bufferSize: Int by lazy {
        val minSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        maxOf(minSize * 2, 4096)
    }
    
    fun start(): Boolean {
        if (isPlaying) return true
        
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            
            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "AudioTrack not initialized")
                return false
            }
            
            audioTrack?.play()
            isPlaying = true
            
            Log.d(TAG, "Audio output started")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio output", e)
            return false
        }
    }
    
    fun write(buffer: ShortArray): Int {
        if (!isPlaying) return -1
        return audioTrack?.write(buffer, 0, buffer.size) ?: -1
    }
    
    fun stop() {
        isPlaying = false
        
        audioTrack?.apply {
            try {
                // Flush any remaining audio data
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    flush()
                }
                pause()
                stop()
                release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping AudioTrack", e)
            }
        }
        audioTrack = null
        
        Log.d(TAG, "Audio output stopped")
    }
    
    fun flush() {
        audioTrack?.flush()
    }
    
    fun isPlaying(): Boolean = isPlaying && audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING
    
    fun setVolume(volume: Float) {
        audioTrack?.setVolume(volume)
    }
    
    companion object {
        private const val TAG = "AudioOutput"
    }
}
