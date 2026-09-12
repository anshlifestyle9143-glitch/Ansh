package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.VisionTab
import com.example.ui.viewmodel.VisionViewModel
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val NeonBlue = Color(0xFF19CFFF)
private val ElectricBlue = Color(0xFF3278FF)
private val NeonPurple = Color(0xFF9B25FF)
private val DeepPurple = Color(0xFF5415C9)
private val White = Color(0xFFF5F7FF)

@Composable
fun DashboardScreen(
    viewModel: VisionViewModel,
    onNavigate: (VisionTab) -> Unit,
    onStartVoiceCall: () -> Unit,
    onNewChat: () -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(26.dp))

            SystemLive()

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Vision Core",
                color = Color(0xFF7E86A7),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {

                val hubSize = minOf(maxWidth, 390.dp)

                VisionRadialCore(
                    modifier = Modifier
                        .size(hubSize)
                        .offset(y = 45.dp),
                    onChat = {
                        onNavigate(VisionTab.CHAT)
                    },
                    onSettings = {
                        onNavigate(VisionTab.SETTINGS)
                    },
                    onVoice = {
                        onStartVoiceCall()
                    },
                    onHistory = {
                        onShowHistory()
                    }
                )
            }

            Text(
                text = "T H I N K   •   A S K   •   E V O L V E",
                color = Color(0xFF3E4562),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SystemLive() {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(NeonBlue)
        )

        Spacer(modifier = Modifier.size(10.dp))

        Text(
            text = "SYSTEM LIVE",
            color = NeonBlue,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun VisionRadialCore(
    modifier: Modifier,
    onChat: () -> Unit,
    onSettings: () -> Unit,
    onVoice: () -> Unit,
    onHistory: () -> Unit
) {

    val transition = androidx.compose.animation.core.rememberInfiniteTransition(
        label = "vision_orbits"
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 14000,
                easing = LinearEasing
            )
        ),
        label = "orbitRotation"
    )

    val reverseRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 11000,
                easing = LinearEasing
            )
        ),
        label = "reverseOrbit"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    val particlePulse by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1300,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particlePulse"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            val center = Offset(
                size.width / 2f,
                size.height / 2f
            )

            val radius = min(
                size.width,
                size.height
            ) / 2f

            // =====================================================
            // MAIN ATMOSPHERIC GLOW
            // =====================================================

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonPurple.copy(alpha = 0.24f),
                        ElectricBlue.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // =====================================================
            // OUTER BROKEN ORBIT
            // =====================================================

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonPurple,
                        ElectricBlue,
                        NeonBlue,
                        Color.Transparent,
                        NeonPurple
                    )
                ),
                startAngle = rotation,
                sweepAngle = 125f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius * 0.96f,
                    center.y - radius * 0.96f
                ),
                size = androidx.compose.ui.geometry.Size(
                    radius * 1.92f,
                    radius * 1.92f
                ),
                style = Stroke(
                    width = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                brush = Brush.horizontalGradient(
                    listOf(
                        NeonBlue,
                        Color.Transparent,
                        NeonPurple
                    )
                ),
                startAngle = 165f,
                sweepAngle = 105f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius * 0.96f,
                    center.y - radius * 0.96f
                ),
                size = androidx.compose.ui.geometry.Size(
                    radius * 1.92f,
                    radius * 1.92f
                ),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // =====================================================
            // SECOND ORBIT
            // =====================================================

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonBlue,
                        NeonPurple,
                        Color.Transparent,
                        NeonBlue
                    )
                ),
                startAngle = reverseRotation,
                sweepAngle = 215f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius * 0.78f,
                    center.y - radius * 0.78f
                ),
                size = androidx.compose.ui.geometry.Size(
                    radius * 1.56f,
                    radius * 1.56f
                ),
                style = Stroke(
                    width = 2.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = NeonPurple.copy(alpha = 0.75f),
                startAngle = 250f,
                sweepAngle = 75f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius * 0.78f,
                    center.y - radius * 0.78f
                ),
                size = androidx.compose.ui.geometry.Size(
                    radius * 1.56f,
                    radius * 1.56f
                ),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // =====================================================
            // THIRD INNER ORBIT
            // =====================================================

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonPurple,
                        ElectricBlue,
                        NeonBlue,
                        NeonPurple
                    )
                ),
                startAngle = rotation * -0.7f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius * 0.59f,
                    center.y - radius * 0.59f
                ),
                size = androidx.compose.ui.geometry.Size(
                    radius * 1.18f,
                    radius * 1.18f
                ),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // =====================================================
            // INNER CORE RING
            // =====================================================

            drawCircle(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonBlue,
                        NeonPurple,
                        NeonBlue
                    )
                ),
                radius = radius * 0.42f,
                center = center,
                style = Stroke(
                    width = 3.dp.toPx()
                )
            )

            // =====================================================
            // INNER PURPLE GLOW
            // =====================================================

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonPurple.copy(alpha = 0.42f),
                        DeepPurple.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.48f
                ),
                radius = radius * 0.48f,
                center = center
            )

            // =====================================================
            // PARTICLES
            // =====================================================

            val particles = listOf(
                18f to 0.69f,
                62f to 0.58f,
                106f to 0.78f,
                144f to 0.47f,
                184f to 0.66f,
                225f to 0.73f,
                270f to 0.55f,
                312f to 0.82f,
                345f to 0.62f,
                82f to 0.74f,
                232f to 0.52f
            )

            particles.forEachIndexed { index, pair ->

                val angle = Math.toRadians(
                    pair.first.toDouble()
                ).toFloat()

                val distance = radius * pair.second

                val x = center.x + cos(angle) * distance
                val y = center.y + sin(angle) * distance

                val particleColor =
                    if (index % 2 == 0) {
                        NeonBlue
                    } else {
                        NeonPurple
                    }

                drawCircle(
                    color = particleColor.copy(
                        alpha = particlePulse
                    ),
                    radius = if (index % 3 == 0) {
                        3.5.dp.toPx()
                    } else {
                        2.dp.toPx()
                    },
                    center = Offset(x, y)
                )

                if (index % 3 == 0) {

                    drawCircle(
                        color = particleColor.copy(
                            alpha = 0.14f
                        ),
                        radius = 9.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            // =====================================================
            // MOVING ELECTRON
            // =====================================================

            val electronAngle = Math.toRadians(
                rotation.toDouble()
            ).toFloat()

            val electronDistance = radius * 0.73f

            val electron = Offset(
                center.x +
                        cos(electronAngle) *
                        electronDistance,
                center.y +
                        sin(electronAngle) *
                        electronDistance
            )

            drawCircle(
                color = NeonBlue.copy(alpha = 0.20f),
                radius = 10.dp.toPx(),
                center = electron
            )

            drawCircle(
                color = NeonBlue,
                radius = 3.dp.toPx(),
                center = electron
            )
        }

        // =========================================================
        // CENTRAL VISION SPHERE
        // =========================================================

        Box(
            modifier = Modifier
                .size(158.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                },
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonBlue.copy(alpha = 0.32f),
                                NeonPurple.copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Canvas(
                modifier = Modifier.size(136.dp)
            ) {

                val c = Offset(
                    size.width / 2f,
                    size.height / 2f
                )

                val r = size.minDimension / 2f

                // -------------------------------------------------
                // SPHERE GLOW
                // -------------------------------------------------

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonBlue.copy(alpha = 0.45f),
                            ElectricBlue.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    radius = r,
                    center = c
                )

                // -------------------------------------------------
                // SPHERE OUTLINE
                // -------------------------------------------------

                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            NeonBlue,
                            NeonPurple,
                            NeonBlue,
                            NeonBlue
                        )
                    ),
                    radius = r * 0.78f,
                    center = c,
                    style = Stroke(
                        width = 2.5.dp.toPx()
                    )
                )

                // -------------------------------------------------
                // LATITUDE LINES
                // -------------------------------------------------

                for (i in -3..3) {

                    val yOffset =
                        i * r * 0.18f

                    val ellipseHeight =
                        r * (
                            0.30f +
                                    (3 - kotlin.math.abs(i)) *
                                    0.07f
                            )

                    drawOval(
                        color = NeonBlue.copy(
                            alpha = 0.45f
                        ),
                        topLeft = Offset(
                            c.x - r * 0.72f,
                            c.y +
                                    yOffset -
                                    ellipseHeight / 2f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 1.44f,
                            ellipseHeight
                        ),
                        style = Stroke(
                            widt
