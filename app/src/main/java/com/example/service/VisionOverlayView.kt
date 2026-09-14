package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class VisionOverlayView(
    context: Context
) : View(context) {

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG)

    private var rotation = 0f
    private var pulse = 0f
    private var scan = 0f

    private val runnable =
        object : Runnable {

            override fun run() {

                rotation += 2.8f
                pulse += 0.075f
                scan += 0.045f

                if (rotation >= 360f) {
                    rotation -= 360f
                }

                if (scan >= 1f) {
                    scan = 0f
                }

                invalidate()

                postDelayed(
                    this,
                    16L
                )
            }
        }

    init {

        setLayerType(
            View.LAYER_TYPE_SOFTWARE,
            null
        )

        post(runnable)
    }

    override fun onDraw(
        canvas: Canvas
    ) {

        super.onDraw(canvas)

        val cx =
            width / 2f

        /*
         * Orb is intentionally kept in the
         * upper portion of the hologram.
         */
        val cy =
            height * 0.32f

        /*
         * Larger and much more visible orb.
         */
        val baseRadius =
            width * 0.145f

        val pulseAmount =
            sin(
                pulse.toDouble()
            ).toFloat() * 0.10f

        val radius =
            baseRadius *
                (1f + pulseAmount)

        /*
         * Outer holographic atmosphere.
         */
        drawOuterGlow(
            canvas,
            cx,
            cy,
            radius
        )

        /*
         * Three independent rotating rings.
         */
        drawRings(
            canvas,
            cx,
            cy,
            radius
        )

        /*
         * Main energy orb.
         */
        drawOrb(
            canvas,
            cx,
            cy,
            radius
        )

        /*
         * Energy beam from orb to stand.
         */
        drawEnergyColumn(
            canvas,
            cx,
            cy,
            radius
        )

        /*
         * Bottom holographic projector stand.
         */
        drawStand(
            canvas,
            cx
        )
    }

    private fun drawOuterGlow(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float
    ) {

        paint.style =
            Paint.Style.FILL

        val glow =
            RadialGradient(
                cx,
                cy,
                radius * 2.6f,
                intArrayOf(
                    Color.argb(
                        110,
                        40,
                        220,
                        255
                    ),
                    Color.argb(
                        55,
                        40,
                        170,
                        255
                    ),
                    Color.argb(
                        20,
                        70,
                        100,
                        255
                    ),
                    Color.TRANSPARENT
                ),
                floatArrayOf(
                    0f,
                    0.35f,
                    0.65f,
                    1f
                ),
                Shader.TileMode.CLAMP
            )

        paint.shader =
            glow

        canvas.drawCircle(
            cx,
            cy,
            radius * 2.6f,
            paint
        )

        paint.shader = null
    }

    private fun drawRings(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float
    ) {

        paint.style =
            Paint.Style.STROKE

        paint.strokeWidth =
            4.0f

        val ringColors =
            intArrayOf(
                Color.argb(
                    245,
                    50,
                    235,
                    255
                ),
                Color.argb(
                    225,
                    90,
                    165,
                    255
                ),
                Color.argb(
                    210,
                    175,
                    100,
                    255
                )
            )

        val ringWidth =
            floatArrayOf(
                3.0f,
                2.4f,
                3.4f
            )

        val horizontal =
            floatArrayOf(
                2.9f,
                2.45f,
                3.15f
            )

        val vertical =
            floatArrayOf(
                0.72f,
                1.05f,
                0.52f
            )

        for (i in 0..2) {

            paint.color =
                ringColors[i]

            paint.strokeWidth =
                ringWidth[i]

            paint.setShadowLayer(
                18f,
                0f,
                0f,
                ringColors[i]
            )

            val rect =
                RectF(
                    cx -
                        radius *
                        horizontal[i],

                    cy -
                        radius *
                        vertical[i],

                    cx +
                        radius *
                        horizontal[i],

                    cy +
                        radius *
                        vertical[i]
                )

            canvas.save()

            val direction =
                if (i == 1) {
                    -1f
                } else {
                    1f
                }

            canvas.rotate(
                rotation *
                    direction +
                    i * 120f,

                cx,
                cy
            )

            canvas.drawOval(
                rect,
                paint
            )

            canvas.restore()

            paint.clearShadowLayer()
        }

        /*
         * Small rotating energy points
         * make the hologram feel alive.
         */
        drawEnergyPoints(
            canvas,
            cx,
            cy,
            radius
        )
    }

    private fun drawEnergyPoints(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float
    ) {

        paint.style =
            Paint.Style.FILL

        paint.setShadowLayer(
            12f,
            0f,
            0f,
            Color.argb(
                230,
                80,
                230,
                255
            )
        )

        for (i in 0 until 6) {

            val angle =
                Math.toRadians(
                    (
                        rotation * 1.7f +
                            i * 60f
                        ).toDouble()
                )

            val x =
                cx +
                    cos(angle).toFloat() *
                    radius *
                    2.0f

            val y =
                cy +
                    sin(angle).toFloat() *
                    radius *
                    0.95f

            canvas.drawCircle(
                x,
                y,
                3.5f,
                paint
            )
        }

        paint.clearShadowLayer()
    }

    private fun drawOrb(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float
    ) {

        paint.style =
            Paint.Style.FILL

        paint.setShadowLayer(
            radius * 2.2f,
            0f,
            0f,
            Color.argb(
                240,
                40,
                220,
                255
            )
        )

        val gradient =
            RadialGradient(
                cx -
                    radius * 0.28f,

                cy -
                    radius * 0.30f,

                radius * 1.45f,

                intArrayOf(
                    Color.WHITE,

                    Color.rgb(
                        185,
                        255,
                        255
                    ),

                    Color.rgb(
                        65,
                        230,
                        255
                    ),

                    Color.rgb(
                        45,
                        100,
                        255
                    ),

                    Color.argb(
                        30,
                        20,
                        50,
                        255
                    ),

                    Color.TRANSPARENT
                ),

                floatArrayOf(
                    0f,
                    0.16f,
                    0.40f,
                    0.68f,
                    0.86f,
                    1f
                ),

                Shader.TileMode.CLAMP
            )

        paint.shader =
            gradient

        canvas.drawCircle(
            cx,
            cy,
            radius,
            paint
        )

        paint.shader = null

        paint.clearShadowLayer()

        /*
         * Bright outer orb boundary.
         */
        paint.style =
            Paint.Style.STROKE

        paint.strokeWidth =
            2.5f

        paint.color =
            Color.argb(
                235,
                150,
                250,
                255
            )

        paint.setShadowLayer(
            12f,
            0f,
            0f,
            Color.argb(
                230,
                70,
                225,
                255
            )
        )

        canvas.drawCircle(
            cx,
            cy,
            radius * 1.08f,
            paint
        )

        paint.clearShadowLayer()

        /*
         * Inner energy core.
         */
        paint.style =
            Paint.Style.FILL

        paint.setShadowLayer(
            15f,
            0f,
            0f,
            Color.WHITE
        )

        paint.color =
            Color.argb(
                220,
                220,
                255,
                255
            )

        canvas.drawCircle(
            cx -
                radius * 0.18f,

            cy -
                radius * 0.20f,

            radius * 0.18f,

            paint
        )

        paint.clearShadowLayer()
    }

    private fun drawEnergyColumn(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float
    ) {

        val startY =
            cy +
                radius * 1.05f

        val endY =
            height * 0.76f

        /*
         * Main beam.
         */
        paint.style =
            Paint.Style.STROKE

        paint.strokeWidth =
            5f

        paint.color =
            Color.argb(
                95,
                70,
                220,
                255
            )

        paint.setShadowLayer(
            15f,
            0f,
            0f,
            Color.argb(
                150,
                50,
                210,
                255
            )
        )

        canvas.drawLine(
            cx,
            startY,
            cx,
            endY,
            paint
        )

        paint.clearShadowLayer()

        /*
         * Bright center beam.
         */
        paint.strokeWidth =
            1.5f

        paint.color =
            Color.argb(
                210,
                170,
                250,
                255
            )

        canvas.drawLine(
            cx,
            startY,
            cx,
            endY,
            paint
        )

        /*
         * Moving scan pulse.
         */
        val scanY =
            startY +
                (
                    endY -
                        startY
                ) *
                scan

        paint.style =
            Paint.Style.FILL

        paint.setShadowLayer(
            18f,
            0f,
            0f,
            Color.argb(
                230,
                70,
                235,
                255
            )
        )

        paint.color =
            Color.argb(
                220,
                170,
                255,
                255
            )

        canvas.drawCircle(
            cx,
            scanY,
            4.5f,
            paint
        )

        paint.clearShadowLayer()
   
