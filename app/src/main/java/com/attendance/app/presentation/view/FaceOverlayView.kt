package com.attendance.app.presentation.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val ovalPaint = Paint().apply {
        color = Color.parseColor("#667eea")
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#80000000") // Semi-transparent black
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val hintTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val cornerPaint = Paint().apply {
        color = Color.parseColor("#667eea")
        style = Paint.Style.STROKE
        strokeWidth = 12f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    var showGuide = true
        set(value) {
            field = value
            invalidate()
        }

    var faceDetected = false
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!showGuide) return

        val centerX = width / 2f
        val centerY = height / 2.2f
        val ovalWidth = width * 0.65f
        val ovalHeight = height * 0.45f

        // Draw darkened overlay except face area
        val path = Path().apply {
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            addOval(
                centerX - ovalWidth / 2,
                centerY - ovalHeight / 2,
                centerX + ovalWidth / 2,
                centerY + ovalHeight / 2,
                Path.Direction.CCW
            )
            fillType = Path.FillType.EVEN_ODD
        }
        canvas.drawPath(path, overlayPaint)

        // Draw oval guide with corners
        val rect = RectF(
            centerX - ovalWidth / 2,
            centerY - ovalHeight / 2,
            centerX + ovalWidth / 2,
            centerY + ovalHeight / 2
        )

        // Change color based on face detection
        if (faceDetected) {
            ovalPaint.color = Color.parseColor("#28a745") // Green when face detected
            cornerPaint.color = Color.parseColor("#28a745")
        } else {
            ovalPaint.color = Color.parseColor("#667eea") // Purple normally
            cornerPaint.color = Color.parseColor("#667eea")
        }

        canvas.drawOval(rect, ovalPaint)

        // Draw corner brackets
        val cornerLength = 60f

        // Top-left corner
        canvas.drawLine(rect.left, rect.top + cornerLength, rect.left, rect.top, cornerPaint)
        canvas.drawLine(rect.left, rect.top, rect.left + cornerLength, rect.top, cornerPaint)

        // Top-right corner
        canvas.drawLine(rect.right - cornerLength, rect.top, rect.right, rect.top, cornerPaint)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + cornerLength, cornerPaint)

        // Bottom-left corner
        canvas.drawLine(rect.left, rect.bottom - cornerLength, rect.left, rect.bottom, cornerPaint)
        canvas.drawLine(rect.left, rect.bottom, rect.left + cornerLength, rect.bottom, cornerPaint)

        // Bottom-right corner
        canvas.drawLine(rect.right - cornerLength, rect.bottom, rect.right, rect.bottom, cornerPaint)
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - cornerLength, cornerPaint)

        // Draw instructions
        val topTextY = centerY - ovalHeight / 2 - 80f

        if (faceDetected) {
            canvas.drawText("✓ Đã phát hiện khuôn mặt", centerX, topTextY, textPaint)
        } else {
            canvas.drawText("📸 Đặt khuôn mặt vào khung", centerX, topTextY, textPaint)
        }

        // Draw hints at bottom
        val bottomHints = listOf(
            "• Nhìn thẳng vào camera",
            "• Ánh sáng đủ sáng",
            "• Khuôn mặt thẳng"
        )

        var hintY = centerY + ovalHeight / 2 + 100f
        bottomHints.forEach { hint ->
            canvas.drawText(hint, centerX, hintY, hintTextPaint)
            hintY += 50f
        }
    }
}