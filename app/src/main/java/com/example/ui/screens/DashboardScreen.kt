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
import kotlin.math.sin

private val NeonBlue = Color(0xFF00D9FF)
private val ElectricBlue = Color(0xFF168BFF)
private val NeonViolet = Color(0xFF7137FF)
private val NeonPurple = Color(0xFFB42CFF)
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

        /*
         * SYSTEM STATUS
         */
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
         * CHAT
         *
         * Orbit ke paas — screen ke bilkul top par nahi.
         */
        OrbitAction(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 238.dp),
            icon = Icons.Default.Chat,
            title = "CHAT",
            onClick = {
                onNewChat()
            }
        )

        /*
         * SETTINGS
         *
         * Left orbit.
         */
        OrbitAction(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(end = 232.dp),
            icon = Icons.Default.Settings,
            title = "SETTINGS",
            onClick = {
                onNavigate(VisionTab.SETTINGS)
            }
        )

        /*
         * VOICE
         *
         * Right orbit.
         */
        OrbitAction(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 232.dp),
            icon = Icons.Default.RecordVoiceOver,
            title = "VOICE",
            onClick = {
                onStartVoiceCall()
            }
        )

        /*
         * HISTORY
         *
         * Orbit ke neeche — bahut bottom par nahi.
         */
        OrbitAction(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 238.dp),
            icon = Icons.Default.History,
            title = "HISTORY",
            onClick = {
                onShowHistory()
            }
        )

        /*
         * MAIN VISION CORE
         */
        VisionEnergyCore(
            modifier = Modifier.align(Alignment.Center),
            onClick = {
                onNavigate(VisionTab.CHAT)
            }
        )

        /*
         * FOOTER
         */
        Text(
            text = "THINK  •  ASK  •  EVOLVE",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 7.dp),
            color = Color(0xFF4D5870),
            fontSize = 7.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun VisionEnergyCore(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(
        label = "vision_energy"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                1300,
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
                9000,
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
                7000,
                easing = LinearEasing
            )
        ),
        label = "reverseRotation"
    )

    Box(
        modifier = modifier
            .size(235.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {

        /*
         * LARGE BLUE/VIOLET ATMOSPHERIC GLOW
         */
        Box(
            modifier = Modifier
                .size(235.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonBlue.copy(alpha = 0.30f),
                            NeonViolet.copy(alpha = 0.20f),
                            NeonPurple.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        /*
         * OUTER HUD RING
         */
        Canvas(
            modifier = Modifier
                .size(225.dp)
                .graphicsLayer {
                    rotationZ = rotation
                }
        ) {
            val stroke = Stroke(
                width = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonBlue,
                        Color.Transparent,
                        NeonViolet,
                        Color.Transparent,
                        NeonBlue
                    )
                ),
                startAngle = 12f,
                sweepAngle = 285f,
                useCenter = false,
                style = stroke
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color.Transparent,
                        NeonPurple,
                        Color.Transparent,
                        NeonBlue
                    )
                ),
                startAngle = 205f,
                sweepAngle = 100f,
                useCenter = false,
                style = stroke
            )
        }

        /*
         * SECOND HUD RING
         */
        Canvas(
            modifier = Modifier
                .size(195.dp)
                .graphicsLayer {
                    rotationZ = reverseRotation
                }
        ) {
            val stroke = Stroke(
                width = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        NeonPurple,
                        Color.Transparent,
                        NeonBlue,
                        Color.Transparent
                    )
                ),
                startAngle = -20f,
                sweepAngle = 245f,
                useCenter = false,
                style = stroke
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
                startAngle = 190f,
                sweepAngle = 105f,
                useCenter = false,
                style = stroke
            )
        }

        /*
         * INNER ENERGY FIELD
         */
        Box(
            modifier = Modifier
                .size(177.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonBlue.copy(alpha = 0.35f),
                            ElectricBlue.copy(alpha = 0.20f),
                            NeonViolet.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )

        /*
         * CORE ORBIT
         */
        Canvas(
            modifier = Modifier
                .size(170.dp)
                .graphicsLayer {
                    rotationZ = rotation * 1.35f
                }
        ) {

            val stroke = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

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
                startAngle = 0f,
                sweepAngle = 315f,
                useCenter = false,
                style = stroke
            )
        }

        /*
         * ENERGY PARTICLES
         */
        EnergyParticles(
            modifier = Modifier.size(165.dp),
            rotation = rotation
        )

        /*
         * CENTRAL CORE
         */
        Box(
            modifier = Modifier
                .size(122.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFBDEFFF),
                            Color(0xFF37BFFF),
                            Color(0xFF4964FF),
                            Color(0xFF6D28D9),
                            Color(0xFF18072D)
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
             * Core logo
             *
             * Existing Vision logo use hoga.
             */
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(
                    id = com.example.R.drawable.ic_vision_logo
                ),
                contentDescription = "Vision Core",
                modifier = Modifier.size(76.dp)
            )
        }

        /*
         * CORE LABEL
         */
        Text(
            text = "VISION CORE",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 2.dp),
            color = Color.White,
            fontSize = 7.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
private fun EnergyParticles(
    modifier: Modifier = Modifier,
    rotation: Float
) {
    val particles = remember {
        listOf(
            Triple(0.12f, 0.18f, 2.2f),
            Triple(0.82f, 0.20f, 1.7f),
            Triple(0.90f, 0.62f, 2.0f),
            Triple(0.18f, 0.82f, 1.5f),
            Triple(0.38f, 0.08f, 1.4f),
            Triple(0.70f, 0.88f, 1.8f),
            Triple(0.08f, 0.52f, 1.5f),
            Triple(0.94f, 0.40f, 1.3f)
        )
    }

    Canvas(
        modifier = modifier
    ) {
        particles.forEachIndexed { index, particle ->

            val x = size.width * particle.first
            val y = size.height * particle.second

            val angle =
                Math.toRadians(
                    (rotation * (if (index % 2 == 0) 1 else -1)).toDouble()
                )

            val centerX = size.width / 2f
            val centerY = size.height / 2f

            val rotatedX =
                centerX +
                        (x - centerX) * cos(angle).toFloat() -
                        (y - centerY) * sin(angle).toFloat()

            val rotatedY =
                centerY +
                        (x - centerX) * sin(angle).toFloat() +
                        (y - centerY) * cos(angle).toFloat()

            drawCircle(
                color = if (index % 2 == 0) {
                    NeonBlue
                } else {
                    NeonPurple
                },
                radius = particle.third.dp.toPx(),
                center = Offset(
                    rotatedX,
                    rotatedY
                )
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
            .clip(CircleShape)
            .clickable {
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF253B8F),
                            Color(0xFF32145E),
                            Color(0xFF07040E)
                        )
                    )
                )
                .border(
                    width = 1.4.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            NeonBlue,
                            NeonViolet,
                            Color.Transparent,
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
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.size(4.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}
