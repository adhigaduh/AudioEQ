package com.audioeq.audio

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class CircularBuffer(
    private val capacity: Int
) {
    private val buffer = ShortArray(capacity)
    private var writeIndex = 0
    private var readIndex = 0
    private var available = 0
    private val lock = Any()
    
    val size: Int
        get() = synchronized(lock) { available }
    
    val isFull: Boolean
        get() = synchronized(lock) { available == capacity }
    
    val isEmpty: Boolean
        get() = synchronized(lock) { available == 0 }
    
    fun write(data: ShortArray, offset: Int = 0, length: Int = data.size): Int {
        synchronized(lock) {
            val toWrite = min(length, capacity - available)
            if (toWrite <= 0) return 0
            
            var written = 0
            repeat(toWrite) {
                buffer[writeIndex] = data[offset + written]
                writeIndex = (writeIndex + 1) % capacity
                written++
            }
            available += toWrite
            
            return toWrite
        }
    }
    
    fun read(output: ShortArray, offset: Int = 0, length: Int = output.size): Int {
        synchronized(lock) {
            val toRead = min(length, available)
            if (toRead <= 0) return 0
            
            var read = 0
            repeat(toRead) {
                output[offset + read] = buffer[readIndex]
                readIndex = (readIndex + 1) % capacity
                read++
            }
            available -= toRead
            
            return toRead
        }
    }
    
    fun peek(output: ShortArray, offset: Int = 0, length: Int = output.size): Int {
        synchronized(lock) {
            val toRead = min(length, available)
            if (toRead <= 0) return 0
            
            var tempReadIndex = readIndex
            var read = 0
            repeat(toRead) {
                output[offset + read] = buffer[tempReadIndex]
                tempReadIndex = (tempReadIndex + 1) % capacity
                read++
            }
            
            return toRead
        }
    }
    
    fun clear() {
        synchronized(lock) {
            writeIndex = 0
            readIndex = 0
            available = 0
        }
    }
    
    fun skip(count: Int): Int {
        synchronized(lock) {
            val toSkip = min(count, available)
            readIndex = (readIndex + toSkip) % capacity
            available -= toSkip
            return toSkip
        }
    }
}
