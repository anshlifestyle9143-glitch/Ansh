package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VisionBackground
import com.example.ui.theme.VisionCardBg
import com.example.ui.theme.VisionCardBorder
import com.example.ui.theme.VisionDeepPlum
import com.example.ui.theme.VisionPrimaryPurple
import com.example.ui.theme.VisionTextMuted
import com.example.ui.theme.VisionTextSecondary
import com.example.ui.viewmodel.VisionViewModel

@Composable
fun SettingsScreen(
    viewModel: VisionViewModel,
    onBack: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenVisionInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VisionBackground)
            .padding(horizontal = 16.dp)
    ) {

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack,
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = VisionDeepPlum
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    color = VisionDeepPlum
                )

                Text(
                    text = "Vision configuration & system",
                    style = MaterialTheme.typography.labelSmall,
                    color = VisionTextSecondary
                )
            }
        }

        Text(
            text = "VOICE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = VisionPrimaryPurple,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        // Wake Word Toggle
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = VisionCardBg,
            border = BorderStroke(
                1.dp,
                VisionCardBorder.copy(alpha = 0.8f)
            ),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 15.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = VisionDeepPlum
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Wake Word",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Wake Word",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = VisionDeepPlum
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = "Say \"Hey Vision\" anytime, even screen off",
                            style = MaterialTheme.typography.bodySmall,
                            color = VisionTextSecondary
                        )
                    }
                }

                Switch(
                    checked = wakeWordEnabled,
                    onCheckedChange = {
                        viewModel.setWakeWordEnabled(it)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "SYSTEM",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.4.sp,
            color = VisionPrimaryPurple,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        SettingsItem(
            icon = Icons.Default.Memory,
            title = "Memory Vault",
            subtitle = "Manage Vision's stored memories and facts",
            onClick = onOpenMemory
        )

        Spacer(modifier = Modifier.height(10.dp))

        SettingsItem(
            icon = Icons.Default.Info,
            title = "Vision Info",
            subtitle = "Creator, system details and application information",
            onClick = onOpenVisionInfo
        )
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = VisionCardBg,
        border = BorderStroke(
            1.dp,
            VisionCardBorder.copy(alpha = 0.8f)
        ),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 15.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = VisionDeepPlum
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = VisionDeepPlum
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = VisionTextSecondary
                )
            }

            Text(
                text = "›",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = VisionTextMuted
            )
        }
    }
}
