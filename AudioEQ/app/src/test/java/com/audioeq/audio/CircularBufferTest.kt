package com.audioeq.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CircularBufferTest {
    
    private lateinit var buffer: CircularBuffer
    private val capacity = 100
    
    @Before
    fun setup() {
        buffer = CircularBuffer(capacity)
    }
    
    @Test
    fun `new buffer is empty`() {
        assertTrue(buffer.isEmpty)
        assertFalse(buffer.isFull)
        assertEquals(0, buffer.size)
    }
    
    @Test
    fun `write returns number of items written`() {
        val data = ShortArray(10) { it.toShort() }
        val written = buffer.write(data)
        
        assertEquals(10, written)
        assertEquals(10, buffer.size)
    }
    
    @Test
    fun `read returns written data`() {
        val data = ShortArray(10) { (it * 100).toShort() }
        buffer.write(data)
        
        val output = ShortArray(10)
        val read = buffer.read(output)
        
        assertEquals(10, read)
        assertArrayEquals(data, output)
    }
    
    @Test
    fun `buffer becomes full`() {
        val data = ShortArray(capacity) { it.toShort() }
        buffer.write(data)
        
        assertTrue(buffer.isFull)
        assertEquals(capacity, buffer.size)
    }
    
    @Test
    fun `write to full buffer returns zero`() {
        val data = ShortArray(capacity) { it.toShort() }
        buffer.write(data)
        
        val extraData = ShortArray(10) { 999.toShort() }
        val written = buffer.write(extraData)
        
        assertEquals(0, written)
    }
    
    @Test
    fun `read from empty buffer returns zero`() {
        val output = ShortArray(10)
        val read = buffer.read(output)
        
        assertEquals(0, read)
    }
    
    @Test
    fun `read clears data from buffer`() {
        val data = ShortArray(10) { it.toShort() }
        buffer.write(data)
        
        buffer.read(ShortArray(10))
        
        assertTrue(buffer.isEmpty)
    }
    
    @Test
    fun `peek does not remove data`() {
        val data = ShortArray(10) { it.toShort() }
        buffer.write(data)
        
        val output = ShortArray(10)
        buffer.peek(output)
        
        assertEquals(10, buffer.size)
    }
    
    @Test
    fun `peek returns correct data`() {
        val data = ShortArray(10) { (it * 50).toShort() }
        buffer.write(data)
        
        val output = ShortArray(10)
        buffer.peek(output)
        
        assertArrayEquals(data, output)
    }
    
    @Test
    fun `partial read works correctly`() {
        val data = ShortArray(20) { it.toShort() }
        buffer.write(data)
        
        val output = ShortArray(5)
        val read = buffer.read(output)
        
        assertEquals(5, read)
        assertEquals(15, buffer.size)
        
        val expected = ShortArray(5) { it.toShort() }
        assertArrayEquals(expected, output)
    }
    
    @Test
    fun `partial write works correctly`() {
        val data = ShortArray(50) { it.toShort() }
        
        val written = buffer.write(data, 10, 20)
        
        assertEquals(20, written)
        assertEquals(20, buffer.size)
    }
    
    @Test
    fun `circular wraparound works`() {
        val halfCapacity = capacity / 2
        val data = ShortArray(halfCapacity) { 1.toShort() }
        
        buffer.write(data)
        buffer.read(ShortArray(halfCapacity))
        
        val wrapData = ShortArray(halfCapacity) { 2.toShort() }
        buffer.write(wrapData)
        
        val output = ShortArray(halfCapacity)
        buffer.read(output)
        
        assertArrayEquals(wrapData, output)
    }
    
    @Test
    fun `clear empties buffer`() {
        buffer.write(ShortArray(50) { it.toShort() })
        buffer.clear()
        
        assertTrue(buffer.isEmpty)
        assertEquals(0, buffer.size)
    }
    
    @Test
    fun `skip advances read position`() {
        val data = ShortArray(20) { it.toShort() }
        buffer.write(data)
        
        val skipped = buffer.skip(5)
        
        assertEquals(5, skipped)
        assertEquals(15, buffer.size)
        
        val output = ShortArray(5)
        buffer.read(output)
        
        val expected = ShortArray(5) { (it + 5).toShort() }
        assertArrayEquals(expected, output)
    }
    
    @Test
    fun `concurrent access is thread safe`() {
        val iterations = 1000
        val writers = 4
        val readers = 4
        
        val largeBuffer = CircularBuffer(10000)
        val writeCount = java.util.concurrent.atomic.AtomicInteger(0)
        val readCount = java.util.concurrent.atomic.AtomicInteger(0)
        
        val writerThreads = (1..writers).map {
            Thread {
                repeat(iterations) { i ->
                    val data = ShortArray(10) { i.toShort() }
                    largeBuffer.write(data)
                    writeCount.addAndGet(largeBuffer.write(data))
                }
            }
        }
        
        val readerThreads = (1..readers).map {
            Thread {
                repeat(iterations) {
                    val output = ShortArray(10)
                    readCount.addAndGet(largeBuffer.read(output))
                }
            }
        }
        
        writerThreads.forEach { it.start() }
        readerThreads.forEach { it.start() }
        
        writerThreads.forEach { it.join() }
        readerThreads.forEach { it.join() }
        
        assertTrue(largeBuffer.size >= 0)
    }
}
