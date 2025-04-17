package com.example.volumeslider

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class VolumeSliderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Interface for volume change callback
    interface OnVolumeChangeListener {
        fun onVolumeChanged(volumePercent: Float)
    }

    // Paint objects for drawing
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Drawing dimensions
    private val trackWidth = 6f  // Narrow track (6dp)
    private val thumbRadius = 7f  // Smaller thumb (14dp diameter)
    private val cornerRadius = 3f // Rounded corners

    // Track and thumb rectangles
    private val trackRect = RectF()
    private val fillRect = RectF()

    // Current volume percentage (0-100)
    private var volumePercent = 50f

    // Drag tracking
    private var isDragging = false
    private var startY = 0f
    private var startPercent = 0f

    // Sensitivity factor - higher values mean less sensitive
    private var sensitivity = 50f

    // Volume change listener
    private var volumeChangeListener: OnVolumeChangeListener? = null

    init {
        // Initialize paint objects
        trackPaint.color = Color.parseColor("#20000000") // Transparent black
        trackPaint.style = Paint.Style.FILL

        fillPaint.color = Color.parseColor("#4A90E2") // Blue
        fillPaint.style = Paint.Style.FILL
        fillPaint.alpha = 200 // Slightly transparent

        thumbPaint.color = Color.parseColor("#3700B3") // Purple
        thumbPaint.style = Paint.Style.FILL

        thumbStrokePaint.color = Color.WHITE
        thumbStrokePaint.style = Paint.Style.STROKE
        thumbStrokePaint.strokeWidth = 1.5f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Position the track centered horizontally
        val left = (width - trackWidth) / 2
        trackRect.set(left, 0f, left + trackWidth, height.toFloat())

        // Update the fill rect
        updateFillRect()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the track
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint)

        // Draw the fill
        canvas.drawRoundRect(fillRect, cornerRadius, cornerRadius, fillPaint)

        // Draw the thumb
        val thumbCenterX = trackRect.centerX()
        val thumbCenterY = height * (1 - volumePercent / 100)
        
        // Draw thumb with white border
        canvas.drawCircle(thumbCenterX, thumbCenterY, thumbRadius, thumbPaint)
        canvas.drawCircle(thumbCenterX, thumbCenterY, thumbRadius, thumbStrokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if touch is within extended touch area (wider than visual track)
                val touchableWidth = 36f // Much wider touch area (36dp)
                val left = (width - touchableWidth) / 2
                val touchableRect = RectF(left, 0f, left + touchableWidth, height.toFloat())
                
                if (touchableRect.contains(event.x, event.y)) {
                    isDragging = true
                    startY = event.y
                    startPercent = volumePercent
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    // Calculate change in Y (inverted for intuitive control)
                    val deltaY = startY - event.y
                    
                    // Apply sensitivity factor - higher value = less sensitive
                    val adjustedDelta = deltaY * (100f / sensitivity)
                    
                    // Calculate percentage change based on view height
                    val percentChange = (adjustedDelta / height) * 100f
                    
                    // Update volume percent
                    volumePercent = (startPercent + percentChange).coerceIn(0f, 100f)
                    
                    // Update visuals
                    updateFillRect()
                    invalidate()
                    
                    // Notify listener
                    volumeChangeListener?.onVolumeChanged(volumePercent)
                    
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateFillRect() {
        fillRect.set(
            trackRect.left,
            height * (1 - volumePercent / 100),
            trackRect.right,
            height.toFloat()
        )
    }

    fun setVolume(percent: Float) {
        volumePercent = percent.coerceIn(0f, 100f)
        updateFillRect()
        invalidate()
    }

    fun getVolume(): Float {
        return volumePercent
    }

    fun setSensitivity(sensitivityValue: Int) {
        sensitivity = sensitivityValue.toFloat()
    }

    fun setOnVolumeChangeListener(listener: OnVolumeChangeListener) {
        volumeChangeListener = listener
    }
}
