package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.VisionTab
import com.example.ui.theme.VisionDeepPlum
import com.example.ui.theme.VisionEmerald
import com.example.ui.theme.VisionLilacLight
import com.example.ui.theme.VisionTextMuted
import com.example.ui.theme.VisionTextSecondary
import com.example.ui.viewmodel.VisionViewModel

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
            .background(Color.Black)
    ) {

        // System status
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(VisionEmerald)
                )

                Spacer(modifier = Modifier.width(6.dp))

                androidx.compose.material3.Text(
                    text = "SYSTEM LIVE",
                    color = VisionEmerald,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            androidx.compose.material3.Text(
                text = activeEngine.displayName,
                color = VisionTextMuted,
                fontSize = 9.sp
            )
        }

        // CHAT — TOP
        HubAction(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 62.dp),
            icon = Icons.Default.Chat,
            title = "Chat",
            subtitle = null,
            onClick = {
                onNewChat()
            }
        )

        // SETTINGS — LEFT
        HubAction(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp),
            icon = Icons.Default.Settings,
            title = "Settings",
            subtitle = null,
            onClick = {
                onNavigate(VisionTab.SETTINGS)
            }
        )

        // VOICE — RIGHT
        HubAction(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            icon = Icons.Default.RecordVoiceOver,
            title = "Voice",
            subtitle = null,
            onClick = {
                onStartVoiceCall()
            }
        )

        // HISTORY — BOTTOM
        HubAction(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp),
            icon = Icons.Default.History,
            title = "History",
            subtitle = null,
            onClick = {
                onShowHistory()
            }
        )

        // CENTER VISION CORE
        VisionCore(
            modifier = Modifier.align(Alignment.Center),
            onClick = {
                onNavigate(VisionTab.CHAT)
            }
        )
    }
}

@Composable
private fun VisionCore(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "vision_core"
        )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 10000,
                easing = LinearEasing
            )
        ),
        label = "coreRotation"
    )

    Box(
        modifier = modifier
            .size(300.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            },
        contentAlignment = Alignment.Center
    ) {

        // Outer ring
        Box(
            modifier = Modifier
                .size(292.dp)
                .graphicsLayer {
                    rotationZ = rotation
                }
                .border(
                    width = 1.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            VisionDeepPlum,
                            Color.Transparent,
                            VisionDeepPlum,
                            Color.Transparent,
                            VisionDeepPlum
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Second ring
        Box(
            modifier = Modifier
                .size(238.dp)
                .graphicsLayer {
                    rotationZ = -rotation * 0.65f
                }
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            VisionLilacLight,
                            Color.Transparent,
                            VisionDeepPlum,
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Glow
        Box(
            modifier = Modifier
                .size(210.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VisionLilacLight.copy(alpha = 0.55f),
                            VisionDeepPlum.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Core
        Surface(
            modifier = Modifier
                .size(148.dp)
                .clickable {
                    onClick()
                },
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 24.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                VisionLilacLight,
                                VisionDeepPlum.copy(alpha = 0.90f),
                                Color.Black
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                VisionLilacLight,
                                VisionDeepPlum,
                                VisionLilacLight,
                                VisionDeepPlum
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                androidx.compose.foundation.Image(
                    painter = painterResource(
                        id = R.drawable.ic_vision_logo
                    ),
                    contentDescription = "Vision AI Core",
                    modifier = Modifier.size(88.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun HubAction(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                onClick()
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VisionLilacLight.copy(alpha = 0.95f),
                            VisionDeepPlum.copy(alpha = 0.65f),
                            Color.Black
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            VisionLilacLight,
                            VisionDeepPlum,
                            Color.Transparent,
                            VisionLilacLight
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
                modifier = Modifier.size(27.dp)
            )
        }

        Spacer(modifier = Modifier.size(5.dp))

        Text(
            text = title,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )

        if (subtitle != null) {
            Text(
                text = subtitle,
                color = VisionTextSecondary,
                fontSize = 9.sp
            )
        }
    }
}
