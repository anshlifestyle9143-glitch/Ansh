package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class VisionOverlayView(
    context: Context
) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var rotation = 0f
    private var pulse = 0f

    private val runnable = object : Runnable {
        override fun run() {
            rotation += 2.2f
            pulse += 0.08f

            if (rotation >= 360f) {
                rotation -= 360f
            }

            invalidate()
            postDelayed(this, 16L)
        }
    }

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        post(runnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height * 0.43f

        val orbRadius = width * 0.105f

        val pulseScale =
            1f + sin(pulse.toDouble()).toFloat() * 0.08f

        val radius =
            orbRadius * pulseScale

        drawRings(
            canvas,
            cx,
            cy,
            radius
        )

        drawOrb(
            canvas,
            cx,
            cy,
            radius
        )

        drawEnergyColumn(
            canvas,
            cx,
            cy,
            radius
        )

        drawStand(
            canvas,
            cx
        )
    }

    private fun drawRings(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float
    ) {

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.5f

        val ringColors =
            intArrayOf(
                Color.argb(220, 70, 225, 255),
                Color.argb(180, 100, 150, 255),
                Color.argb(150, 170, 100, 255)
            )

        val sizes =
            arrayOf(
                floatArrayOf(2.8f, 0.72f),
                floatArrayOf(2.45f, 1.05f),
                floatArrayOf(3.05f, 0.52f)
            )

        for (i in 0..2) {

            paint.color = ringColors[i]

            paint.setShadowLayer(
                14f,
                0f,
                0f,
                ringColors[i]
            )

            val rect =
                RectF(
                    cx - radius * sizes[i][0],
                    cy - radius * sizes[i][1],
                    cx + radius * sizes[i][0],
                    cy + radius * sizes[i][1]
                )

            canvas.save()

            val direction =
                if (i == 1) -1f else 1f

            canvas.rotate(
                rotation * direction +
                    i * 120f,
                cx,
                cy
            )

            canvas.drawOval(
                rect,
                paint
            )

            canvas.restore()
        }

        paint.clearShadowLayer()
    }

    private fun drawOrb(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float
    ) {

        paint.style = Paint.Style.FILL

        paint.setShadowLayer(
            radius * 1.8f,
            0f,
            0f,
            Color.argb(220, 60, 210, 255)
        )

        val gradient =
            android.graphics.RadialGradient(
                cx - radius * 0.25f,
                cy - radius * 0.25f,
                radius * 1.5f,
                intArrayOf(
                    Color.WHITE,
                    Color.rgb(90, 235, 255),
                    Color.rgb(55, 105, 255),
                    Color.TRANSPARENT
                ),
                floatArrayOf(
                    0f,
                    0.25f,
                    0.68f,
                    1f
                ),
                android.graphics.Shader.TileMode.CLAMP
            )

        paint.shader = gradient

        canvas.drawCircle(
            cx,
            cy,
            radius,
            paint
        )

        paint.shader = null
        paint.clearShadowLayer()

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.argb(
            210,
            120,
            245,
            255
        )

        canvas.drawCircle(
            cx,
            cy,
            radius * 1.18f,
            paint
        )
    }

    private fun drawEnergyColumn(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float
    ) {

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = Color.argb(
            120,
            80,
            220,
            255
        )

        canvas.drawLine(
            cx,
            cy + radius * 1.15f,
            cx,
            height * 0.79f,
            paint
        )
    }

    private fun drawStand(
        canvas: Canvas,
        cx: Float
    ) {

        val top =
            height * 0.78f

        paint.style = Paint.Style.FILL

        paint.setShadowLayer(
            15f,
            0f,
            0f,
            Color.argb(180, 60, 210, 255)
        )

        paint.color = Color.argb(
            100,
            50,
            170,
            255
        )

        canvas.drawRoundRect(
            RectF(
                cx - width * 0.115f,
                top,
                cx + width * 0.115f,
                top + height * 0.10f
            ),
            18f,
            18f,
            paint
        )

        paint.clearShadowLayer()

        paint.color = Color.argb(
            190,
            80,
            220,
            255
        )

        canvas.drawRoundRect(
            RectF(
                cx - width * 0.17f,
                height * 0.91f,
                cx + width * 0.17f,
                height * 0.96f
            ),
            14f,
            14f,
            paint
        )
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(runnable)
        super.onDetachedFromWindow()
    }
}
