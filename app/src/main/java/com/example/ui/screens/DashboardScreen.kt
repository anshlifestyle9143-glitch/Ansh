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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import kotlin.math.cos
import kotlin.math.sin

private val NeonBlue = Color(0xFF00DFFF)
private val ElectricBlue = Color(0xFF167BFF)
private val NeonViolet = Color(0xFF633CFF)
private val NeonPurple = Color(0xFFB72CFF)
private val DeepBlack = Color(0xFF010207)

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
            .background(DeepBlack)
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
                color = NeonBlue,
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
         * MAIN HUD
         *
         * Screen ke center se thoda upar,
         * taaki History neeche cut na ho.
         */
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 35.dp)
                .size(400.dp),
            contentAlignment = Alignment.Center
        ) {

            VisionHudRings()

            // CHAT — top of orbit
            OrbitAction(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 0.dp),
                icon = Icons.Default.Chat,
                title = "CHAT",
                onClick = {
                    onNewChat()
                }
            )

            // SETTINGS — left
            OrbitAction(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 0.dp),
                icon = Icons.Default.Settings,
                title = "SETTINGS",
                onClick = {
                    onNavigate(VisionTab.SETTINGS)
                }
            )

            // VOICE — right
            OrbitAction(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 0.dp),
                icon = Icons.Default.RecordVoiceOver,
                title = "VOICE",
                onClick = {
                    onStartVoiceCall()
                }
            )

            // HISTORY — bottom
            OrbitAction(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 0.dp),
                icon = Icons.Default.History,
                title = "HISTORY",
                onClick = {
                    onShowHistory()
                }
            )

            // VISION CORE
            VisionCore(
                modifier = Modifier.align(Alignment.Center),
                onClick = {
                    onNavigate(VisionTab.CHAT)
                }
            )
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

@Composable
private fun VisionHudRings() {

    val transition = rememberInfiniteTransition(
        label = "hud_rotation"
    )

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 9000,
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
                durationMillis = 7000,
                easing = LinearEasing
            )
        ),
        label = "reverse"
    )

    Box(
        modifier = Modifier.size(400.dp),
        contentAlignment = Alignment.Center
    ) {

        // Wide atmospheric glow
        Box(
            modifier = Modifier
                .size(380.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonBlue.copy(alpha = 0.10f),
                            NeonViolet.copy(alpha = 0.13f),
                            NeonPurple.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Outer ring
        Canvas(
            modifier = Modifier
                .size(382.dp)
                .graphicsLayer {
                    rotationZ = rotation
                }
        ) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonBlue,
                        NeonViolet,
                        Color.Transparent,
                        NeonPurple,
                        NeonBlue
                    )
                ),
                startAngle = 5f,
                sweepAngle = 305f,
                useCenter = false,
                style = Stroke(
                    width = 1.7.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        NeonBlue,
                        Color.Transparent
                    )
                ),
                startAngle = 220f,
                sweepAngle = 70f,
                useCenter = false,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // Middle ring
        Canvas(
            modifier = Modifier
                .size(330.dp)
                .graphicsLayer {
                    rotationZ = reverseRotation
                }
        ) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonViolet,
                        NeonBlue,
                        Color.Transparent,
                        NeonPurple
                    )
                ),
                startAngle = -15f,
                sweepAngle = 325f,
                useCenter = false,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // Inner ring
        Canvas(
            modifier = Modifier
                .size(270.dp)
                .graphicsLayer {
                    rotationZ = rotation * 1.35f
                }
        ) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonBlue,
                        Color.Transparent,
                        NeonPurple,
                        NeonViolet,
                        NeonBlue
                    )
                ),
                startAngle = 20f,
                sweepAngle = 290f,
                useCenter = false,
                style = Stroke(
                    width = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }

        // Inner energy glow
        Box(
            modifier = Modifier
                .size(245.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonBlue.copy(alpha = 0.25f),
                            NeonViolet.copy(alpha = 0.25f),
                            NeonPurple.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        EnergyDots(
            modifier = Modifier.size(245.dp),
            rotation = rotation
        )
    }
}

@Composable
private fun VisionCore(
    modifier: Modifier,
    onClick: () -> Unit
) {

    val transition = rememberInfiniteTransition(
        label = "core_pulse"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1300,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .size(185.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {

        // Core outer glow
        Box(
            modifier = Modifier
                .size(185.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonBlue.copy(alpha = 0.40f),
                            ElectricBlue.copy(alpha = 0.25f),
                            NeonViolet.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Core sphere
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0C6FFF),
                            Color(0xFF173BCE),
                            Color(0xFF3920A4),
                            Color(0xFF13072F)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            NeonBlue,
                            NeonPurple,
                            NeonViolet,
                            NeonBlue
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {

            /*
             * Existing Vision logo ko circular crop
             * kiya gaya hai taaki square box kam dikhe.
             */
            Image(
                painter = painterResource(
                    id = R.drawable.ic_vision_logo
                ),
                contentDescription = "Vision Core",
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Text(
            text = "VISION CORE",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 7.dp),
            color = Color.White,
            fontSize = 7.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun EnergyDots(
    modifier: Modifier,
    rotation: Float
) {
    val points = remember {
        listOf(
            0.10f to 0.31f,
            0.18f to 0.72f,
            0.35f to 0.12f,
            0.67f to 0.16f,
            0.84f to 0.39f,
            0.79f to 0.75f,
            0.51f to 0.91f,
            0.28f to 0.87f,
            0.92f to 0.58f
        )
    }

    Canvas(
        modifier = modifier
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f

        points.forEachIndexed { index, point ->

            val px = size.width * point.first
            val py = size.height * point.second

            val angle = Math.toRadians(
                (rotation * if (index % 2 == 0) 1 else -1).toDouble()
            )

            val x =
                cx +
                        (px - cx) * cos(angle).toFloat() -
                        (py - cy) * sin(angle).toFloat()

            val y =
                cy +
                        (px - cx) * sin(angle).toFloat() +
                        (py - cy) * cos(angle).toFloat()

            drawCircle(
                color = if (index % 2 == 0) {
                    NeonBlue
                } else {
                    NeonPurple
                },
                radius = if (index % 3 == 0) {
                    2.2.dp.toPx()
                } else {
                    1.5.dp.toPx()
                },
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun OrbitAction(
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
                            Color(0xFF2857C7),
                            Color(0xFF24206F),
                            Color(0xFF09051A)
                        )
                    )
                )
                .border(
                    width = 1.6.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            NeonBlue,
                            NeonViolet,
                            NeonPurple,
                            Color.Transparent,
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
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.size(6.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}
