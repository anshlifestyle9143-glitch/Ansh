package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.VisionTab
import com.example.ui.theme.VisionCardBg
import com.example.ui.theme.VisionCardBorder
import com.example.ui.theme.VisionDeepPlum
import com.example.ui.theme.VisionEmerald
import com.example.ui.theme.VisionIndigo
import com.example.ui.theme.VisionLilacLight
import com.example.ui.theme.VisionTextMuted
import com.example.ui.theme.VisionTextSecondary
import com.example.ui.viewmodel.VisionViewModel
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun DashboardScreen(
    viewModel: VisionViewModel,
    onNavigate: (VisionTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.memories.collectAsState()
    val activeEngine by viewModel.activeEngine.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 90.dp
        )
    ) {

        // Minimal header
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
                            "SYSTEM LIVE",
                            color = VisionEmerald,
                            fontSize = 10.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    activeEngine.displayName,
                    color = VisionTextMuted,
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }
        }

        // Radial hub — Vision orb + orbiting shortcuts
        item {
            RadialHub(
                memoriesCount = memories.size,
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
    memoriesCount: Int,
    onCenterClick: () -> Unit,
    onNavigate: (VisionTab) -> Unit
) {
    val satellites = listOf(
        Triple(
            "Chat",
            Icons.Default.Chat,
            VisionDeepPlum
        ) to VisionTab.CHAT,

        Triple(
            "Memory",
            Icons.Default.Psychology,
            VisionIndigo
        ) to VisionTab.MEMORY,

        Triple(
            "Engines",
            Icons.Default.Tune,
            VisionEmerald
        ) to VisionTab.ENGINES,

        Triple(
            "Info",
            Icons.Default.Info,
            VisionTextSecondary
        ) to VisionTab.CREATOR
    )

    val infiniteTransition =
        rememberInfiniteTransition(label = "hubPulse")

    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .size(320.dp)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {

        // Outer faint ring
        Box(
            modifier = Modifier
                .size(300.dp)
                .clip(CircleShape)
                .border(
                    BorderStroke(
                        1.dp,
                        VisionCardBorder
                    ),
                    CircleShape
                )
        )

        // Inner faint ring
        Box(
            modifier = Modifier
                .size(230.dp)
                .clip(CircleShape)
                .border(
                    BorderStroke(
                        1.dp,
                        VisionCardBorder.copy(alpha = 0.7f)
                    ),
                    CircleShape
                )
        )

        // Pulsing glow behind center orb
        Box(
            modifier = Modifier
                .size(150.dp)
                .graphicsLayerScale(ringScale)
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

        // Satellite buttons
        val radius = 128f

        satellites.forEachIndexed { index, (info, tab) ->

            val (label, icon, accent) = info

            val angleDeg =
                -90.0 + (360.0 / satellites.size) * index

            val angleRad =
                Math.toRadians(angleDeg)

            val x =
                (radius * cos(angleRad))
                    .toFloat()
                    .dp

            val y =
                (radius * sin(angleRad))
                    .toFloat()
                    .dp

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .offset(
                        x = x,
                        y = y
                    )
                    .clickable {
                        onNavigate(tab)
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(VisionCardBg)
                        .border(
                            BorderStroke(
                                1.dp,
                                accent.copy(alpha = 0.4f)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    label,
                    color = VisionTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            }
        }

        // Center Vision orb
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            VisionLilacLight,
                            VisionDeepPlum.copy(alpha = 0.3f)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.5.dp,
                        VisionDeepPlum.copy(alpha = 0.6f)
                    ),
                    CircleShape
                )
                .clickable {
                    onCenterClick()
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "Vision",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    "Vision",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
        }
    }
}

private fun Modifier.graphicsLayerScale(
    scale: Float
): Modifier =
    this.then(
        Modifier.graphicsLayer(
            scaleX = scale,
            scaleY = scale
        )
    )
