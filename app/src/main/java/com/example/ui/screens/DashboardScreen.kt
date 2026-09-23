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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.VisionTab
import com.example.ui.viewmodel.VisionViewModel

// ================================================================
// COLORS
// ================================================================

private val VisionBlack = Color(0xFF02050D)
private val NeonBlue = Color(0xFF00D2FF)
private val NeonCyan = Color(0xFF00E7FF)
private val ElectricBlue = Color(0xFF0066FF)
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

    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    val glow by infinite.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1800,
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

        // ========================================================
        // TOP HEADER
        // ========================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, top = 2.dp)
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
                    .clip(
                        RoundedCornerShape(20.dp)
                    )
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
        // SYSTEM LIVE STATUS
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
        // CENTRAL HUB & ORBITAL BUTTONS
        // ========================================================

        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {

            // ====================================================
            // BACKGROUND NEON CIRCLES
            // ====================================================

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {

                val cx = size.width / 2f
                val cy = size.height / 2f

                // Outer circle

                drawCircle(
                    color = ElectricBlue.copy(
                        alpha = 0.2f
                    ),
                    radius = 150.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(
                        cx,
                        cy
                    ),
                    style = Stroke(
                        width = 1.dp.toPx()
                    )
                )

                // Inner circle

                drawCircle(
                    color = ElectricBlue.copy(
                        alpha = 0.3f
                    ),
                    radius = 115.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(
                        cx,
                        cy
                    ),
                    style = Stroke(
                        width = 1.5.dp.toPx()
                    )
                )
            }


            // ====================================================
            // CENTRAL CORE
            // ====================================================

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
                    .drawBehind {

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    NeonBlue.copy(
                                        alpha = 0.7f * glow
                                    ),
                                    ElectricBlue.copy(
                                        alpha = 0.4f * glow
                                    ),
                                    Color.Transparent
                                ),
                                radius = size.minDimension * 0.9f
                            ),
                            radius = size.minDimension * 0.9f
                        )
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF00D2FF),
                                Color(0xFF0052D4),
                                Color(0xFF000106)
                            ),
                            radius = 260f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "V",
                    color = BrightWhite,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.W900
                )
            }


            // ====================================================
            // CHAT - TOP
            // ====================================================

            DashboardAction(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-10).dp),
                icon = Icons.Default.Chat,
                title = "Chat",
                onClick = {
                    onNewChat()
                    onNavigate(VisionTab.CHAT)
                }
            )


            // ====================================================
            // VOICE - RIGHT
            // ====================================================

            DashboardAction(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp),
                icon = Icons.Default.Mic,
                title = "Voice\nText",
                onClick = {
                    onNewChat()
                    onStartVoiceCall()
                }
            )


            // ====================================================
            // HISTORY - BOTTOM
            // ====================================================

            DashboardAction(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 10.dp),
                icon = Icons.Default.History,
                title = "History",
                onClick = onShowHistory
            )


            // ====================================================
            // SETTINGS - LEFT
            // ====================================================

            DashboardAction(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-10).dp),
                icon = Icons.Default.Settings,
                title = "Settings",
                onClick = {
                    onNavigate(VisionTab.SETTINGS)
                }
            )
        }


        // ========================================================
        // FOOTER
        // ========================================================

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, bottom = 28.dp)
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
    icon: ImageVector,
    title: String,
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
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Color(0xFF050E1E)
                )
                .border(
                    1.5.dp,
                    NeonBlue.copy(alpha = 0.8f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = BrightWhite,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = title,
            color = MutedTextBlue,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}
