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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.rememberInfiniteTransition
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
private val CoreBlue = Color(0xFF087BFF)
private val Dark = Color(0xFF02030A)
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

            // SYSTEM LIVE
            RowSystemLive()

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
private fun RowSystemLive() {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF041B27),
                        Color(0xFF07121E)
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(NeonBlue)
        )

        androidx.compose.foundation.layout.Spacer(
            modifier = Modifier.size(10.dp)
        )

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

    val transition = rememberInfiniteTransition(label = "vision_orbits")

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

        // =========================================================
        // COMPLETE NEON HUD
        // =========================================================

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            val center = Offset(
                size.width / 2f,
                size.height / 2f
            )

            val radius = min(size.width, size.height) / 2f

            // -----------------------------------------------------
            // MASSIVE SOFT OUTER GLOW
            // -----------------------------------------------------

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonPurple.copy(alpha = 0.18f),
                        ElectricBlue.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.98f
                ),
                radius = radius * 0.98f,
                center = center
            )

            // -----------------------------------------------------
            // OUTER ORBIT - PURPLE
            // -----------------------------------------------------

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
                size = Size(
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
                size = Size(
                    radius * 1.92f,
                    radius * 1.92f
                ),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // -----------------------------------------------------
            // SECOND ORBIT
            // -----------------------------------------------------

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
                size = Size(
                    radius * 1.56f,
                    radius * 1.56f
                ),
                style = Stroke(
                    width = 2.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = NeonPurple.copy(alpha = 0.65f),
                startAngle = 250f,
                sweepAngle = 75f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius * 0.78f,
                    center.y - radius * 0.78f
                ),
                size = Size(
                    radius * 1.56f,
                    radius * 1.56f
                ),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // -----------------------------------------------------
            // THIRD ORBIT
            // -----------------------------------------------------

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
                size = Size(
                    radius * 1.18f,
                    radius * 1.18f
                ),
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // -----------------------------------------------------
            // INNER CORE RING
            // -----------------------------------------------------

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

            // -----------------------------------------------------
            // CORE PURPLE GLOW
            // -----------------------------------------------------

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonPurple.copy(alpha = 0.40f),
                        DeepPurple.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 0.48f
                ),
                radius = radius * 0.48f,
                center = center
            )

            // -----------------------------------------------------
            // ORBIT PARTICLES
            // -----------------------------------------------------

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

                val angle =
                    Math.toRadians(
                        pair.first.toDouble()
                    ).toFloat()

                val distance =
                    radius * pair.second

                val x =
                    center.x + cos(angle) * distance

                val y =
                    center.y + sin(angle) * distance

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
                        color = particleColor.copy(alpha = 0.14f),
                        radius = 9.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            // -----------------------------------------------------
            // SMALL ORBITING ELECTRON
            // -----------------------------------------------------

            val electronAngle =
                Math.toRadians(
                    rotation.toDouble()
                ).toFloat()

            val electronDistance =
                radius * 0.73f

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
        // CENTRAL VISION CORE
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

            // Outer glow
            Box(
                modifier = Modifier
                    .matchParentSize()
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

            // =====================================================
            // 3D DIGITAL SPHERE
            // =====================================================

            Canvas(
                modifier = Modifier
                    .size(132.dp)
                    .align(Alignment.Center)
            ) {

                val c = Offset(
                    size.width / 2f,
                    size.height / 2f
                )

                val r = size.minDimension / 2f

                // sphere glow
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

                // globe boundary
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

                // horizontal latitude lines
                for (i in -3..3) {

                    val yOffset =
                        i * r * 0.18f

                    val ellipseHeight =
                        r * (0.30f + (3 - kotlin.math.abs(i)) * 0.07f)

                    drawOval(
                        color = NeonBlue.copy(
                            alpha = 0.45f
                        ),
                        topLeft = Offset(
                            c.x - r * 0.72f,
                            c.y + yOffset - ellipseHeight / 2
                        ),
                        size = Size(
                            r * 1.44f,
                            ellipseHeight
                        ),
                        style = Stroke(
                            width = 1.dp.toPx()
                        )
                    )
                }

                // vertical longitude lines
                for (i in -2..2) {

                    val xOffset =
                        i * r * 0.22f

                    drawOval(
                        color = NeonPurple.copy(
                            alpha = 0.55f
                        ),
                        topLeft = Offset(
                            c.x + xOffset - r * 0.20f,
                            c.y - r * 0.76f
                        ),
                        size = Size(
                            r * 0.40f,
                            r * 1.52f
                        ),
                        style = Stroke(
                            width = 1.dp.toPx()
                        )
                    )
                }

                // sphere particles
                for (i in 0 until 26) {

                    val angle =
                        Math.toRadians(
                            (i * 47 % 360).toDouble()
                        ).toFloat()

                    val d =
                        r * (
                                0.35f +
                                        ((i * 13) % 45) /
                                        100f
                                )

                    drawCircle(
                        color =
                            if (i % 2 == 0) {
                                NeonBlue
                            } else {
                                NeonPurple
                            },
                        radius = 1.3.dp.toPx(),
                        center = Offset(
                            c.x + cos(angle) * d,
                            c.y + sin(angle) * d
                        )
                    )
                }

                // =================================================
                // BIG V
                // =================================================

                val vPath =
                    androidx.compose.ui.graphics.Path().apply {

                        moveTo(
                            c.x - r * 0.43f,
                            c.y - r * 0.40f
                        )

                        lineTo(
                            c.x,
                            c.y + r * 0.46f
                        )

                        lineTo(
                            c.x + r * 0.43f,
                            c.y - r * 0.40f
                        )
                    }

                drawPath(
                    path = vPath,
                    color = NeonBlue.copy(alpha = 0.20f),
                    style = Stroke(
                        width = 8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                drawPath(
                    path = vPath,
                    brush = Brush.verticalGradient(
                        listOf(
                            NeonBlue,
                            Color(0xFF75E8FF),
                            ElectricBlue
                        )
                    ),
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }

        // =========================================================
        // CHAT - TOP
        // =========================================================

        RadialAction(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp),
            icon = Icons.Default.ChatBubble,
            title = "CHAT",
            onClick = onChat
        )

        // =========================================================
        // SETTINGS - LEFT
        // =========================================================

        RadialAction(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-3).dp),
            icon = Icons.Default.Settings,
            title = "SETTINGS",
            onClick = onSettings
        )

        // =========================================================
        // VOICE - RIGHT
        // =========================================================

        RadialAction(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 3.dp),
            icon = Icons.Default.Mic,
            title = "VOICE",
            onClick = onVoice
        )

        // =========================================================
        // HISTORY - BOTTOM
        // =========================================================

        RadialAction(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 5.dp),
            icon = Icons.Default.History,
            title = "HISTORY",
            onClick = onHistory
        )
    }
}

@Composable
private fun RadialAction(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {

    Column(
        modifier = modifier
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(70.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = CircleShape,
                    ambientColor = NeonPurple,
                    spotColor = NeonBlue
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF172D92),
                            Color(0xFF0A0C35),
                            Color(0xFF08001A)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {

            Canvas(
                modifier = Modifier.matchParentSize()
            ) {

                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            NeonBlue,
                            NeonPurple,
                            NeonBlue
                        )
                    ),
                    radius = size.minDimension / 2f - 1.dp.toPx(),
                    style = Stroke(
                        width = 2.dp.toPx()
                    )
                )

                drawCircle(
                    color = NeonBlue.copy(alpha = 0.12f),
                    radius = size.minDimension * 0.38f
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = White,
                modifier = Modifier.size(31.dp)
            )
        }

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        Text(
            text = title,
            color = White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}
