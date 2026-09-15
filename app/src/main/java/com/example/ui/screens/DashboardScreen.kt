package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.VisionViewModel
import com.example.ui.components.VisionTab
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DashboardScreen(
    viewModel: VisionViewModel,
    onNavigate: (VisionTab) -> Unit,
    onStartVoiceCall: () -> Unit,
    onNewChat: () -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "vision_animation")

    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 18000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulse by infinite.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glow by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        /* ---------------- BACKGROUND ---------------- */

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.30f)
        ) {
            drawBackgroundGrid()
        }

        /* ---------------- HEADER ---------------- */

        DashboardHeader(
            onCoreClick = {
                onNavigate(VisionTab.CREATOR)
            }
        )

        /* ---------------- MAIN CONTENT ---------------- */

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = 92.dp,
                    bottom = 72.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF36FF7A))
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "SYSTEM LIVE",
                    color = Color(0xFF56FF8A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "Vision Core",
                color = Color(0xFF6F7D91),
                fontSize = 10.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.weight(0.12f))

            /* ---------------- ORBITAL CORE ---------------- */

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {

                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    drawOrbitalSystem(
                        rotation = rotation,
                        pulse = pulse,
                        glow = glow
                    )
                }

                /* CENTER VISION CORE */

                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF0E6FFF),
                                    Color(0xFF063F9C),
                                    Color(0xFF020A18),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.dp,
                                color = Color(0xFF168BFF).copy(alpha = 0.75f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "V",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-3).sp
                        )
                    }
                }

                /* CHAT */

                OrbitalButton(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-4).dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Chat",
                            tint = Color(0xFF58AFFF),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = "CHAT",
                    onClick = onNewChat
                )

                /* SETTINGS */

                OrbitalButton(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = 5.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF58AFFF),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = "SETTINGS",
                    onClick = {
                        onNavigate(VisionTab.SETTINGS)
                    }
                )

                /* VOICE / TEXT */

                OrbitalButton(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-5).dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice",
                            tint = Color(0xFF58AFFF),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = "VOICE / TEXT",
                    onClick = onStartVoiceCall
                )

                /* HISTORY */

                OrbitalButton(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 4.dp),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = Color(0xFF58AFFF),
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = "HISTORY",
                    onClick = onShowHistory
                )
            }

            Spacer(modifier = Modifier.weight(0.06f))

            /* ---------------- MOUNTAIN / WATER ---------------- */

            MountainFooter(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            /* ---------------- FOOTER ---------------- */

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Column {
                    Text(
                        text = "VISION",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )

                    Text(
                        text = "BY ANSH YADAV",
                        color = Color(0xFF64748B),
                        fontSize = 7.sp,
                        letterSpacing = 1.5.sp
                    )
                }

                Text(
                    text = "THINK  •  ASK  •  EVOLVE",
                    color = Color(0xFF64748B),
                    fontSize = 7.sp,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}


/* ============================================================
   HEADER
   ============================================================ */

@Composable
private fun DashboardHeader(
    onCoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 18.dp,
                end = 18.dp,
                top = 16.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0D5BFF),
                            Color(0xFF031A48),
                            Color(0xFF01040A)
                        )
                    )
                )
                .border(
                    1.dp,
                    Color(0xFF168BFF).copy(alpha = 0.75f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "V",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "VISION",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Text(
                text = "AI ASSISTANT",
                color = Color(0xFF6B778A),
                fontSize = 7.sp,
                letterSpacing = 1.5.sp
            )

            Text(
                text = "BY ANSH YADAV",
                color = Color(0xFF4C5A6C),
                fontSize = 6.sp,
                letterSpacing = 1.2.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(
                    width = 1.dp,
                    color = Color(0xFF1D76FF).copy(alpha = 0.65f),
                    shape = RoundedCornerShape(50)
                )
                .clickable {
                    onCoreClick()
                }
                .padding(
                    horizontal = 13.dp,
                    vertical = 7.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3295FF))
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Vision Core",
                    color = Color(0xFF82B9FF),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}


/* ============================================================
   ORBITAL BUTTON
   ============================================================ */

@Composable
private fun OrbitalButton(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF071A35),
                            Color(0xFF020811)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color(0xFF167EFF).copy(alpha = 0.7f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = label,
            color = Color(0xFF7391B6),
            fontSize = 7.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
    }
}


/* ============================================================
   ORBITAL SYSTEM
   ============================================================ */

private fun DrawScope.drawOrbitalSystem(
    rotation: Float,
    pulse: Float,
    glow: Float
) {
    val center = Offset(
        x = size.width / 2f,
        y = size.height / 2f
    )

    val baseRadius = minOf(size.width, size.height) * 0.285f

    /* Outer blue glow */

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF006EFF).copy(alpha = 0.22f * glow),
                Color(0xFF004CFF).copy(alpha = 0.08f),
                Color.Transparent
            )
        ),
        radius = baseRadius * 1.48f,
        center = center
    )

    /* Outer orbital ring */

    drawCircle(
        color = Color(0xFF0A5DFF).copy(alpha = 0.35f),
        radius = baseRadius * 1.45f,
        center = center,
        style = Stroke(
            width = 1.2.dp.toPx()
        )
    )

    /* Middle ring */

    drawCircle(
        color = Color(0xFF168BFF).copy(alpha = 0.52f),
        radius = baseRadius * 1.14f,
        center = center,
        style = Stroke(
            width = 1.1.dp.toPx()
        )
    )

    /* Inner ring */

    drawCircle(
        color = Color(0xFF1695FF).copy(alpha = 0.72f),
        radius = baseRadius * 0.84f,
        center = center,
        style = Stroke(
            width = 1.dp.toPx()
        )
    )

    /* Rotating ellipse 1 */

    val ellipseRect = Rect(
        center.x - baseRadius * 1.38f,
        center.y - baseRadius * 0.46f,
        center.x + baseRadius * 1.38f,
        center.y + baseRadius * 0.46f
    )

    drawOval(
        color = Color(0xFF238FFF).copy(alpha = 0.52f),
        topLeft = ellipseRect.topLeft,
        size = ellipseRect.size,
        style = Stroke(width = 1.dp.toPx())
    )

    /* Rotating ellipse 2 */

    val ellipseRect2 = Rect(
        center.x - baseRadius * 0.55f,
        center.y - baseRadius * 1.38f,
        center.x + baseRadius * 0.55f,
        center.y + baseRadius * 1.38f
    )

    drawOval(
        color = Color(0xFF168BFF).copy(alpha = 0.42f),
        topLeft = ellipseRect2.topLeft,
        size = ellipseRect2.size,
        style = Stroke(width = 1.dp.toPx())
    )

    /* Globe latitude lines */

    for (i in -2..2) {
        val yOffset = i * baseRadius * 0.22f
        val widthFactor =
            cos((i * 18f) * Math.PI / 180f).toFloat().coerceAtLeast(0.35f)

        drawOval(
            color = Color(0xFF38A4FF).copy(alpha = 0.23f),
            topLeft = Offset(
                center.x - baseRadius * widthFactor,
                center.y + yOffset - baseRadius * 0.07f
            ),
            size = Size(
                baseRadius * 2f * widthFactor,
                baseRadius * 0.14f
            ),
            style = Stroke(width = 0.7.dp.toPx())
        )
    }

    /* Globe longitude lines */

    for (i in -2..2) {
        val xOffset = i * baseRadius * 0.23f

        drawOval(
            color = Color(0xFF38A4FF).copy(alpha = 0.19f),
            topLeft = Offset(
                center.x + xOffset - baseRadius * 0.12f,
                center.y - baseRadius
            ),
            size = Size(
                baseRadius * 0.24f,
                baseRadius * 2f
            ),
            style = Stroke(width = 0.7.dp.toPx())
        )
    }

    /* Rotating orbital highlight */

    val angle = Math.toRadians(rotation.toDouble())

    val dotX = center.x + cos(angle).toFloat() * baseRadius * 1.45f
    val dotY = center.y + sin(angle).toFloat() * baseRadius * 0.46f

    drawCircle(
        color = Color(0xFF63B8FF).copy(alpha = 0.9f),
        radius = 3.dp.toPx(),
        center = Offset(dotX, dotY)
    )

    /* Small orbital dots */

    val dots = listOf(
        0f,
        72f,
        144f,
        216f,
        288f
    )

    dots.forEach { degrees ->
        val a = Math.toRadians((degrees + rotation * 0.15f).toDouble())

        val x =
            center.x + cos(a).toFloat() * baseRadius * 1.14f

        val y =
            center.y + sin(a).toFloat() * baseRadius * 1.14f

        drawCircle(
            color = Color(0xFF3EA6FF).copy(alpha = 0.7f),
            radius = 1.7.dp.toPx(),
            center = Offset(x, y)
        )
    }

    /* Center energy ring */

    drawCircle(
        color = Color(0xFF1597FF).copy(alpha = 0.35f * pulse),
        radius = baseRadius * 0.54f * pulse,
        center = center,
        style = Stroke(
            width = 1.dp.toPx()
        )
    )
}


/* ============================================================
   BACKGROUND GRID
   ============================================================ */

private fun DrawScope.drawBackgroundGrid() {
    val spacing = 42.dp.toPx()

    var x = 0f

    while (x <= size.width) {
        drawLine(
            color = Color(0xFF0C3B70).copy(alpha = 0.12f),
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 0.5.dp.toPx()
        )
        x += spacing
    }

    var y = 0f

    while (y <= size.height) {
        drawLine(
            color = Color(0xFF0C3B70).copy(alpha = 0.12f),
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 0.5.dp.toPx()
        )
        y += spacing
    }
}


/* ============================================================
   MOUNTAIN FOOTER
   ============================================================ */

@Composable
private fun MountainFooter(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
    ) {

        val horizon = size.height * 0.42f

        /* Water glow */

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF06172B).copy(alpha = 0.75f),
                    Color(0xFF01050B),
                    Color.Black
                ),
                startY = horizon,
                endY = size.height
            )
        )

        /* Mountain silhouette */

        val mountain = Path().apply {
            moveTo(0f, horizon + 24.dp.toPx())

            lineTo(
                size.width * 0.10f,
                horizon + 2.dp.toPx()
            )

            lineTo(
                size.width * 0.18f,
                horizon + 15.dp.toPx()
            )

            lineTo(
                size.width * 0.28f,
                horizon - 17.dp.toPx()
            )

            lineTo(
                size.width * 0.37f,
                horizon + 7.dp.toPx()
            )

            lineTo(
                size.width * 0.46f,
                horizon - 28.dp.toPx()
            )

            lineTo(
                size.width * 0.53f,
                horizon - 3.dp.toPx()
            )

            lineTo(
                size.width * 0.61f,
                horizon - 21.dp.toPx()
            )

            lineTo(
                size.width * 0.69f,
                horizon + 4.dp.toPx()
            )

            lineTo(
                size.width * 0.79f,
                horizon - 24.dp.toPx()
            )

            lineTo(
                size.width * 0.88f,
                horizon + 8.dp.toPx()
            )

            lineTo(
                size.width,
                horizon - 2.dp.toPx()
            )

            lineTo(
                size.width,
                size.height
            )

            lineTo(
                0f,
                size.height
            )

            close()
        }

        drawPath(
            path = mountain,
            color = Color(0xFF02070D)
        )

        /* Blue horizon line */

        drawLine(
            color = Color(0xFF0E70C9).copy(alpha = 0.35f),
            start = Offset(0f, horizon),
            end = Offset(size.width, horizon),
            strokeWidth = 1.dp.toPx()
        )

        /* Water reflection */

        for (i in 0..9) {
            val y = horizon + 11.dp.toPx() + i * 5.dp.toPx()

            drawLine(
                color = Color(0xFF1268A9).copy(
                    alpha = 0.16f - i * 0.012f
                ),
                start = Offset(size.width * 0.20f, y),
                end = Offset(size.width * 0.80f, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}


/* ============================================================
   VISION TAB
   ============================================================ */
