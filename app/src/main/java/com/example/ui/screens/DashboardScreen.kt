
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.VisionTab
import com.example.ui.viewmodel.VisionViewModel
import kotlin.math.cos
import kotlin.math.sin

// ================================================================
// NEW DESIGN COLORS
// ================================================================

private val VisionBlack = Color(0xFF02050D)
private val NeonBlue = Color(0xFF00D2FF)
private val NeonCyan = Color(0xFF00E7FF)
private val ElectricBlue = Color(0xFF0066FF)
private val NeonPurple = Color(0xFF7B2CFF)
private val NeonViolet = Color(0xFF5A35FF)
private val MutedTextBlue = Color(0xFF6B7C96)
private val BrightWhite = Color(0xFFFFFFFF)

// ================================================================
// DASHBOARD SCREEN
// ================================================================

@Composable
fun DashboardScreen(
viewModel: VisionViewModel,
onNavigate: (VisionTab) -> Unit,
onStartVoiceCall: () -> Unit,
onNewChat: () -> Unit,
onShowHistory: () -> Unit,
modifier: Modifier = Modifier
) {

val infinite = rememberInfiniteTransition(
    label = "vision_dashboard"
)

// OLD FEATURE: outer orbital rotation
val rotation by infinite.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
        animation = tween(
            durationMillis = 18000,
            easing = LinearEasing
        ),
        repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
)

// OLD FEATURE: reverse inner rotation
val reverseRotation by infinite.animateFloat(
    initialValue = 360f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
        animation = tween(
            durationMillis = 12000,
            easing = LinearEasing
        ),
        repeatMode = RepeatMode.Restart
    ),
    label = "reverse_rotation"
)

// OLD FEATURE: core pulse
val pulse by infinite.animateFloat(
    initialValue = 0.96f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
        animation = tween(
            durationMillis = 2200,
            easing = FastOutSlowInEasing
        ),
        repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
)

// OLD FEATURE: core glow
val glow by infinite.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.75f,
    animationSpec = infiniteRepeatable(
        animation = tween(
            durationMillis = 1800,
            easing = FastOutSlowInEasing
        ),
        repeatMode = RepeatMode.Reverse
    ),
    label = "glow"
)

Box(
    modifier = modifier
        .fillMaxSize()
        .background(VisionBlack)
) {

    // ========================================================
    // NEW DESIGN: TOP HEADER
    // ========================================================

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 24.dp,
                top = 28.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            1.5.dp,
                            NeonBlue,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "V",
                        color = NeonBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.size(8.dp)
                )

                Text(
                    text = "Vision •",
                    color = BrightWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "BY ANSH YADAV",
                color = MutedTextBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(
                    start = 2.dp,
                    top = 2.dp
                )
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Color(0xFF0F1A30)
                )
                .padding(
                    horizontal = 14.dp,
                    vertical = 6.dp
                )
        ) {

            Text(
                text = "Vision Core",
                color = ElectricBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }


    // ========================================================
    // NEW DESIGN: SYSTEM LIVE STATUS
    // ========================================================

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 110.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .border(
                    1.dp,
                    Color(0xFF00E676),
                    RoundedCornerShape(20.dp)
                )
                .padding(
                    horizontal = 16.dp,
                    vertical = 6.dp
                )
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            Color(0xFF00E676)
                        )
                )

                Spacer(
                    modifier = Modifier.size(8.dp)
                )

                Text(
                    text = "SYSTEM LIVE",
                    color = Color(0xFF00E676),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Vision Core",
            color = MutedTextBlue,
            fontSize = 14.sp
        )
    }


    // ========================================================
    // OLD FEATURE: MAIN ORBITAL CORE
    // ========================================================

    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .size(340.dp),
        contentAlignment = Alignment.Center
    ) {

        // ====================================================
        // OLD FEATURE: OUTER ORBITS + ROTATING ARCS
        // ====================================================

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
        ) {

            val center = Offset(
                size.width / 2f,
                size.height / 2f
            )

            val radius = size.minDimension / 2f

            // NEW COLOR: outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonBlue.copy(alpha = 0.16f),
                        NeonPurple.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                center = center,
                radius = radius
            )

            // Outer orbit
            drawCircle(
                color = NeonBlue.copy(alpha = 0.22f),
                center = center,
                radius = radius - 18.dp.toPx(),
                style = Stroke(
                    width = 1.2.dp.toPx()
                )
            )

            // Second orbit
            drawCircle(
                color = NeonPurple.copy(alpha = 0.25f),
                center = center,
                radius = radius - 38.dp.toPx(),
                style = Stroke(
                    width = 1.dp.toPx()
                )
            )

            // Rotating cyan arc
            drawArc(
                color = NeonCyan.copy(alpha = 0.8f),
                startAngle = 20f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius + 18.dp.toPx(),
                    center.y - radius + 18.dp.toPx()
                ),
                size = Size(
                    (radius - 18.dp.toPx()) * 2f,
                    (radius - 18.dp.toPx()) * 2f
                ),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // Rotating purple arc
            drawArc(
                color = NeonPurple.copy(alpha = 0.7f),
                startAngle = 210f,
                sweepAngle = 65f,
                useCenter = false,
                topLeft = Offset(
                    center.x - radius + 38.dp.toPx(),
                    center.y - radius + 38.dp.toPx()
                ),
                size = Size(
                    (radius - 38.dp.toPx()) * 2f,
                    (radius - 38.dp.toPx()) * 2f
                ),
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // OLD FEATURE: orbital points
            val points = listOf(
                0f,
                60f,
                120f,
                180f,
                240f,
                300f
            )

            points.forEach { angle ->

                val radians = Math.toRadians(
                    angle.toDouble()
                )

                val x = center.x +
                        cos(radians).toFloat() *
                        (radius - 18.dp.toPx())

                val y = center.y +
                        sin(radians).toFloat() *
                        (radius - 18.dp.toPx())

                drawCircle(
                    color = NeonCyan.copy(alpha = 0.8f),
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }


        // ====================================================
        // OLD FEATURE: INNER ROTATING ORBIT
        // ====================================================

        Canvas(
            modifier = Modifier
                .size(275.dp)
                .rotate(reverseRotation)
        ) {

            val center = Offset(
                size.width / 2f,
                size.height / 2f
            )

            val radius = size.minDimension / 2f

            drawArc(
                color = NeonPurple.copy(alpha = 0.55f),
                startAngle = 120f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset.Zero,
                size = size,
                style = Stroke(
                    width = 1.5.dp.toPx()
                )
            )

            drawArc(
                color = NeonBlue.copy(alpha = 0.5f),
                startAngle = 300f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset.Zero,
                size = size,
                style = Stroke(
                    width = 1.5.dp.toPx()
                )
            )

            drawCircle(
                color = NeonBlue.copy(alpha = 0.12f),
                center = center,
                radius = radius - 25.dp.toPx(),
                style = Stroke(
                    width = 1.dp.toPx()
                )
            )
        }


        // ====================================================
        // OLD FEATURE: CENTRAL VISION CORE
        // ====================================================

        Box(
            modifier = Modifier
                .size(205.dp)
                .scale(pulse),
            contentAlignment = Alignment.Center
        ) {

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {

                val center = Offset(
                    size.width / 2f,
                    size.height / 2f
                )

                val radius = size.minDimension / 2f

                // Core glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonBlue.copy(alpha = glow),
                            NeonPurple.copy(
                                alpha = glow * 0.5f
                            ),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    center = center,
                    radius = radius
                )

                // Core background
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF071C3A),
                            Color(0xFF040817),
                            VisionBlack
                        )
                    ),
                    center = center,
                    radius = radius - 5.dp.toPx()
                )

                // Outer ring
                drawCircle(
                    color = NeonBlue.copy(alpha = 0.7f),
                    center = center,
                    radius = radius - 6.dp.toPx(),
                    style = Stroke(
                        width = 2.dp.toPx()
                    )
                )

                // Inner ring
                drawCircle(
                    color = NeonPurple.copy(alpha = 0.45f),
                    center = center,
                    radius = radius - 18.dp.toPx(),
                    style = Stroke(
                        width = 1.dp.toPx()
                    )
                )
            }


            // =================================================
            // OLD FEATURE: DIGITAL GLOBE
            // =================================================

            Canvas(
                modifier = Modifier.size(145.dp)
            ) {

                val center = Offset(
                    size.width / 2f,
                    size.height / 2f
                )

                val radius = size.minDimension / 2f

                // Globe background
                drawCircle(
                    color = NeonBlue.copy(alpha = 0.06f),
                    center = center,
                    radius = radius
                )

                // Globe border
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.55f),
                    center = center,
                    radius = radius - 2.dp.toPx(),
                    style = Stroke(
                        width = 1.dp.toPx()
                    )
                )

                // Latitude lines
                drawOval(
                    color = NeonBlue.copy(alpha = 0.3f),
                    topLeft = Offset(
                        center.x - radius,
                        center.y - radius * 0.45f
                    ),
                    size = Size(
                        radius * 2f,
                        radius * 0.9f
                    ),
                    style = Stroke(
                        width = 0.8.dp.toPx()
                    )
                )

                drawOval(
                    color = NeonBlue.copy(alpha = 0.3f),
                    topLeft = Offset(
                        center.x - radius,
                        center.y - radius * 0.15f
                    ),
                    size = Size(
                        radius * 2f,
                        radius * 0.3f
                    ),
                    style = Stroke(
                        width = 0.8.dp.toPx()
                    )
                )

                drawOval(
                    color = NeonBlue.copy(alpha = 0.3f),
                    topLeft = Offset(
                        center.x - radius,
                        center.y + radius * 0.15f
                    ),
                    size = Size(
                        radius * 2f,
                        radius * 0.3f
                    ),
                    style = Stroke(
                        width = 0.8.dp.toPx()
                    )
                )

                // Longitude lines
                drawOval(
                    color = NeonPurple.copy(alpha = 0.3f),
                    topLeft = Offset(
                        center.x - radius * 0.45f,
                        center.y - radius
                    ),
                    size = Size(
                        radius * 0.9f,
                        radius * 2f
                    ),
                    style = Stroke(
                        width = 0.8.dp.toPx()
                    )
                )

                drawOval(
                    color = NeonPurple.copy(alpha = 0.3f),
                    topLeft = Offset(
                        center.x - radius * 0.15f,
                        center.y - radius
                    ),
                    size = Size(
                        radius * 0.3f,
                        radius * 2f
                    ),
                    style = Stroke(
                        width = 0.8.dp.toPx()
                    )
                )

                drawOval(
                    color = NeonPurple.copy(alpha = 0.3f),
                    topLeft = Offset(
                        center.x + radius * 0.15f,
                        center.y - radius
                    ),
                    size = Size(
                        radius * 0.3f,
                        radius * 2f
                    ),
                    style = Stroke(
                        width = 0.8.dp.toPx()
                    )
                )

                // Globe particles
                val particles = listOf(
                    Offset(0.25f, 0.30f),
                    Offset(0.70f, 0.25f),
                    Offset(0.45f, 0.42f),
                    Offset(0.65f, 0.58f),
                    Offset(0.30f, 0.65f),
                    Offset(0.52f, 0.76f),
                    Offset(0.80f, 0.72f),
                    Offset(0.18f, 0.48f)
                )

                particles.forEach { p ->

                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.7f),
                        radius = 1.5.dp.toPx(),
                        center = Offset(
                            size.width * p.x,
                            size.height * p.y
                        )
                    )
                }
            }


            // Central V
            Text(
                text = "V",
                color = BrightWhite,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-2).sp
            )
        }
    }


    // ========================================================
    // NEW DESIGN: CORE LABEL
    // ========================================================

    Text(
        text = "VISION CORE",
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = 126.dp),
        color = BrightWhite,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 4.sp
    )


    // ========================================================
    // OLD FEATURE: CHAT
    // ========================================================

    DashboardAction(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = (-165).dp),
        icon = Icons.Default.Chat,
        label = "CHAT",
        accent = NeonBlue,
        onClick = {
            onNewChat()
            onNavigate(VisionTab.CHAT)
        }
    )


    // ========================================================
    // OLD FEATURE: SETTINGS
    // ========================================================

    DashboardAction(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(
                x = (-145).dp,
                y = (-5).dp
            ),
        icon = Icons.Default.Settings,
        label = "SETTINGS",
        accent = NeonPurple,
        onClick = {
            onNavigate(VisionTab.SETTINGS)
        }
    )


    // ========================================================
    // OLD FEATURE: VOICE
    // ========================================================

    DashboardAction(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(
                x = 145.dp,
                y = (-5).dp
            ),
        icon = Icons.Default.Mic,
        label = "VOICE",
        accent = NeonCyan,
        onClick = {
            onNewChat()
            onStartVoiceCall()
        }
    )


    // ========================================================
    // OLD FEATURE: HISTORY
    // ========================================================

    DashboardAction(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = 165.dp),
        icon = Icons.Default.History,
        label = "HISTORY",
        accent = NeonViolet,
        onClick = {
            onShowHistory()
        }
    )


    // ========================================================
    // NEW DESIGN: FOOTER
    // ========================================================

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(
                horizontal = 24.dp,
                bottom = 28.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {

        Column {

            Text(
                text = "VISION",
                color = BrightWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Text(
                text = "BY ANSH YADAV",
                color = MutedTextBlue,
                fontSize = 8.sp
            )
        }

        Text(
            text = "THINK  •  ASK  •  EVOLVE",
            color = MutedTextBlue.copy(
                alpha = 0.7f
            ),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}

}

// ================================================================
// DASHBOARD ACTION BUTTON
// ================================================================

@Composable
private fun DashboardAction(
modifier: Modifier = Modifier,
icon: androidx.compose.ui.graphics.vector.ImageVector,
label: String,
accent: Color,
onClick: () -> Unit
) {

Column(
    modifier = modifier.clickable {
        onClick()
    },
    horizontalAlignment = Alignment.CenterHorizontally
) {

    Box(
        modifier = Modifier
            .size(66.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.22f),
                        Color(0xFF050E1E),
                        VisionBlack
                    )
                )
            )
            .border(
                1.5.dp,
                accent.copy(alpha = 0.8f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {

            drawCircle(
                color = accent.copy(alpha = 0.18f),
                radius = size.minDimension / 2f - 8.dp.toPx(),
                style = Stroke(
                    width = 1.dp.toPx()
                )
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = BrightWhite,
            modifier = Modifier.size(24.dp)
        )
    }

    Spacer(
        modifier = Modifier.height(6.dp)
    )

    Text(
        text = label,
        color = MutedTextBlue,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        textAlign = TextAlign.Center
    )
}
}
