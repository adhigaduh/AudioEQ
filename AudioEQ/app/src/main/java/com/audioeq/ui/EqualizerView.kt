package com.audioeq.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.audioeq.equalizer.FilterBand
import kotlin.math.max
import kotlin.math.min

class EqualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var bands: List<FilterBand> = emptyList()
    
    private val linePaint = Paint().apply {
        color = Color.parseColor("#2196F3")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    private val pointPaint = Paint().apply {
        color = Color.parseColor("#1976D2")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#333333")
        strokeWidth = 1f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        isAntiAlias = true
    }
    
    private val minGain = -12f
    private val maxGain = 12f
    private val padding = 60f
    
    var onBandChanged: ((Int, Float) -> Unit)? = null
    
    fun setBands(bands: List<FilterBand>) {
        this.bands = bands
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        drawGrid(canvas)
        
        if (bands.isEmpty()) return
        
        val width = width - padding * 2
        val height = height - padding * 2
        
        val points = bands.mapIndexed { index, band ->
            val x = padding + (index.toFloat() / (bands.size - 1)) * width
            val normalizedGain = (band.gain - minGain) / (maxGain - minGain)
            val y = padding + (1 - normalizedGain) * height
            Pair(x, y)
        }
        
        if (points.size > 1) {
            val path = android.graphics.Path()
            path.moveTo(points[0].first, points[0].second)
            
            for (i in 1 until points.size) {
                path.lineTo(points[i].first, points[i].second)
            }
            
            canvas.drawPath(path, linePaint)
        }
        
        points.forEachIndexed { index, (x, y) ->
            canvas.drawCircle(x, y, 20f, pointPaint)
            
            val freqText = formatFrequency(bands[index].frequency)
            canvas.drawText(freqText, x - 20, height + padding + 30, textPaint)
        }
        
        val gainLabels = listOf(-12, -6, 0, 6, 12)
        gainLabels.forEach { gain ->
            val normalizedGain = (gain.toFloat() - minGain) / (maxGain - minGain)
            val y = padding + (1 - normalizedGain) * height
            canvas.drawText("${gain}dB", 10f, y, textPaint)
        }
    }
    
    private fun drawGrid(canvas: Canvas) {
        val width = width - padding * 2
        val height = height - padding * 2
        
        for (i in 0..4) {
            val y = padding + (i / 4f) * height
            canvas.drawLine(padding, y, width + padding, y, gridPaint)
        }
        
        bands.indices.forEach { index ->
            val x = padding + (index.toFloat() / max(1, bands.size - 1)) * width
            canvas.drawLine(x, padding, x, height + padding, gridPaint)
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (bands.isEmpty()) return false
        
        val width = width - padding * 2
        val height = height - padding * 2
        
        val x = event.x
        val y = event.y
        
        val bandWidth = width / (bands.size - 1)
        val bandIndex = ((x - padding) / bandWidth).toInt().coerceIn(0, bands.size - 1)
        
        val normalizedGain = 1 - ((y - padding) / height).coerce(0f, 1f)
        val gain = minGain + normalizedGain * (maxGain - minGain)
        
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                onBandChanged?.invoke(bandIndex, gain)
                return true
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    private fun formatFrequency(freq: Float): String {
        return when {
            freq >= 1000 -> "${(freq / 1000).toInt()}kHz"
            else -> "${freq.toInt()}Hz"
        }
    }
}
