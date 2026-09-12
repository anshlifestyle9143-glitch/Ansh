package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiEngineType
import com.example.ui.theme.VisionBackground
import com.example.ui.theme.VisionCardBg
import com.example.ui.theme.VisionCardBorder
import com.example.ui.theme.VisionDeepPlum
import com.example.ui.theme.VisionLilacLight
import com.example.ui.theme.VisionLilacPill
import com.example.ui.theme.VisionPrimaryPurple
import com.example.ui.theme.VisionTextMuted
import com.example.ui.theme.VisionTextSecondary
import com.example.ui.viewmodel.VisionViewModel

@Composable
fun EnginesScreen(
    viewModel: VisionViewModel,
    modifier: Modifier = Modifier
) {
    val activeEngine by viewModel.activeEngine.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VisionBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        // Section Header
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = VisionCardBg,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                VisionCardBorder.copy(alpha = 0.8f)
            ),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(VisionDeepPlum),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Modular AI Engine System",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                            color = VisionDeepPlum
                        )

                        Text(
                            text = "Multi-Model Orchestration",
                            style = MaterialTheme.typography.labelSmall,
                            color = VisionTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Available Neural Engines
        Text(
            text = "AVAILABLE NEURAL ENGINES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = VisionPrimaryPurple,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        AiEngineType.values().forEach { engine ->

            val isSelected = engine == activeEngine
            val badgeColor = Color(engine.badgeColorHex)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) {
                    VisionLilacPill
                } else {
                    VisionCardBg
                },
                border = androidx.compose.foundation.BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) {
                        VisionDeepPlum
                    } else {
                        VisionCardBorder.copy(alpha = 0.8f)
                    }
                ),
                shadowElevation = if (isSelected) 2.dp else 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        viewModel.setEngine(engine)
                    }
                    .testTag("engine_card_${engine.id}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            val icon = when (engine) {
                                AiEngineType.VISION_CORE ->
                                    Icons.Default.Bolt

                                AiEngineType.VISION_NEURAL_PRO ->
                                    Icons.Default.Psychology

                                AiEngineType.VISION_CREATIVE ->
                                    Icons.Default.AutoAwesome

                                AiEngineType.VISION_OFFLINE ->
                                    Icons.Default.Storage
                            }

                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = engine.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = VisionDeepPlum
                            )
                        }

                        // Active Badge
                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = VisionLilacLight,
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    VisionDeepPlum
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 2.dp
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = VisionDeepPlum,
                                        modifier = Modifier.size(12.dp)
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = VisionDeepPlum
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = engine.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = VisionTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Model: ${engine.modelTag}",
                            style = MaterialTheme.typography.labelSmall,
                            color = VisionTextMuted
                        )

                        Text(
                            text = if (
                                engine == AiEngineType.VISION_OFFLINE
                            ) {
                                "Zero Latency"
                            } else {
                                "60s Max Timeout"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = VisionTextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
