package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VisionTab
import com.example.viewmodel.VisionViewModel
import kotlin.math.cos
import kotlin.math.sin

private val VisionBlack = Color(0xFF000106)
private val NeonBlue = Color(0xFF16CFFF)
private val NeonCyan = Color(0xFF00E7FF)
private val NeonPurple = Color(0xFF9B20FF)
private val NeonViolet = Color(0xFF632CFF)
private val BrightWhite = Color(0xFFF7FAFF)
private val MutedBlue = Color(0xFF7891C8)

@Composable
fun DashboardScreen(
    viewModel: VisionViewModel,
    onNavigate: (VisionTab) -> Unit,
    onStartVoiceCall: () -> Unit,
    onNewChat: () -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dashboard")

    val rotation by infiniteTransition.animateFloat(
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

    val reverseRotation by infiniteTransition.animateFloat(
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

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2200,
                easing = EaseInOut
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = EaseInOut
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

        // ---------------------------------------------------------
        // TOP STATUS
        // ---------------------------------------------------------

        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(NeonCyan)
            )

            Spacer(modifier = Modifier.width(8.dp))

            androidx.compose.material3.Text(
                text = "SYSTEM LIVE",
                color = BrightWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            androidx.compose.material3.Text(
                text = "•",
                color = NeonPurple,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            androidx.compose.material3.Text(
                text = "Vision Core",
                color = MutedBlue,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }

        // ---------------------------------------------------------
        // MAIN ORBITAL CORE
        // ---------------------------------------------------------

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(330.dp),
            contentAlignment = Alignment.Center
        ) {

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

                // Outer glow
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

                // Rotating arc
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
                        width = 2.dp.toPx()
                    )
                )

                // Purple rotating arc
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
                        width = 2.dp.toPx()
                    )
                )

                // Orbital points
                val points = listOf(
                    0f,
                    60f,
                    120f,
                    180f,
                    240f,
                    300f
                )

                points.forEach { angle ->

                    val radians = Math.toRadians(angle.toDouble())

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

            // Inner rotating orbit
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

            // -----------------------------------------------------
            // CENTRAL VISION CORE
            // -----------------------------------------------------

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
                                NeonPurple.copy(alpha = glow * 0.5f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius
                        ),
                        center = center,
                        radius = radius
                    )

                    // Core outer circle
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

                // -------------------------------------------------
                // DIGITAL GLOBE
                // -------------------------------------------------

                Canvas(
                    modifier = Modifier
                        .size(145.dp)
                ) {

                    val center = Offset(
                        size.width / 2f,
                        size.height / 2f
                    )

                    val radius = size.minDimension / 2f

                    // Globe
                    drawCircle(
                        color = NeonBlue.copy(alpha = 0.06f),
                        center = center,
                        radius = radius
                    )

                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.55f),
                        center = center,
                        radius = radius - 2.dp.toPx(),
                        style = Stroke(
                            width = 1.dp.toPx()
                        )
                    )

                    // Latitude
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

                    // Longitude
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

                androidx.compose.material3.Text(
                    text = "V",
                    color = BrightWhite,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-2).sp
                )
            }
        }

        // ---------------------------------------------------------
        // CORE LABEL
        // ---------------------------------------------------------

        androidx.compose.material3.Text(
            text = "VISION CORE",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 126.dp),
            color = BrightWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )

        // ---------------------------------------------------------
        // CHAT BUTTON
        // ---------------------------------------------------------

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

        // ---------------------------------------------------------
        // SETTINGS BUTTON
        // ---------------------------------------------------------

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

        // ---------------------------------------------------------
        // VOICE BUTTON
        // ---------------------------------------------------------

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

        // ---------------------------------------------------------
        // HISTORY BUTTON
        // ---------------------------------------------------------

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

        // ---------------------------------------------------------
        // FOOTER
        // ---------------------------------------------------------

        androidx.compose.material3.Text(
            text = "T H I N K   •   A S K   •   E V O L V E",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            color = MutedBlue.copy(alpha = 0.75f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
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
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        androidx.compose.material3.IconButton(
            onClick = onClick,
            modifier = Modifier.size(66.dp)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.22f),
                                Color(0xFF050817),
                                VisionBlack
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {

                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {

                    drawCircle(
                        color = accent.copy(alpha = 0.65f),
                        radius = size.minDimension / 2f - 2.dp.toPx(),
                        style = Stroke(
                            width = 1.5.dp.toPx()
                        )
                    )

                    drawCircle(
                        color = accent.copy(alpha = 0.18f),
                        radius = size.minDimension / 2f - 8.dp.toPx(),
                        style = Stroke(
                            width = 1.dp.toPx()
                        )
                    )
                }

                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = BrightWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        androidx.compose.material3.Text(
            text = label,
            color = BrightWhite,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}
