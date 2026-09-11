package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VisionCardBorder
import com.example.ui.theme.VisionDeepPlum
import com.example.ui.theme.VisionLilacPill
import com.example.ui.theme.VisionSurface
import com.example.ui.theme.VisionTextSecondary

enum class VisionTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME(
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_home"
    ),

    CHAT(
        title = "Assistant",
        selectedIcon = Icons.Filled.ChatBubble,
        unselectedIcon = Icons.Outlined.ChatBubbleOutline,
        testTag = "nav_tab_chat"
    ),

    MEMORY(
        title = "Memory Vault",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Psychology,
        testTag = "nav_tab_memory"
    ),

    ENGINES(
        title = "AI Engines",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_tab_engines"
    ),

    CREATOR(
        title = "Vision Info",
        selectedIcon = Icons.Filled.Info,
        unselectedIcon = Icons.Outlined.Info,
        testTag = "nav_tab_creator"
    )
}

@Composable
fun VisionBottomNav(
    currentTab: VisionTab,
    onTabSelected: (VisionTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = VisionSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            VisionCardBorder.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 4.dp,
                    vertical = 6.dp
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VisionTab.values().forEach { tab ->

                val isSelected = tab == currentTab

                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) {
                        VisionDeepPlum
                    } else {
                        VisionTextSecondary
                    },
                    label = "navTint"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onTabSelected(tab)
                        }
                        .padding(
                            horizontal = 1.dp,
                            vertical = 2.dp
                        )
                        .testTag(tab.testTag)
                ) {

                    Box(
                        modifier = Modifier
                            .size(
                                if (isSelected) {
                                    34.dp
                                } else {
                                    30.dp
                                }
                            )
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (isSelected) {
                                    VisionLilacPill
                                } else {
                                    Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) {
                                tab.selectedIcon
                            } else {
                                tab.unselectedIcon
                            },
                            contentDescription = tab.title,
                            tint = iconTint,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(2.dp)
                    )

                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        ),
                        color = if (isSelected) {
                            VisionDeepPlum
                        } else {
                            VisionTextSecondary
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}
