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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import kotlin.math.sin

private val Black = Color(0xFF010207)
private val Blue = Color(0xFF00DFFF)
private val BrightBlue = Color(0xFF168BFF)
private val Violet = Color(0xFF633CFF)
private val Purple = Color(0xFFB82CFF)

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
            .background(Black)
    ) {

        // SYSTEM LIVE
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "●  SYSTEM LIVE",
                color = Blue,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.6.sp
            )

            Spacer(modifier = Modifier.size(2.dp))

            Text(
                text = activeEngine.displayName,
                color = Color(0xFF8994AA),
                fontSize = 8.sp,
                letterSpacing = 0.8.sp
            )
        }

        /*
         * RESPONSIVE HUD
         *
         * Fixed 400dp nahi.
         * Phone ki available width ke according scale hoga.
         */
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 35.dp, bottom = 30.dp),
            contentAlignment = Alignment.Center
        ) {

            val hudSize = minOf(
                maxWidth * 0.94f,
                410.dp
            )

            Box(
                modifier = Modifier
                    .size(hudSize)
                    .align(Alignment.Center)
                    .padding(bottom = 18.dp),
                contentAlignment = Alignment.Center
            ) {

                // =========================
                // HUD RINGS
                // =========================

                HudRings(
                    modifier = Modifier.fillMaxSize()
                )

                // =========================
                // CHAT
                // =========================

                HudButton(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = hudSize * 0.015f),
                    icon = Icons.Default.Chat,
                    title = "Chat",
                    onClick = {
                        onNewChat()
                    }
                )

                // =========================
                // SETTINGS
                // =========================

                HudButton(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = hudSize * 0.015f),
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    onClick = {
                        onNavigate(VisionTab.SETTINGS)
                    }
                )

                // =========================
                // VOICE
                // =========================

                HudButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = hudSize * 0.015f),
                    icon = Icons.Default.Mic,
                    title = "Voice",
                    onClick = {
                        onStartVoiceCall()
                    }
                )

                // =========================
                // HISTORY
                // =========================

                HudButton(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = hudSize * 0.015f),
                    icon = Icons.Default.History,
                    title = "History",
                    onClick = {
                        onShowHistory()
                    }
                )

                // =========================
                // VISION ENERGY CORE
                // =========================

                VisionEnergyCore(
                    modifier = Modifier.align(Alignment.Center),
                    onClick = {
                        onNavigate(VisionTab.CHAT)
                    }
                )
            }
        }

        // FOOTER
        Text(
            text = "THINK  •  ASK  •  EVOLVE",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            color = Color(0xFF53607A),
            fontSize = 7.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )
    }
}


/* =========================================================
   HUD RINGS
   ========================================================= */

@Composable
private fun HudRings(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(
        label = "hud"
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                10000,
                easing = LinearEasing
            )
        ),
        label = "rotation"
    )

    val reverseRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                7500,
                easing = LinearEasing
            )
        ),
        label = "reverse"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        // Outer glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Blue.copy(alpha = 0.12f),
                            Violet.copy(alpha = 0.10f),
                            Purple.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        // OUTER RING
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation
                }
        ) {
            val stroke = Stroke(
                width = 1.7.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Blue,
                        Violet,
                        Color.Transparent,
                        Purple,
                        Blue
                    )
                ),
                startAngle = 5f,
                sweepAngle = 305f,
                useCenter = false,
                style = stroke
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        Blue,
                        Color.Transparent
                    )
                ),
                startAngle = 225f,
                sweepAngle = 65f,
                useCenter = false,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // SECOND RING
        Canvas(
            modifier = Modifier
                .fillMaxSize(0.83f)
                .graphicsLayer {
                    rotationZ = reverseRotation
                }
        ) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Purple,
                        Blue,
                        Color.Transparent,
                        Violet
                    )
                ),
                startAngle = -20f,
                sweepAngle = 320f,
                useCenter = false,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // THIRD RING
        Canvas(
            modifier = Modifier
                .fillMaxSize(0.68f)
                .graphicsLayer {
                    rotationZ = rotation * 1.3f
                }
        ) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Blue,
                        Color.Transparent,
                        Purple,
                        Violet,
                        Blue
                    )
                ),
                startAngle = 15f,
                sweepAngle = 295f,
                useCenter = false,
                style = Stroke(
                    width = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // Inner glow
        Box(
            modifier = Modifier
                .fillMaxSize(0.61f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Blue.copy(alpha = 0.25f),
                            BrightBlue.copy(alpha = 0.16f),
                            Violet.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )

        EnergyParticles(
            modifier = Modifier.fillMaxSize(0.62f),
            rotation = rotation
        )
    }
}


/* =========================================================
   VISION ENERGY CORE
   ========================================================= */

@Composable
private fun VisionEnergyCore(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(
        label = "vision_core"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                1400,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                6500,
                easing = LinearEasing
            )
        ),
        label = "coreRotation"
    )

    Box(
        modifier = modifier
            .size(174.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {

        // Massive glow
        Box(
            modifier = Modifier
                .size(174.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Blue.copy(alpha = 0.38f),
                            BrightBlue.copy(alpha = 0.22f),
                            Violet.copy(alpha = 0.20f),
                            Purple.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Energy sphere
        Box(
            modifier = Modifier
                .size(146.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0D7DFF),
                            Color(0xFF0753D6),
                            Color(0xFF2024A0),
                            Color(0xFF401A91),
                            Color(0xFF08051A)
                        )
                    )
                )
        )

        // Orbital energy lines
        Canvas(
            modifier = Modifier
                .size(145.dp)
                .graphicsLayer {
                    rotationZ = rotation
                }
        ) {
            val oval = Rect(
                left = size.width * 0.06f,
                top = size.height * 0.25f,
                right = size.width * 0.94f,
                bottom = size.height * 0.75f
            )

            drawOval(
                brush = Brush.sweepGradient(
                    listOf(
                        Blue,
                        Color.Transparent,
                        Purple,
                        Violet,
                        Blue
                    )
                ),
                topLeft = oval.topLeft,
                size = oval.size,
                style = Stroke(
                    width = 1.6.dp.toPx()
                )
            )

            val secondOval = Rect(
                left = size.width * 0.22f,
                top = size.height * 0.05f,
                right = size.width * 0.78f,
                bottom = size.height * 0.95f
            )

            drawOval(
                brush = Brush.sweepGradient(
                    listOf(
                        Purple,
                        Color.Transparent,
                        Blue,
                        Color.Transparent
                    )
                ),
                topLeft = secondOval.topLeft,
                size = secondOval.size,
                style = Stroke(
                    width = 1.3.dp.toPx()
                )
            )
        }

        // Bright central light
        Box(
            modifier = Modifier
                .size(125.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF07152F),
                            Color(0xFF03102A),
                            Color.Transparent
                        )
                    )
                )
        )

        // Glowing V
        Canvas(
            modifier = Modifier.size(78.dp)
        ) {
            val path = Path().apply {
                moveTo(
                    size.width * 0.08f,
                    size.height * 0.18f
                )

                lineTo(
                    size.width * 0.50f,
                    size.height * 0.82f
                )

                lineTo(
                    size.width * 0.92f,
                    size.height * 0.18f
                )
            }

            // Outer glow
            drawPath(
                path = path,
                color = Blue.copy(alpha = 0.40f),
                style = Stroke(
                    width = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // Main V
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White,
                        Blue,
                        Color(0xFF5D7CFF),
                        Purple
                    )
                ),
                style = Stroke(
                    width = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // Tiny energy points
        Canvas(
            modifier = Modifier.size(155.dp)
        ) {
            drawCircle(
                color = Blue,
                radius = 2.dp.toPx(),
                center = Offset(
                    size.width * 0.18f,
                    size.height * 0.32f
                )
            )

            drawCircle(
                color = Purple,
                radius = 2.dp.toPx(),
                center = Offset(
                    size.width * 0.82f,
                    size.height * 0.65f
                )
            )

            drawCircle(
                color = Blue,
                radius = 1.7.dp.toPx(),
                center = Offset(
                    size.width * 0.74f,
                    size.height * 0.18f
                )
            )
        }
    }
}


/* =========================================================
   PARTICLES
   ========================================================= */

@Composable
private fun EnergyParticles(
    modifier: Modifier,
    rotation: Float
) {
    val points = listOf(
        0.10f to 0.32f,
        0.18f to 0.70f,
        0.34f to 0.10f,
        0.68f to 0.13f,
        0.86f to 0.39f,
        0.79f to 0.76f,
        0.52f to 0.91f,
        0.28f to 0.86f,
        0.92f to 0.58f
    )

    Canvas(
        modifier = modifier
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        points.forEachIndexed { index, point ->

            val px = size.width * point.first
            val py = size.height * point.second

            val angle = Math.toRadians(
                (rotation * if (index % 2 == 0) 1 else -1).toDouble()
            )

            val x =
                centerX +
                        (px - centerX) * cos(angle).toFloat() -
                        (py - centerY) * sin(angle).toFloat()

            val y =
                centerY +
                        (px - centerX) * sin(angle).toFloat() +
                        (py - centerY) * cos(angle).toFloat()

            drawCircle(
                color = if (index % 2 == 0) Blue else Purple,
                radius = if (index % 3 == 0) {
                    2.dp.toPx()
                } else {
                    1.3.dp.toPx()
                },
                center = Offset(x, y)
            )
        }
    }
}


/* =========================================================
   ACTION BUTTON
   ========================================================= */

@Composable
private fun HudButton(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable {
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2858C9),
                            Color(0xFF27217A),
                            Color(0xFF09051C)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Blue,
                            Violet,
                            Purple,
                            Color.Transparent,
                            Blue
                        )
                    ),
                    radius = size.minDimension / 2f - 1.dp.toPx(),
                    center = center,
                    style = Stroke(
                        width = 1.7.dp.toPx()
                    )
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.size(5.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.1.sp
        )
    }
}
