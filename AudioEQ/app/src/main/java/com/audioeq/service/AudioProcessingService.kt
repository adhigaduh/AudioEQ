package com.audioeq.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.audioeq.R
import com.audioeq.audio.AudioCapture
import com.audioeq.audio.AudioOutput
import com.audioeq.audio.AudioPipeline
import com.audioeq.equalizer.EqualizerPreset
import com.audioeq.equalizer.ParametricEqualizer
import com.audioeq.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ProcessingState {
    IDLE,
    STARTING,
    RUNNING,
    STOPPING,
    ERROR
}

class AudioProcessingService : Service() {
    
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    
    private var mediaProjection: MediaProjection? = null
    private var audioCapture: AudioCapture? = null
    private var audioOutput: AudioOutput? = null
    private var audioPipeline: AudioPipeline? = null
    private var equalizer: ParametricEqualizer? = null
    
    private var _processingState = ProcessingState.IDLE
    val processingState: ProcessingState get() = _processingState
    
    private var _errorMessage: String? = null
    val errorMessage: String? get() = _errorMessage
    
    private var resultCode: Int = 0
    private var resultData: Intent? = null
    
    var onProcessingStateChanged: ((ProcessingState) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    
    inner class LocalBinder : Binder() {
        fun getService(): AudioProcessingService = this@AudioProcessingService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.hasExtra(EXTRA_RESULT_CODE)) {
                resultCode = it.getIntExtra(EXTRA_RESULT_CODE, 0)
                resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    it.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    it.getParcelableExtra(EXTRA_RESULT_DATA)
                }
            }
            
            when (it.action) {
                ACTION_START -> startProcessing()
                ACTION_STOP -> stopProcessing()
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        stopProcessing()
        serviceScope.cancel()
        super.onDestroy()
    }
    
    fun setMediaProjectionData(resultCode: Int, data: Intent) {
        this.resultCode = resultCode
        this.resultData = data
    }
    
    private fun setState(newState: ProcessingState) {
        _processingState = newState
        onProcessingStateChanged?.invoke(newState)
    }
    
    private fun setError(message: String) {
        _errorMessage = message
        setState(ProcessingState.ERROR)
        onError?.invoke(message)
    }
    
    private fun startProcessing() {
        if (_processingState != ProcessingState.IDLE) {
            Log.w(TAG, "Cannot start: current state is $_processingState")
            return
        }
        
        setState(ProcessingState.STARTING)
        
        val notification = createNotification()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        serviceScope.launch {
            try {
                initializeMediaProjection()
                initializeAudioComponents()
                startAudioProcessing()
                
                withContext(Dispatchers.Main) {
                    setState(ProcessingState.RUNNING)
                }
                
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception", e)
                withContext(Dispatchers.Main) {
                    setError("Permission denied: ${e.message}")
                    stopSelf()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start processing", e)
                withContext(Dispatchers.Main) {
                    setError("Failed to start: ${e.message}")
                    stopSelf()
                }
            }
        }
    }
    
    private fun initializeMediaProjection() {
        if (resultCode == 0 || resultData == null) {
            throw IllegalStateException("Media projection data not set")
        }
        
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = manager.getMediaProjection(resultCode, resultData!!)
        
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Log.d(TAG, "Media projection stopped by user")
                serviceScope.launch {
                    stopProcessing()
                }
            }
        }, null)
    }
    
    private fun initializeAudioComponents() {
        val sampleRate = 44100
        
        equalizer = ParametricEqualizer(sampleRate.toFloat())
        
        audioPipeline = AudioPipeline(equalizer!!, 8192)
        
        audioCapture = AudioCapture(
            mediaProjection = mediaProjection!!,
            sampleRate = sampleRate
        )
        
        audioOutput = AudioOutput(
            context = this,
            sampleRate = sampleRate
        )
    }
    
    private fun startAudioProcessing() {
        audioOutput?.start()
        
        audioPipeline?.onOutputReady = { buffer, count ->
            val output = audioOutput
            if (output != null && count > 0) {
                output.write(buffer.copyOf(count))
            }
        }
        
        audioPipeline?.onError = { error ->
            Log.e(TAG, "Pipeline error", error)
            serviceScope.launch {
                setError("Audio processing error: ${error.message}")
            }
        }
        
        audioPipeline?.start()
        
        audioCapture?.onAudioBufferReady = { buffer ->
            audioPipeline?.writeInput(buffer)
        }
        
        val captureStarted = audioCapture?.startCapture() ?: false
        if (!captureStarted) {
            throw RuntimeException("Failed to start audio capture")
        }
    }
    
    fun stopProcessing() {
        if (_processingState == ProcessingState.IDLE || 
            _processingState == ProcessingState.STOPPING) {
            return
        }
        
        setState(ProcessingState.STOPPING)
        
        // Stop capture first to break feedback loop
        audioCapture?.stopCapture()
        
        // Wait for capture to fully stop and pipeline to empty
        var bufferEmptyCount = 0
        while (bufferEmptyCount < 20 && (audioPipeline?.bufferLevel ?: 0) > 0) {
            Thread.sleep(50)
            bufferEmptyCount++
        }
        
        // Stop all components with proper cleanup
        audioPipeline?.stop()
        audioOutput?.stopProcessing() // Use new method with proper cleanup
        audioOutput?.resetVolume() // Reset volume to default
        
        // Ensure MediaProjection is completely stopped
        mediaProjection?.stop()
        mediaProjection = null
        setState(ProcessingState.IDLE)
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    fun setEqualizerPreset(preset: EqualizerPreset) {
        equalizer?.applyPreset(preset)
    }
    
    fun setBandGain(bandIndex: Int, gain: Float) {
        equalizer?.setBandGain(bandIndex, gain)
    }
    
    fun setBandFrequency(bandIndex: Int, frequency: Float) {
        equalizer?.setBandFrequency(bandIndex, frequency)
    }
    
    fun setBandQ(bandIndex: Int, q: Float) {
        equalizer?.setBandQ(bandIndex, q)
    }
    
    fun setEqualizerEnabled(enabled: Boolean) {
        equalizer?.isEnabled = enabled
    }
    
    fun getEqualizerBands() = equalizer?.getBands() ?: emptyList()
    
    fun isProcessing(): Boolean = _processingState == ProcessingState.RUNNING
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Audio Processing",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Audio equalizer processing notification"
        }
        
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Audio Equalizer")
            .setContentText("Processing audio...")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    companion object {
        private const val TAG = "AudioProcessingService"
        private const val CHANNEL_ID = "audio_eq_channel"
        private const val NOTIFICATION_ID = 1001
        
        const val ACTION_START = "com.audioeq.action.START"
        const val ACTION_STOP = "com.audioeq.action.STOP"
        
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        
        fun createStartIntent(context: Context, resultCode: Int, data: Intent): Intent {
            return Intent(context, AudioProcessingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
        }
        
        fun createStopIntent(context: Context): Intent {
            return Intent(context, AudioProcessingService::class.java).apply {
                action = ACTION_STOP
            }
        }
    }
}
