package com.audioeq.audio

import android.util.Log
import com.audioeq.equalizer.ParametricEqualizer
import java.util.concurrent.atomic.AtomicBoolean

class AudioPipeline(
    private val equalizer: ParametricEqualizer,
    bufferSize: Int = 8192
) {
    private val inputBuffer = CircularBuffer(bufferSize)
    private val processingBuffer = ShortArray(bufferSize)
    
    private val isProcessing = AtomicBoolean(false)
    private var processingThread: Thread? = null
    
    var onOutputReady: ((ShortArray, Int) -> Unit)? = null
    var onError: ((Exception) -> Unit)? = null
    
    val bufferLevel: Int
        get() = inputBuffer.size
    
    val isRunning: Boolean
        get() = isProcessing.get()
    
    fun start() {
        if (isProcessing.getAndSet(true)) return
        
        processingThread = Thread {
            processingLoop()
        }.apply {
            name = "AudioPipeline-Processor"
            priority = Thread.MAX_PRIORITY
            start()
        }
        
        Log.d(TAG, "Audio pipeline started")
    }
    
    fun stop() {
        if (!isProcessing.getAndSet(false)) return
        
        // Signal to processing thread to stop
        processingThread?.interrupt()
        
        // Wait for thread to finish
        processingThread?.join(1000)
        processingThread = null
        
        // Clear all buffers completely
        inputBuffer.clear()
        processingBuffer.fill(0)
        
        // Flush audio output to prevent hanging samples (if available)
        audioOutput?.flush()
        
        Log.d(TAG, "Audio pipeline stopped")
    }
    
    fun writeInput(data: ShortArray, offset: Int = 0, length: Int = data.size): Int {
        if (!isProcessing.get()) return 0
        return inputBuffer.write(data, offset, length)
    }
    
    private fun processingLoop() {
        while (isProcessing.get()) {
            try {
                val available = inputBuffer.size
                
                if (available >= MIN_PROCESS_CHUNK) {
                    val toProcess = minOf(available, processingBuffer.size)
                    val read = inputBuffer.read(processingBuffer, 0, toProcess)
                    
                    if (read > 0) {
                        val outputBuffer = ShortArray(read)
                        equalizer.processBuffer(
                            processingBuffer.copyOf(read),
                            outputBuffer
                        )
                        
                        onOutputReady?.invoke(outputBuffer, read)
                    }
                } else {
                    Thread.sleep(1)
                }
                
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: Exception) {
                Log.e(TAG, "Error in processing loop", e)
                onError?.invoke(e)
            }
        }
    }
    
    fun getBufferLevelPercent(): Float {
        return inputBuffer.size.toFloat() / inputBuffer.capacity.coerceAtLeast(1)
    }
    
    companion object {
        private const val TAG = "AudioPipeline"
        private const val MIN_PROCESS_CHUNK = 256
    }
}
