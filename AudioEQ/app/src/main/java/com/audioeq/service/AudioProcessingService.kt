package com.audioeq.service

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
import com.audioeq.equalizer.EqualizerPreset
import com.audioeq.equalizer.ParametricEqualizer
import com.audioeq.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AudioProcessingService : Service() {
    
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    
    private var mediaProjection: MediaProjection? = null
    private var audioCapture: AudioCapture? = null
    private var audioOutput: AudioOutput? = null
    private var equalizer: ParametricEqualizer? = null
    
    private var isProcessing = false
    private var resultCode: Int = 0
    private var resultData: Intent? = null
    
    var onProcessingStateChanged: ((Boolean) -> Unit)? = null
    
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
                resultData = it.getParcelableExtra(EXTRA_RESULT_DATA)
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
    
    private fun startProcessing() {
        if (isProcessing) return
        
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
                
                isProcessing = true
                onProcessingStateChanged?.invoke(true)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start processing", e)
                stopSelf()
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
                Log.d(TAG, "Media projection stopped")
                stopProcessing()
            }
        }, null)
    }
    
    private fun initializeAudioComponents() {
        val sampleRate = 44100
        
        equalizer = ParametricEqualizer(sampleRate.toFloat())
        
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
        
        audioCapture?.onAudioBufferReady = { buffer ->
            val eq = equalizer ?: return@AudioCapture
            val output = audioOutput ?: return@AudioCapture
            
            val processedBuffer = ShortArray(buffer.size)
            eq.processBuffer(buffer, processedBuffer)
            output.write(processedBuffer)
        }
        
        audioCapture?.startCapture()
    }
    
    fun stopProcessing() {
        if (!isProcessing) return
        
        audioCapture?.stopCapture()
        audioOutput?.stop()
        
        mediaProjection?.stop()
        mediaProjection = null
        
        audioCapture = null
        audioOutput = null
        
        isProcessing = false
        onProcessingStateChanged?.invoke(false)
        
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
    
    fun getEqualizerBands() = equalizer?.getBands() ?: emptyList()
    
    fun isProcessing(): Boolean = isProcessing
    
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
