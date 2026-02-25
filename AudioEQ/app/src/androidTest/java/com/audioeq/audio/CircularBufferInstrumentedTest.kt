package com.audioeq.audio

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CircularBufferInstrumentedTest {
    
    private lateinit var context: Context
    private lateinit var buffer: CircularBuffer
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        buffer = CircularBuffer(4096)
    }
    
    @Test
    fun testBufferPerformanceOnDevice() {
        val data = ShortArray(1024) { it.toShort() }
        
        val iterations = 10000
        val startTime = System.nanoTime()
        
        repeat(iterations) {
            buffer.write(data)
            buffer.read(ShortArray(1024))
        }
        
        val durationMs = (System.nanoTime() - startTime) / 1_000_000.0
        val operationsPerMs = iterations * 2 / durationMs
        
        assertTrue(
            "Buffer operations should be fast: $operationsPerMs ops/ms",
            operationsPerMs > 100
        )
    }
    
    @Test
    fun testLargeBufferOperations() {
        val largeBuffer = CircularBuffer(65536)
        val chunkSize = 4096
        
        val data = ShortArray(chunkSize) { 16384 }
        val output = ShortArray(chunkSize)
        
        val writeStartTime = System.nanoTime()
        repeat(16) {
            largeBuffer.write(data)
        }
        val writeTime = (System.nanoTime() - writeStartTime) / 1_000_000.0
        
        val readStartTime = System.nanoTime()
        repeat(16) {
            largeBuffer.read(output)
        }
        val readTime = (System.nanoTime() - readStartTime) / 1_000_000.0
        
        assertTrue("Write 64KB should be < 10ms, took $writeTime ms", writeTime < 10)
        assertTrue("Read 64KB should be < 10ms, took $readTime ms", readTime < 10)
    }
}
