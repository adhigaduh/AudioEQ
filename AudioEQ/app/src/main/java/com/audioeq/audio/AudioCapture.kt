package com.audioeq.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.util.Log

class AudioCapture(
    private val mediaProjection: MediaProjection,
    private val sampleRate: Int = 44100,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_STEREO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    private var audioRecord: AudioRecord? = null
    private var isCapturing = false
    private var captureThread: Thread? = null
    
    var onAudioBufferReady: ((ShortArray) -> Unit)? = null
    
    val bufferSize: Int by lazy {
        val minSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        maxOf(minSize * 2, 4096)
    }
    
    fun startCapture(): Boolean {
        if (isCapturing) return true
        
        try {
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            // Remove USAGE_MEDIA to prevent feedback loop
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
            
            audioRecord = AudioRecord.Builder()
                .setAudioPlaybackCaptureConfig(config)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .setEncoding(audioFormat)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized")
                return false
            }
            
            audioRecord?.startRecording()
            isCapturing = true
            
            captureThread = Thread {
                captureLoop()
            }.apply { start() }
            
            Log.d(TAG, "Audio capture started")
            return true
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception during capture setup", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start capture", e)
            return false
        }
    }
    
    private fun captureLoop() {
        val buffer = ShortArray(bufferSize / 2)
        
        while (isCapturing && audioRecord != null) {
            val result = audioRecord?.read(buffer, 0, buffer.size) ?: -1
            
            if (result > 0) {
                val audioData = buffer.copyOf(result)
                onAudioBufferReady?.invoke(audioData)
            } else if (result < 0) {
                Log.e(TAG, "AudioRecord read error: $result")
            }
        }
    }
    
    fun stopCapture() {
        isCapturing = false
        
        captureThread?.join(1000)
        captureThread = null
        
        audioRecord?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping AudioRecord", e)
            }
        }
        audioRecord = null
        
        Log.d(TAG, "Audio capture stopped")
    }
    
    fun isCapturing(): Boolean = isCapturing && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING
    
    companion object {
        private const val TAG = "AudioCapture"
    }
}
