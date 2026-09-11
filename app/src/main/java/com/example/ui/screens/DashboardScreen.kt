package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.VisionCardBorder
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
    modifier: Modifier = Modifier
) {
    val activeEngine by viewModel.activeEngine.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 90.dp
        )
    ) {

        // ---------------------------------------------------------
        // SYSTEM STATUS
        // ---------------------------------------------------------
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            VisionEmerald.copy(alpha = 0.12f)
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                VisionEmerald.copy(alpha = 0.25f)
                            ),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        )
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

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text(
                            text = "SYSTEM LIVE",
                            color = VisionEmerald,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = activeEngine.displayName,
                    color = VisionTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ---------------------------------------------------------
        // VISION AI CORE
        // ---------------------------------------------------------
        item {
            RadialHub(
                onCenterClick = {
                    onNavigate(VisionTab.CHAT)
                },
                onNavigate = onNavigate
            )
        }
    }
}


@Composable
private fun RadialHub(
    onCenterClick: () -> Unit,
    onNavigate: (VisionTab) -> Unit
) {

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "vision_ai_core"
        )

    // ---------------------------------------------------------
    // CORE PULSE
    // ---------------------------------------------------------
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1700,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    // ---------------------------------------------------------
    // OUTER RING ROTATION
    // ---------------------------------------------------------
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 10000,
                easing = LinearEasing
            )
        ),
        label = "rotation"
    )

    // ---------------------------------------------------------
    // GLOW PULSE
    // ---------------------------------------------------------
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.50f,
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
        modifier = Modifier
            .fillMaxWidth()
            .height(430.dp),
        contentAlignment = Alignment.Center
    ) {

        // -------------------------------------------------------
        // OUTER ROTATING AI RING
        // -------------------------------------------------------
        Box(
            modifier = Modifier
                .size(285.dp)
                .graphicsLayer {
                    rotationZ = rotation
                }
                .border(
                    BorderStroke(
                        1.dp,
                        VisionDeepPlum.copy(alpha = 0.28f)
                    ),
                    CircleShape
                )
        )

        // -------------------------------------------------------
        // SECOND AI RING
        // -------------------------------------------------------
        Box(
            modifier = Modifier
                .size(220.dp)
                .graphicsLayer {
                    scaleX = corePulse
                    scaleY = corePulse
                }
                .border(
                    BorderStroke(
                        1.5.dp,
                        VisionDeepPlum.copy(alpha = 0.40f)
                    ),
                    CircleShape
                )
        )

        // -------------------------------------------------------
        // AI ENERGY GLOW
        // -------------------------------------------------------
        Box(
            modifier = Modifier
                .size(190.dp)
                .graphicsLayer {
                    scaleX = corePulse
                    scaleY = corePulse
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VisionDeepPlum.copy(
                                alpha = glowAlpha
                            ),
                            Color.Transparent
                        )
                    )
                )
        )

        // -------------------------------------------------------
        // LEFT — VOICE TO TEXT
        // -------------------------------------------------------
        HubAction(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp),
            icon = Icons.Default.Mic,
            title = "Voice",
            subtitle = "Text",
            onClick = {
                onNavigate(VisionTab.CHAT)
            }
        )

        // -------------------------------------------------------
        // RIGHT — VOICE TO VOICE
        // -------------------------------------------------------
        HubAction(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            icon = Icons.Default.RecordVoiceOver,
            title = "Voice",
            subtitle = "Voice",
            onClick = {
                onNavigate(VisionTab.CHAT)
            }
        )

        // -------------------------------------------------------
        // BOTTOM — HISTORY
        // -------------------------------------------------------
        HubAction(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            icon = Icons.Default.History,
            title = "History",
            subtitle = null,
            onClick = {
                onNavigate(VisionTab.CHAT)
            }
        )

        // -------------------------------------------------------
        // MAIN VISION AI CORE
        // -------------------------------------------------------
        Surface(
            modifier = Modifier
                .size(148.dp)
                .graphicsLayer {
                    scaleX = corePulse
                    scaleY = corePulse
                }
                .clickable {
                    onCenterClick()
                },
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 22.dp
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                VisionLilacLight,
                                VisionDeepPlum.copy(
                                    alpha = 0.75f
                                ),
                                VisionDeepPlum.copy(
                                    alpha = 0.35f
                                )
                            )
                        )
                    )
                    .border(
                        BorderStroke(
                            2.dp,
                            VisionDeepPlum.copy(
                                alpha = 0.65f
                            )
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {

                // New V logo
                Image(
                    painter = painterResource(
                        id = R.drawable.ic_vision_logo
                    ),
                    contentDescription = "Vision AI",
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
            .clickable {
                onClick()
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // -------------------------------------------------------
        // ACTION CIRCLE
        // -------------------------------------------------------
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 8.dp
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                VisionLilacLight.copy(
                                    alpha = 0.80f
                                ),
                                VisionDeepPlum.copy(
                                    alpha = 0.35f
                                )
                            )
                        )
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            VisionDeepPlum.copy(
                                alpha = 0.45f
                            )
                        ),
                        CircleShape
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
        }

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = title,
            color = VisionTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (subtitle != null) {

            Text(
                text = subtitle,
                color = VisionTextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
