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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.VisionTab
import com.example.ui.viewmodel.VisionViewModel
import kotlin.math.cos
import kotlin.math.sin

private val VisionBlack = Color(0xFF000207)
private val NeonBlue = Color(0xFF00D9FF)
private val ElectricBlue = Color(0xFF347CFF)
private val NeonPurple = Color(0xFF9B18FF)
private val DeepPurple = Color(0xFF3C0B78)
private val CoreBlue = Color(0xFF126BFF)
private val White = Color(0xFFF8FAFF)

@Composable
fun DashboardScreen(
    viewModel: VisionViewModel,
    onNavigate: (VisionTab) -> Unit,
    onStartVoiceCall: () -> Unit,
    onNewChat: () -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeEngine by viewModel.activeEngine.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VisionBlack)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(28.dp))

            SystemLiveBadge()

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Vision Core",
                color = Color(0xFF7785A8),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = activeEngine.displayName,
                color = Color.Transparent,
                fontSize = 1.sp
            )

            VisionRadialInterface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weightSafe(),
                onSettings = {
                    onNavigate(VisionTab.SETTINGS)
                },
                onChat = {
                    onNavigate(VisionTab.CHAT)
                },
                onVoice = {
                    onStartVoiceCall()
                },
                onHistory = {
                    onShowHistory()
                },
                onCoreClick = {
                    onNavigate(VisionTab.CHAT)
                }
            )

            FooterText()
        }
    }
}

/*
 * Custom modifier instead of weight().
 * This deliberately avoids Row/Column weight conflicts.
 */
private fun Modifier.weightSafe(): Modifier {
    return this
        .height(610.dp)
}

/* --------------------------------------------------------- */
/* SYSTEM LIVE                                               */
/* --------------------------------------------------------- */

@Composable
private fun SystemLiveBadge() {
    val transition = rememberInfiniteTransition(label = "system_live")

    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_pulse"
    )

    Box(
        modifier = Modifier
            .height(62.dp)
            .padding(horizontal = 18.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF00151E),
                        Color(0xFF002331),
                        Color(0xFF001018)
                    )
                )
            )
            .border(
                1.5.dp,
                NeonBlue.copy(alpha = 0.65f),
                RoundedCornerShape(32.dp)
            )
            .padding(horizontal = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        NeonBlue.copy(alpha = 0.45f + pulse * 0.55f)
                    )
            )

            Spacer(modifier = Modifier.size(12.dp))

            Text(
                text = "SYSTEM LIVE",
                color = NeonBlue,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

/* --------------------------------------------------------- */
/* MAIN RADIAL INTERFACE                                     */
/* --------------------------------------------------------- */

@Composable
private fun VisionRadialInterface(
    modifier: Modifier,
    onSettings: () -> Unit,
    onChat: () -> Unit,
    onVoice: () -> Unit,
    onHistory: () -> Unit,
    onCoreClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "vision_radial")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 16000,
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
        label = "inner_rotation"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        val centerX = maxWidth / 2

        /*
         * The whole HUD.
         */
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(610.dp),
            contentAlignment = Alignment.Center
        ) {

            /*
             * OUTER ORBITAL SYSTEM
             */
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(610.dp)
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f - 4.dp.toPx()

                val outerRadius =
                    minOf(size.width * 0.48f, 300.dp.toPx())

                val middleRadius =
                    minOf(size.width * 0.40f, 245.dp.toPx())

                val innerRadius =
                    minOf(size.width * 0.31f, 190.dp.toPx())

                /*
                 * Soft atmospheric glow
                 */
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonPurple.copy(alpha = 0.28f),
                            CoreBlue.copy(alpha = 0.13f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = outerRadius
                    ),
                    radius = outerRadius,
                    center = Offset(cx, cy)
                )

                /*
                 * OUTER RING
                 */
                rotate(
                    degrees = rotation,
                    pivot = Offset(cx, cy)
                ) {
                    drawArc(
                        color = NeonBlue.copy(alpha = 0.95f),
                        startAngle = 12f,
                        sweepAngle = 88f,
                        useCenter = false,
                        topLeft = Rect(
                            cx - outerRadius,
                            cy - outerRadius,
                            cx + outerRadius,
                            cy + outerRadius
                        ),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    drawArc(
                        color = NeonPurple.copy(alpha = 0.90f),
                        startAngle = 145f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = Rect(
                            cx - outerRadius,
                            cy - outerRadius,
                            cx + outerRadius,
                            cy + outerRadius
                        ),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    drawArc(
                        color = NeonBlue.copy(alpha = 0.75f),
                        startAngle = 285f,
                        sweepAngle = 55f,
                        useCenter = false,
                        topLeft = Rect(
                            cx - outerRadius,
                            cy - outerRadius,
                            cx + outerRadius,
                            cy + outerRadius
                        ),
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                /*
                 * SECOND RING
                 */
                rotate(
                    degrees = reverseRotation,
                    pivot = Offset(cx, cy)
                ) {
                    drawArc(
                        color = NeonPurple.copy(alpha = 0.95f),
                        startAngle = 20f,
                        sweepAngle = 125f,
                        useCenter = false,
                        topLeft = Rect(
                            cx - middleRadius,
                            cy - middleRadius,
                            cx + middleRadius,
                            cy + middleRadius
                        ),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    drawArc(
                        color = NeonBlue.copy(alpha = 0.92f),
                        startAngle = 205f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = Rect(
                            cx - middleRadius,
                            cy - middleRadius,
                            cx + middleRadius,
                            cy + middleRadius
                        ),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }

                /*
                 * INNER RING
                 */
                drawArc(
                    color = NeonBlue.copy(alpha = 0.9f),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Rect(
                        cx - innerRadius,
                        cy - innerRadius,
                        cx + innerRadius,
                        cy + innerRadius
                    ),
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                drawArc(
                    color = NeonPurple.copy(alpha = 0.9f),
                    startAngle = 20f,
                    sweepAngle = 125f,
                    useCenter = false,
                    topLeft = Rect(
                        cx - innerRadius,
                        cy - innerRadius,
                        cx + innerRadius,
                        cy + innerRadius
                    ),
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                /*
                 * ORBITING PARTICLES
                 */
                val particleAngles = listOf(
                    15f,
                    48f,
                    82f,
                    128f,
                    172f,
                    215f,
                    260f,
                    310f,
                    345f
                )

                particleAngles.forEachIndexed { index, angle ->

                    val radius = when {
                        index % 3 == 0 -> middleRadius
                        index % 3 == 1 -> innerRadius + 25.dp.toPx()
                        else -> outerRadius - 30.dp.toPx()
                    }

                    val radians =
                        Math.toRadians(angle.toDouble())

                    val x =
                        cx + cos(radians).toFloat() * radius

                    val y =
                        cy + sin(radians).toFloat() * radius

                    drawCircle(
                        color = if (index % 2 == 0) {
                            NeonBlue
                        } else {
                            NeonPurple
                        },
                        radius = if (index % 3 == 0) {
                            4.dp.toPx()
                        } else {
                            2.5.dp.toPx()
                        },
                        center = Offset(x, y)
                    )

                    if (index % 3 == 0) {
                        drawCircle(
                            color = if (index % 2 == 0) {
                                NeonBlue.copy(alpha = 0.16f)
                            } else {
                                NeonPurple.copy(alpha = 0.16f)
                            },
                            radius = 11.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }

            /*
             * CHAT — TOP
             */
            RadialAction(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 45.dp),
                icon = Icons.Default.Chat,
                title = "CHAT",
                onClick = onChat
            )

            /*
             * SETTINGS — LEFT
             */
            RadialAction(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 2.dp, y = 18.dp),
                icon = Icons.Default.Settings,
                title = "SETTINGS",
                onClick = onSettings
            )

            /*
             * VOICE — RIGHT
             */
            RadialAction(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-2).dp, y = 18.dp),
                icon = Icons.Default.Mic,
                title = "VOICE",
                onClick = onVoice
            )

            /*
             * HISTORY — BOTTOM
             */
            RadialAction(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-25).dp),
                icon = Icons.Default.History,
                title = "HISTORY",
                onClick = onHistory
            )

            /*
             * VISION CORE
             */
            VisionCore(
                modifier = Modifier
                    .size(205.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .clickable {
                        onCoreClick()
                    }
            )
        }
    }
}

/* --------------------------------------------------------- */
/* RADIAL ACTION BUTTON                                     */
/* --------------------------------------------------------- */

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
                .size(86.dp)
                .shadow(
                    elevation = 22.dp,
                    shape = CircleShape,
                    ambientColor = NeonPurple,
                    spotColor = NeonBlue
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF263A9D),
                            Color(0xFF10164C),
                            Color(0xFF090B24)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            NeonBlue,
                            NeonPurple,
                            NeonBlue
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = White,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            color = White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp
        )
    }
}

/* --------------------------------------------------------- */
/* VISION CORE                                              */
/* --------------------------------------------------------- */

@Composable
private fun VisionCore(
    modifier: Modifier
) {
    val transition = rememberInfiniteTransition(label = "core_orbit")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 9000,
                easing = LinearEasing
            )
        ),
        label = "core_rotation"
    )

    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1700,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_glow"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        /*
         * Large glow
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonPurple.copy(alpha = glow),
                            CoreBlue.copy(alpha = glow * 0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        /*
         * Orbit sphere canvas
         */
        Canvas(
            modifier = Modifier
                .size(190.dp)
                .graphicsLayer {
                    rotationZ = rotation
                }
        ) {

            val cx = size.width / 2f
            val cy = size.height / 2f

            /*
             * Multiple orbital ellipses
             */
            rotate(
                degrees = 18f,
                pivot = Offset(cx, cy)
            ) {
                drawOval(
                    color = NeonBlue.copy(alpha = 0.9f),
                    topLeft = Offset(
                        cx - 78.dp.toPx(),
                        cy - 35.dp.toPx()
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        156.dp.toPx(),
                        70.dp.toPx()
                    ),
                    style = Stroke(
                        width = 2.dp.toPx()
                    )
                )
            }

            rotate(
                degrees = -42f,
                pivot = Offset(cx, cy)
            ) {
                drawOval(
                    color = NeonPurple.copy(alpha = 0.95f),
                    topLeft = Offset(
                        cx - 80.dp.toPx(),
                        cy - 34.dp.toPx()
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        160.dp.toPx(),
                        68.dp.toPx()
                    ),
                    style = Stroke(
                        width = 2.dp.toPx()
                    )
                )
            }

            rotate(
                degrees = 90f,
                pivot = Offset(cx, cy)
            ) {
                drawOval(
                    color = NeonBlue.copy(alpha = 0.7f),
                    topLeft = Offset(
                        cx - 75.dp.toPx(),
                        cy - 32.dp.toPx()
                    ),
                    size = androidx.compose.ui.geometry.Size(
                        150.dp.toPx(),
                        64.dp.toPx()
                    ),
                    style = Stroke(
                        width = 1.5.dp.toPx()
                    )
                )
            }

            /*
             * Sphere points
             */
            for (i in 0 until 34) {

                val angle = i * 10.8f

                val radians =
                    Math.toRadians(angle.toDouble())

                val radius =
                    45.dp.toPx() +
                            ((i % 5) * 3).dp.toPx()

                val x =
                    cx + cos(radians).toFloat() * radius

                val y =
                    cy + sin(radians).toFloat() * radius

                drawCircle(
                    color = if (i % 2 == 0) {
                        NeonBlue.copy(alpha = 0.9f)
                    } else {
                        NeonPurple.copy(alpha = 0.8f)
                    },
                    radius = 1.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        /*
         * Inner dark sphere
         */
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF071F5C),
                            Color(0xFF020817),
                            Color(0xFF000208)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            NeonBlue,
                            NeonPurple,
                            NeonBlue
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {

            GlowingV()
        }
    }
}

/* --------------------------------------------------------- */
/* GLOWING V                                                */
/* --------------------------------------------------------- */

@Composable
private fun GlowingV() {

    Canvas(
        modifier = Modifier.size(88.dp)
    ) {

        val path = Path().apply {

            moveTo(
                15.dp.toPx(),
                18.dp.toPx()
            )

            lineTo(
                43.dp.toPx(),
                68.dp.toPx()
            )

            lineTo(
                72.dp.toPx(),
                18.dp.toPx()
            )
        }

        /*
         * Outer glow
         */
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    NeonBlue,
                    Color(0xFF7DEBFF),
                    NeonPurple
                )
            ),
            style = Stroke(
                width = 10.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        /*
         * Bright inner V
         */
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFB9F8FF),
                    NeonBlue,
                    Color(0xFF8B5CFF)
                )
            ),
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        /*
         * Bright core point
         */
        drawCircle(
            color = White,
            radius = 3.dp.toPx(),
            center = Offset(
                43.dp.toPx(),
                68.dp.toPx()
            )
        )
    }
}

/* --------------------------------------------------------- */
/* FOOTER                                                    */
/* --------------------------------------------------------- */

@Composable
private fun FooterText() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 30.dp,
                end = 30.dp,
                bottom = 22.dp
            )
    ) {

        Text(
            text = "V I S I O N     •     T H I N K  •  A S K  •  E V O L V E",
            color = Color(0xFF58617A),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
