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

    val infinite = rememberInfiniteTransition(label = "vision_dashboard")

    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 18000,
                easing = LinearEasing
            )
        ),
        label = "orbit_rotation"
    )

    val reverseRotation by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 24000,
                easing = LinearEasing
            )
        ),
        label = "reverse_orbit"
    )

    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1700,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    val glow by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_glow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VisionBlack)
    ) {

        /*
         * =========================================================
         * TOP SYSTEM STATUS
         * =========================================================
         */

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(40.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                NeonCyan.copy(alpha = 0.10f),
                                NeonBlue.copy(alpha = 0.08f),
                                NeonPurple.copy(alpha = 0.10f)
                            )
                        )
                    )
                    .padding(
                        horizontal = 30.dp,
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
                            .background(NeonCyan)
                    )

                    Spacer(modifier = Modifier.size(11.dp))

                    Text(
                        text = "SYSTEM LIVE",
                        color = NeonCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Vision Core",
                color = MutedBlue,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }

        /*
         * =========================================================
         * MAIN ORBITAL CORE
         * =========================================================
         */

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 155.dp, bottom = 145.dp)
        ) {

            /*
             * =====================================================
             * ORBIT CANVAS
             * =====================================================
             */

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {

                val centerX = size.width / 2f
                val centerY = size.height / 2f

                val baseRadius =
                    minOf(size.width, size.height) * 0.39f

                /*
                 * -------------------------
                 * SOFT CENTRAL GLOW
                 * -------------------------
                 */

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonPurple.copy(alpha = 0.30f * glow),
                            NeonBlue.copy(alpha = 0.13f * glow),
                            Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(
                            centerX,
                            centerY
                        ),
                        radius = baseRadius * 0.78f
                    ),
                    radius = baseRadius * 0.78f,
                    center = androidx.compose.ui.geometry.Offset(
                        centerX,
                        centerY
                    )
                )

                /*
                 * -------------------------
                 * OUTER ORBIT
                 * -------------------------
                 */

                drawCircle(
                    color = NeonPurple.copy(alpha = 0.72f),
                    radius = baseRadius,
                    center = androidx.compose.ui.geometry.Offset(
                        centerX,
                        centerY
                    ),
                    style = Stroke(width = 2.5f)
                )

                /*
                 * -------------------------
                 * SECOND ORBIT
                 * -------------------------
                 */

                drawCircle(
                    color = NeonBlue.copy(alpha = 0.82f),
                    radius = baseRadius * 0.82f,
                    center = androidx.compose.ui.geometry.Offset(
                        centerX,
                        centerY
                    ),
                    style = Stroke(width = 3f)
                )

                /*
                 * -------------------------
                 * INNER ORBIT
                 * -------------------------
                 */

                drawCircle(
                    color = NeonPurple.copy(alpha = 0.88f),
                    radius = baseRadius * 0.61f,
                    center = androidx.compose.ui.geometry.Offset(
                        centerX,
                        centerY
                    ),
                    style = Stroke(width = 3f)
                )

                /*
                 * -------------------------
                 * CYAN ORBIT
                 * -------------------------
                 */

                drawCircle(
                    color = NeonCyan.copy(alpha = 0.72f),
                    radius = baseRadius * 0.48f,
                    center = androidx.compose.ui.geometry.Offset(
                        centerX,
                        centerY
                    ),
                    style = Stroke(width = 2.5f)
                )

                /*
                 * =================================================
                 * ROTATING ARC SYSTEM
                 * =================================================
                 */

                val arcRectSize = baseRadius * 2f

                val arcTopLeft = androidx.compose.ui.geometry.Offset(
                    centerX - baseRadius,
                    centerY - baseRadius
                )

                drawArc(
                    color = NeonCyan,
                    startAngle = rotation,
                    sweepAngle = 72f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = androidx.compose.ui.geometry.Size(
                        arcRectSize,
                        arcRectSize
                    ),
                    style = Stroke(
                        width = 5f,
                        cap = StrokeCap.Round
                    )
                )

                drawArc(
                    color = NeonPurple,
                    startAngle = rotation + 115f,
                    sweepAngle = 95f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = androidx.compose.ui.geometry.Size(
                        arcRectSize,
                        arcRectSize
                    ),
                    style = Stroke(
                        width = 4f,
                        cap = StrokeCap.Round
                    )
                )

                drawArc(
                    color = NeonBlue,
                    startAngle = reverseRotation + 40f,
                    sweepAngle = 55f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = androidx.compose.ui.geometry.Size(
                        arcRectSize,
                        arcRectSize
                    ),
                    style = Stroke(
                        width = 5f,
                        cap = StrokeCap.Round
                    )
                )

                /*
                 * =================================================
                 * SECOND ROTATING RING
                 * =================================================
                 */

                val secondRadius = baseRadius * 0.82f

                val secondTopLeft = androidx.compose.ui.geometry.Offset(
                    centerX - secondRadius,
                    centerY - secondRadius
                )

                drawArc(
                    color = NeonPurple,
                    startAngle = reverseRotation,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = secondTopLeft,
                    size = androidx.compose.ui.geometry.Size(
                        secondRadius * 2f,
                        secondRadius * 2f
                    ),
                    style = Stroke(
                        width = 4f,
                        cap = StrokeCap.Round
                    )
                )

                drawArc(
                    color = NeonCyan,
                    startAngle = rotation + 170f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = secondTopLeft,
                    size = androidx.compose.ui.geometry.Size(
                        secondRadius * 2f,
                        secondRadius * 2f
                    ),
                    style = Stroke(
                        width = 3f,
                        cap = StrokeCap.Round
                    )
                )

                /*
                 * =================================================
                 * PARTICLES
                 * =================================================
                 */

                val particleAngles = listOf(
                    15f,
                    48f,
                    82f,
                    118f,
                    154f,
                    190f,
                    225f,
                    260f,
                    296f,
                    330f
                )

                particleAngles.forEachIndexed { index, angle ->

                    val radians =
                        Math.toRadians(
                            (angle + rotation * 0.15f).toDouble()
                        )

                    val radius =
                        baseRadius * when (index % 3) {
                            0 -> 0.56f
                            1 -> 0.70f
                            else -> 0.88f
                        }

                    val px =
                        centerX + cos(radians).toFloat() * radius

                    val py =
                        centerY + sin(radians).toFloat() * radius

                    drawCircle(
                        color =
                            if (index % 2 == 0) {
                                NeonCyan.copy(alpha = 0.85f)
                            } else {
                                NeonPurple.copy(alpha = 0.90f)
                            },
                        radius = if (index % 3 == 0) 4f else 2.5f,
                        center = androidx.compose.ui.geometry.Offset(
                            px,
                            py
                        )
                    )
                }

                /*
                 * =================================================
                 * SMALL ORBITAL POINTS
                 * =================================================
                 */

                val smallAngles = listOf(
                    35f,
                    110f,
                    200f,
                    285f
                )

                smallAngles.forEach { angle ->

                    val radians =
                        Math.toRadians(
                            (angle + reverseRotation * 0.10f).toDouble()
                        )

                    val radius = baseRadius * 0.44f

                    val px =
                        centerX + cos(radians).toFloat() * radius

                    val py =
                        centerY + sin(radians).toFloat() * radius

                    drawCircle(
                        color = NeonBlue.copy(alpha = 0.90f),
                        radius = 5f,
                        center = androidx.compose.ui.geometry.Offset(
                            px,
                            py
                        )
                    )
                }
            }

            /*
             * =====================================================
             * CENTER VISION CORE
             * =====================================================
             */

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(205.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeonBlue.copy(alpha = 0.32f),
                                NeonPurple.copy(alpha = 0.25f),
                                Color(0xFF030714),
                                Color.Black
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {

                /*
                 * INNER GLOBE
                 */

                Canvas(
                    modifier = Modifier.size(145.dp)
                ) {

                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.minDimension * 0.43f

                    /*
                     * Globe glow
                     */

                    drawCircle(
                        color = NeonBlue.copy(
                            alpha = 0.18f * glow
                        ),
                        radius = r * 1.25f,
                        center = androidx.compose.ui.geometry.Offset(
                            cx,
                            cy
                        )
                    )

                    /*
                     * Globe outer ring
                     */

                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.90f),
                        radius = r,
                        center = androidx.compose.ui.geometry.Offset(
                            cx,
                            cy
                        ),
                        style = Stroke(width = 2.5f)
                    )

                    drawCircle(
                        color = NeonPurple.copy(alpha = 0.70f),
                        radius = r * 0.90f,
                        center = androidx.compose.ui.geometry.Offset(
                            cx,
                            cy
                        ),
                        style = Stroke(width = 1.5f)
                    )

                    /*
                     * Longitude lines
                     */

                    val globeTopLeft =
                        androidx.compose.ui.geometry.Offset(
                            cx - r,
                            cy - r
                        )

                    val globeSize =
                        androidx.compose.ui.geometry.Size(
                            r * 2f,
                            r * 2f
                        )

                    drawOval(
                        color = NeonBlue.copy(alpha = 0.55f),
                        topLeft = globeTopLeft,
                        size = globeSize,
                        style = Stroke(width = 1.2f)
                    )

                    drawOval(
                        color = NeonPurple.copy(alpha = 0.55f),
                        topLeft = androidx.compose.ui.geometry.Offset(
                            cx - r * 0.55f,
                            cy - r
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 1.10f,
                            r * 2f
                        ),
                        style = Stroke(width = 1.2f)
                    )

                    drawOval(
                        color = NeonCyan.copy(alpha = 0.42f),
                        topLeft = androidx.compose.ui.geometry.Offset(
                            cx - r * 0.30f,
                            cy - r
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 0.60f,
                            r * 2f
                        ),
                        style = Stroke(width = 1f)
                    )

                    /*
                     * Latitude lines
                     */

                    drawOval(
                        color = NeonPurple.copy(alpha = 0.48f),
                        topLeft = androidx.compose.ui.geometry.Offset(
                            cx - r,
                            cy - r * 0.58f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 2f,
                            r * 1.16f
                        ),
                        style = Stroke(width = 1f)
                    )

                    drawOval(
                        color = NeonCyan.copy(alpha = 0.48f),
                        topLeft = androidx.compose.ui.geometry.Offset(
                            cx - r,
                            cy - r * 0.28f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 2f,
                            r * 0.56f
                        ),
                        style = Stroke(width = 1f)
                    )

                    drawOval(
                        color = NeonPurple.copy(alpha = 0.45f),
                        topLeft = androidx.compose.ui.geometry.Offset(
                            cx - r,
                            cy + r * 0.20f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            r * 2f,
                            r * 0.56f
                        ),
                        style = Stroke(width = 1f)
                    )

                    /*
                     * Globe particles
                     */

                    for (i in 0 until 14) {

                        val angle =
                            Math.toRadians(
                                (i * 27 + rotation * 0.4f).toDouble()
                            )

                        val rr =
                            r * (0.45f + (i % 4) * 0.10f)

                        val px =
                            cx + cos(angle).toFloat() * rr

                        val py =
                            cy + sin(angle).toFloat() * rr

                        drawCircle(
                            color =
                                if (i % 2 == 0) {
                                    NeonCyan
                                } else {
                                    NeonPurple
                                },
                            radius = 1.8f,
                            center = androidx.compose.ui.geometry.Offset(
                                px,
                                py
                            )
                        )
                    }
                }

                /*
                 * BIG V
                 */

                Text(
                    text = "V",
                    color = BrightWhite,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .graphicsLayer {
                            shadowElevation = 30f
                        }
                )
            }

            /*
             * =====================================================
             * VISION CORE LABEL
             * =====================================================
             */

            Text(
                text = "VISION CORE",
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 122.dp),
                color = BrightWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            /*
             * =====================================================
             * CHAT — TOP
             * =====================================================
             */

            DashboardAction(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 10.dp),
                icon = Icons.Default.Chat,
                title = "CHAT",
                onClick = {
                    onNewChat()
                    onNavigate(VisionTab.CHAT)
                }
            )

            /*
             * =====================================================
             * SETTINGS — LEFT
             * =====================================================
             */

            DashboardAction(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 4.dp),
                icon = Icons.Default.Settings,
                title = "SETTINGS",
                onClick = {
                    onNavigate(VisionTab.SETTINGS)
                }
            )

            /*
             * =====================================================
             * VOICE — RIGHT
             * =====================================================
             */

            DashboardAction(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-4).dp),
                icon = Icons.Default.Mic,
                title = "VOICE",
                onClick = {
                    onNewChat()
                    onStartVoiceCall()
                }
            )

            /*
             * =====================================================
             * HISTORY — BOTTOM
             * =====================================================
             */

            DashboardAction(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-4).dp),
                icon = Icons.Default.History,
                title = "HISTORY",
                onClick = {
                    onShowHistory()
                }
            )
        }

        /*
         * =========================================================
         * FOOTER
         * =========================================================
         */

        Text(
            text = "T H I N K   •   A S K   •   E V O L V E",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            color = MutedBlue.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp
        )
    }
}


/*
 * =================================================================
 * DASHBOARD ACTION BUTTON
 * =================================================================
 */

@Composable
private fun DashboardAction(
    modifier: Modifier = Modifier,
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
                .size(82.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF253E9A),
                            Color(0xFF10134F),
                            Color(0xFF070718)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {

            /*
             * Neon ring
             */

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {

                drawCircle(
                    color = NeonBlue.copy(alpha = 0.85f),
                    radius = size.minDimension / 2f - 2f,
                    style = Stroke(width = 2.5f)
                )

                drawArc(
                    color = NeonPurple,
                    startAngle = 205f,
                    sweepAngle = 125f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(
                        2f,
                        2f
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - 4f,
                        size.height - 4f
                    ),
                    style = Stroke(
                        width = 3f,
                        cap = StrokeCap.Round
                    )
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = BrightWhite,
                modifier = Modifier.size(39.dp)
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = title,
            color = BrightWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
    }
}
