package com.audioeq.audio

import com.audioeq.equalizer.ParametricEqualizer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class AudioPipelineTest {
    
    private lateinit var equalizer: ParametricEqualizer
    private lateinit var pipeline: AudioPipeline
    
    @Before
    fun setup() {
        equalizer = ParametricEqualizer(44100f, 10)
        pipeline = AudioPipeline(equalizer, 1024)
    }
    
    @Test
    fun `pipeline starts and stops`() {
        assertFalse(pipeline.isRunning)
        
        pipeline.start()
        assertTrue(pipeline.isRunning)
        
        pipeline.stop()
        Thread.sleep(50)
        assertFalse(pipeline.isRunning)
    }
    
    @Test
    fun `pipeline processes audio`() {
        val latch = CountDownLatch(1)
        var processedData: ShortArray? = null
        var processedCount = 0
        
        pipeline.onOutputReady = { data, count ->
            processedData = data
            processedCount = count
            latch.countDown()
        }
        
        pipeline.start()
        
        val inputData = ShortArray(512) { 16384 }
        pipeline.writeInput(inputData)
        
        assertTrue("Pipeline should process within 1 second", latch.await(1, TimeUnit.SECONDS))
        
        assertNotNull(processedData)
        assertEquals(512, processedCount)
        
        pipeline.stop()
    }
    
    @Test
    fun `pipeline applies equalizer to audio`() {
        equalizer.setBandGain(0, 12f)
        
        val latch = CountDownLatch(1)
        var outputSum = 0L
        
        pipeline.onOutputReady = { data, _ ->
            outputSum = data.sumOf { it.toInt().toLong() }
            latch.countDown()
        }
        
        pipeline.start()
        
        val inputData = ShortArray(512) { 1000 }
        val inputSum = inputData.sumOf { it.toInt().toLong() }
        pipeline.writeInput(inputData)
        
        assertTrue(latch.await(1, TimeUnit.SECONDS))
        
        assertNotEquals(inputSum, outputSum)
        
        pipeline.stop()
    }
    
    @Test
    fun `pipeline handles multiple chunks`() {
        val chunksReceived = mutableListOf<Int>()
        val latch = CountDownLatch(3)
        
        pipeline.onOutputReady = { _, count ->
            chunksReceived.add(count)
            latch.countDown()
        }
        
        pipeline.start()
        
        repeat(3) {
            val inputData = ShortArray(512) { (it * 100).toShort() }
            pipeline.writeInput(inputData)
            Thread.sleep(10)
        }
        
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        assertEquals(3, chunksReceived.size)
        
        pipeline.stop()
    }
    
    @Test
    fun `pipeline ignores input when stopped`() {
        var received = false
        
        pipeline.onOutputReady = { _, _ ->
            received = true
        }
        
        val inputData = ShortArray(512) { 1000 }
        val written = pipeline.writeInput(inputData)
        
        assertEquals(0, written)
        assertFalse(received)
    }
    
    @Test
    fun `pipeline returns buffer level`() {
        assertEquals(0, pipeline.bufferLevel)
        
        pipeline.start()
        
        val inputData = ShortArray(512) { 1000 }
        pipeline.writeInput(inputData)
        
        Thread.sleep(50)
        
        pipeline.stop()
    }
    
    @Test
    fun `pipeline handles large input`() {
        val latch = CountDownLatch(1)
        var totalProcessed = 0
        
        pipeline = AudioPipeline(equalizer, 2048)
        
        pipeline.onOutputReady = { _, count ->
            totalProcessed += count
            if (totalProcessed >= 4096) {
                latch.countDown()
            }
        }
        
        pipeline.start()
        
        val inputData = ShortArray(4096) { 16384 }
        pipeline.writeInput(inputData)
        
        assertTrue("Pipeline should process large input within 5 seconds", latch.await(5, TimeUnit.SECONDS))
        
        pipeline.stop()
    }
    
    @Test
    fun `pipeline reports buffer level percent`() {
        val level = pipeline.getBufferLevelPercent()
        assertTrue(level in 0f..1f)
    }
    
    @Test
    fun `pipeline restart works`() {
        pipeline.start()
        Thread.sleep(20)
        pipeline.stop()
        Thread.sleep(20)
        
        assertFalse(pipeline.isRunning)
        
        pipeline.start()
        assertTrue(pipeline.isRunning)
        
        pipeline.stop()
    }
    
    @Test
    fun `pipeline handles error gracefully`() {
        var errorReceived: Exception? = null
        val latch = CountDownLatch(1)
        
        pipeline.onError = { e ->
            errorReceived = e
            latch.countDown()
        }
        
        pipeline.start()
        pipeline.stop()
        
        assertNull(errorReceived)
    }
}
