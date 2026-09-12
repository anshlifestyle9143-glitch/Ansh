package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.VisionTab
import com.example.ui.viewmodel.VisionViewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin


/* ============================================================
   NEON COLORS
   ============================================================ */

private val NeonBlue = Color(0xFF00CFFF)
private val ElectricBlue = Color(0xFF2677FF)
private val NeonPurple = Color(0xFF9B20FF)
private val ElectricPurple = Color(0xFF6C35FF)
private val DeepBlue = Color(0xFF071638)
private val DeepPurple = Color(0xFF17072E)
private val CoreBlue = Color(0xFF123D9A)


/* ============================================================
   DASHBOARD
   ============================================================ */

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

            /* ================= SYSTEM LIVE ================= */

            Box(
                modifier = Modifier
                    .padding(top = 25.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Color(0xFF061C28)
                            .copy(alpha = 0.55f)
                    )
                    .border(
                        1.dp,
                        NeonBlue.copy(alpha = 0.65f),
                        RoundedCornerShape(30.dp)
                    )
                    .padding(
                        horizontal = 25.dp,
                        vertical = 10.dp
                    )
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {

                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(NeonBlue)
                    )

                    Spacer(
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

            /* ================= TITLE ================= */

            Text(
                text = "Vision Core",
                color = Color(0xFF7784A8),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 14.dp)
            )

            /* ================= MAIN HUD ================= */

            VisionHud(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),

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
                },

                onCore = {
                    onNavigate(VisionTab.CHAT)
                }
            )

            /* ================= FOOTER ================= */

            Text(
                text = "T H I N K   •   A S K   •   E V O L V E",
                color = Color(0xFF4D5878),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
                modifier = Modifier.padding(
                    bottom = 28.dp
                )
            )
        }
    }
}


/* ============================================================
   MAIN HUD
   ============================================================ */

@Composable
private fun VisionHud(
    modifier: Modifier = Modifier,
    onChat: () -> Unit,
    onSettings: () -> Unit,
    onVoice: () -> Unit,
    onHistory: () -> Unit,
    onCore: () -> Unit
) {

    val transition =
        rememberInfiniteTransition(
            label = "vision_hud"
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
        label = "outer_rotation"
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
        label = "reverse_rotation"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                1700,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        /* ====================================================
           ORBIT FIELD
           ==================================================== */

        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxSize(0.72f)
                .graphicsLayer {
                    rotationZ = rotation
                }
        ) {

            val stroke = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            /* OUTER ARC 1 */

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonBlue,
                        ElectricBlue,
                        NeonPurple,
                        Color.Transparent,
                        NeonBlue
                    )
                ),
                startAngle = -28f,
                sweepAngle = 120f,
                useCenter = false,
                style = stroke
            )

            /* OUTER ARC 2 */

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        NeonPurple,
                        ElectricPurple,
                        NeonBlue
                    )
                ),
                startAngle = 145f,
                sweepAngle = 105f,
                useCenter = false,
                style = stroke
            )

            /* OUTER ARC 3 */

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonBlue,
                        Color.Transparent,
                        NeonPurple
                    )
                ),
                startAngle = 285f,
                sweepAngle = 55f,
                useCenter = false,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }


        /* ====================================================
           SECOND ORBIT
           ==================================================== */

        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .fillMaxSize(0.60f)
                .graphicsLayer {
                    rotationZ = reverseRotation
                }
        ) {

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonPurple,
                        ElectricPurple,
                        Color.Transparent,
                        NeonBlue,
                        NeonPurple
                    )
                ),
                startAngle = 5f,
                sweepAngle = 210f,
                useCenter = false,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        NeonBlue,
                        Color.Transparent,
                        NeonPurple
                    )
                ),
                startAngle = 245f,
                sweepAngle = 85f,
                useCenter = false,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }


        /* ====================================================
           INNER ORBIT
           ==================================================== */

        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.63f)
                .fillMaxSize(0.47f)
                .graphicsLayer {
                    rotationZ = rotation * 1.25f
                }
        ) {

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonBlue,
                        ElectricBlue,
                        NeonPurple,
                        Color.Transparent,
                        NeonBlue
                    )
                ),
                startAngle = -30f,
                sweepAngle = 275f,
                useCenter = false,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = NeonBlue.copy(alpha = 0.95f),
                startAngle = 105f,
                sweepAngle = 38f,
                useCenter = false,
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }


        /* ====================================================
           AMBIENT CORE GLOW
           ==================================================== */

        Box(
            modifier = Modifier
                .size(255.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonPurple.copy(alpha = 0.25f),
                            ElectricBlue.copy(alpha = 0.18f),
                            DeepPurple.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )


        /* ====================================================
           FLOATING PARTICLES
           ==================================================== */

        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .fillMaxSize(0.54f)
        ) {

            val particles = listOf(
                0.12f to 0.38f,
                0.19f to 0.22f,
                0.28f to 0.13f,
                0.40f to 0.08f,
                0.58f to 0.12f,
                0.72f to 0.21f,
                0.84f to 0.39f,
                0.77f to 0.61f,
                0.68f to 0.82f,
                0.51f to 0.91f,
                0.32f to 0.83f,
                0.18f to 0.67f,
                0.08f to 0.51f
            )

            particles.forEachIndexed { index, point ->

                val x = size.width * point.first
                val y = size.height * point.second

                val particleColor =
                    if (index % 2 == 0) {
                        NeonBlue
                    } else {
                        NeonPurple
                    }

                drawCircle(
                    color = particleColor.copy(
                        alpha = 0.85f
                    ),
                    radius = if (index % 4 == 0) {
                        2.5.dp.toPx()
                    } else {
                        1.5.dp.toPx()
                    },
                    center = Offset(x, y)
                )

                if (index % 4 == 0) {

                    drawCircle(
                        color = particleColor.copy(
                            alpha = 0.15f
                        ),
                        radius = 8.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }


        /* ====================================================
           CHAT — TOP
           ==================================================== */

        HubAction(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),

            icon = Icons.Default.Chat,
            title = "Chat",

            onClick = onChat
        )


        /* ====================================================
           SETTINGS — LEFT
           ==================================================== */

        HubAction(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 2.dp),

            icon = Icons.Default.Settings,
            title = "Settings",

            onClick = onSettings
        )


        /* ====================================================
           VOICE — RIGHT
           ==================================================== */

        HubAction(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 2.dp),

            icon = Icons.Default.RecordVoiceOver,
            title = "Voice",

            onClick = onVoice
        )


        /* ====================================================
           HISTORY — BOTTOM
           ==================================================== */

        HubAction(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 15.dp),

            icon = Icons.Default.History,
            title = "History",

            onClick = onHistory
        )


        /* ====================================================
           VISION ENERGY CORE
           ==================================================== */

        VisionEnergyCore(
            modifier = Modifier
                .size(190.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                },

            onClick = onCore
        )
    }
}


/* ============================================================
   ACTION BUTTON
   ============================================================ */

@Composable
private fun HubAction(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {

    Column(
        modifier = modifier
            .clickable {
                onClick()
            }
            .padding(5.dp),

        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF183FBA),
                            Color(0xFF17115D),
                            Color(0xFF08051C)
                        )
                    )
                )
                .border(
                    2.dp,
                    Brush.linearGradient(
                        listOf(
                            NeonBlue,
                            ElectricBlue,
                            NeonPurple
                        )
                    ),
                    CircleShape
                ),

            contentAlignment = Alignment.Center
        ) {

            /* glow ring */

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonBlue.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        Text(
            text = title.uppercase(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}


/* ============================================================
   VISION ENERGY CORE
   ============================================================ */

@Composable
private fun VisionEnergyCore(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    val transition =
        rememberInfiniteTransition(
            label = "energy_core"
        )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                9000,
                easing = LinearEasing
            )
        ),
        label = "globe_rotation"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable {
                onClick()
            },

        contentAlignment = Alignment.Center
    ) {

        /* ====================================================
           CORE OUTER GLOW
           ==================================================== */

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonBlue.copy(alpha = 0.42f),
                            ElectricBlue.copy(alpha = 0.25f),
                            NeonPurple.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    )
                )
        )


        /* ====================================================
           DARK CORE
           ==================================================== */

        Box(
            modifier = Modifier
                .size(158.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0D2C65),
                            Color(0xFF071737),
                            Color(0xFF01040C)
                        )
                    )
                )
        )


        /* ====================================================
           ENERGY GLOBE
           ==================================================== */

        EnergyGlobe(
            modifier = Modifier.size(150.dp),
            rotation = rotation
        )


        /* ====================================================
           GLOWING V
           ==================================================== */

        Canvas(
            modifier = Modifier.size(88.dp)
        ) {

            val path = Path().apply {

                moveTo(
                    size.width * 0.08f,
                    size.height * 0.15f
                )

                lineTo(
                    size.width * 0.50f,
                    size.height * 0.84f
                )

                lineTo(
                    size.width * 0.92f,
                    size.height * 0.15f
                )
            }

            /* BIG GLOW */

            drawPath(
                path = path,
                color = NeonBlue.copy(
                    alpha = 0.25f
                ),
                style = Stroke(
                    width = 15.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            /* PURPLE GLOW */

            drawPath(
                path = path,
                color = NeonPurple.copy(
                    alpha = 0.28f
                ),
                style = Stroke(
                    width = 9.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            /* BRIGHT V */

            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White,
                        NeonBlue,
                        ElectricBlue,
                        NeonPurple
                    )
                ),
                style = Stroke(
                    width = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}


/* ============================================================
   ENERGY GLOBE
   ============================================================ */

@Composable
private fun EnergyGlobe(
    modifier: Modifier = Modifier,
    rotation: Float
) {

    Canvas(
        modifier = modifier
    ) {

        val cx = size.width / 2f
        val cy = size.height / 2f

        /* ====================================================
           GLOBE GLOW
           ==================================================== */

        drawCircle(
            color = NeonBlue.copy(alpha = 0.18f),
            radius = size.minDimension * 0.46f,
            center = Offset(cx, cy)
        )


        /* ====================================================
           OUTER SPHERE
           ==================================================== */

        drawCircle(
            color = NeonBlue.copy(alpha = 0.72f),
            radius = size.minDimension * 0.43f,
            center = Offset(cx, cy),
            style = Stroke(
                width = 1.5.dp.toPx()
            )
        )


        /* ====================================================
           EQUATOR
           ==================================================== */

        drawOval(
            brush = Brush.sweepGradient(
                colors = listOf(
                    NeonBlue,
                    Color.Transparent,
                    NeonPurple,
                    NeonBlue
                )
            ),
            topLeft = Offset(
                size.width * 0.04f,
                size.height * 0.32f
            ),
            size = Size(
                size.width * 0.92f,
                size.height * 0.36f
            ),
            style = Stroke(
                width = 2.dp.toPx()
            )
        )


        /* ====================================================
           LATITUDE LINES
           ==================================================== */

        val latitudes = listOf(
            0.18f,
            0.27f,
            0.38f,
            0.50f,
            0.62f,
            0.73f,
            0.82f
        )

        latitudes.forEachIndexed { index, y ->

            val distance =
                abs(y - 0.5f) * 2f

            val widthFactor =
                0.35f + (1f - distance) * 0.58f

            drawOval(
                color =
                    if (index % 2 == 0) {
                        NeonBlue.copy(alpha = 0.58f)
                    } else {
                        NeonPurple.copy(alpha = 0.48f)
                    },

                topLeft = Offset(
                    cx -
                        size.width *
                        widthFactor /
                        2f,

                    size.height * y -
                        size.height * 0.045f
                ),

                size = Size(
                    size.width * widthFactor,
                    size.height * 0.09f
                ),

                style = Stroke(
                    width = 0.9.dp.toPx()
                )
            )
        }


        /* ====================================================
           LONGITUDE LINES
           ==================================================== */

        val longitudeAngles =
            listOf(
                -55f,
                -40f,
                -25f,
                -12f,
                0f,
                12f,
                25f,
                40f,
                55f
            )

        longitudeAngles.forEachIndexed {
                index,
                angleDeg
            ->

            val angle =
                Math.toRadians(
                    (
                        angleDeg +
                            rotation * 0.12f
                        ).toDouble()
                )

            val horizontalScale =
                abs(
                    cos(angle)
                        .toFloat()
                )

            val ovalWidth =
                size.width *
                    (
                        0.16f +
                            horizontalScale *
                            0.76f
                        )

            drawOval(
                color =
                    if (index % 2 == 0) {
                        NeonBlue.copy(alpha = 0.60f)
                    } else {
                        NeonPurple.copy(alpha = 0.48f)
                    },

                topLeft = Offset(
                    cx -
                        ovalWidth / 2f,

                    size.height * 0.04f
                ),

                size = Size(
                    ovalWidth,
                    size.height * 0.92f
                ),

                style = Stroke(
                    width = 0.9.dp.toPx()
                )
            )
        }


        /* ====================================================
           DIAGONAL ENERGY ORBITS
           ==================================================== */

        drawOval(
            brush = Brush.sweepGradient(
                colors = listOf(
                    NeonBlue,
                    Color.Transparent,
                    NeonPurple,
                    Color.Transparent,
                    NeonBlue
                )
            ),

            topLeft = Offset(
                size.width * 0.01f,
                size.height * 0.27f
            ),

            size = Size(
                size.width * 0.98f,
                size.height * 0.46f
            ),

            style = Stroke(
                width = 1.4.dp.toPx()
            )
        )


        drawOval(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    NeonPurple,
                    NeonBlue,
                    Color.Transparent
                )
            ),

            topLeft = Offset(
                size.width * 0.12f,
                size.height * 0.05f
            ),

            size = Size(
                size.width * 0.76f,
                size.height * 0.90f
            ),

            style = Stroke(
                width = 1.dp.toPx()
            )
        )


        /* ====================================================
           ENERGY PARTICLES
           ==================================================== */

        val stars = listOf(
            0.17f to 0.36f,
            0.25f to 0.22f,
            0.36f to 0.13f,
            0.52f to 0.10f,
            0.66f to 0.20f,
            0.79f to 0.39f,
            0.73f to 0.60f,
            0.63f to 0.77f,
            0.49f to 0.88f,
            0.34f to 0.77f,
            0.23f to 0.59f,
            0.12f to 0.48f
        )

        stars.forEachIndexed { index, point ->

            val x =
                size.width *
                    point.first

            val y =
                size.height *
                    point.second

            val color =
                if (index % 2 == 0) {
                    NeonBlue
                } else {
                    NeonPurple
                }

            drawCircle(
                color = color,
                radius =
                    if (index % 3 == 0) {
                        2.dp.toPx()
                    } else {
                        1.1.dp.toPx()
                    },
                center = Offset(x, y)
            )

            if (index % 4 == 0) {

                drawCircle(
                    color = color.copy(
                        alpha = 0.18f
                    ),
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }


        /* ====================================================
           MOVING ORBIT DOTS
           ==================================================== */

        val orbitRadius =
            size.width * 0.39f

        for (i in 0 until 8) {

            val angle =
                Math.toRadians(
                    (
                        rotation +
                            i * 45f
                        ).toDouble()
                )

            val x =
                cx +
                    cos(angle).toFloat() *
                    orbitRadius

            val y =
                cy +
                    sin(angle).toFloat() *
                    orbitRadius

            drawCircle(
                color =
                    if (i % 2 == 0) {
                        NeonBlue
                    } else {
                        NeonPurple
                    },

                radius = 1.6.dp.toPx(),

                center = Offset(
                    x,
                    y
                )
            )
        }
    }
}
