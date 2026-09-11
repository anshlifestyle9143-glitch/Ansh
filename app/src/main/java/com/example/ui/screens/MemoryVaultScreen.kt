package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.model.MemoryCategory
import com.example.data.model.MemoryFact
import com.example.ui.theme.VisionAmber
import com.example.ui.theme.VisionBackground
import com.example.ui.theme.VisionCardBg
import com.example.ui.theme.VisionCardBorder
import com.example.ui.theme.VisionDeepPlum
import com.example.ui.theme.VisionEmerald
import com.example.ui.theme.VisionIndigo
import com.example.ui.theme.VisionLilacLight
import com.example.ui.theme.VisionLilacPill
import com.example.ui.theme.VisionPrimaryPurple
import com.example.ui.theme.VisionRose
import com.example.ui.theme.VisionSurface
import com.example.ui.theme.VisionTextMuted
import com.example.ui.theme.VisionTextPrimary
import com.example.ui.theme.VisionTextSecondary
import com.example.ui.viewmodel.VisionViewModel

@Composable
fun MemoryVaultScreen(
    viewModel: VisionViewModel,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.memories.collectAsState()
    val searchQuery by viewModel.memorySearchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<MemoryCategory?>(null) }

    val filteredMemories = remember(memories, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) {
            memories
        } else {
            memories.filter { it.category == selectedCategoryFilter?.name }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VisionBackground)
            .padding(horizontal = 16.dp)
    ) {
        // Vault Header & Stats Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = VisionCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, VisionCardBorder.copy(alpha = 0.8f)),
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(VisionDeepPlum),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Local Memory Vault",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                color = VisionDeepPlum
                            )
                            Text(
                                text = "Persistent on-device knowledge bank",
                                style = MaterialTheme.typography.labelSmall,
                                color = VisionTextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VisionDeepPlum,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_memory_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Fact",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add Fact", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MemoryStatBadge(label = "Active Memories", value = "${memories.size}")
                    MemoryStatBadge(label = "Injection Status", value = "Synchronized", color = VisionEmerald)
                    MemoryStatBadge(label = "Local Engine", value = "Room DB", color = VisionIndigo)
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setMemorySearchQuery(it) },
            placeholder = { Text("Search learned memories & facts...", color = VisionTextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = VisionPrimaryPurple,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.setMemorySearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = VisionTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VisionCardBg,
                unfocusedContainerColor = VisionCardBg,
                focusedBorderColor = VisionDeepPlum,
                unfocusedBorderColor = VisionCardBorder,
                focusedTextColor = VisionTextPrimary,
                unfocusedTextColor = VisionTextPrimary
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("memory_search_input")
        )

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            item {
                CategoryChip(
                    title = "All (${memories.size})",
                    isSelected = selectedCategoryFilter == null,
                    color = VisionDeepPlum,
                    onClick = { selectedCategoryFilter = null }
                )
            }
            items(MemoryCategory.values()) { category ->
                val count = memories.count { it.category == category.name }
                CategoryChip(
                    title = "${category.displayName} ($count)",
                    isSelected = selectedCategoryFilter == category,
                    color = Color(category.colorHex),
                    onClick = { selectedCategoryFilter = category }
                )
            }
        }

        // Memories List
        if (filteredMemories.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = VisionTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No matching memories found" else "Memory Vault is empty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VisionTextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredMemories, key = { it.id }) { memory ->
                    MemoryFactCard(
                        memory = memory,
                        onDelete = { viewModel.deleteMemory(memory.id) }
                    )
                }
            }
        }
    }

    // Add Memory Dialog
    if (showAddDialog) {
        AddMemoryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { category, key, detail, importance ->
                viewModel.addMemory(category, key, detail, importance)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MemoryStatBadge(label: String, value: String, color: Color = VisionDeepPlum) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = VisionTextMuted, fontSize = 10.sp)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun CategoryChip(
    title: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) VisionLilacPill else VisionCardBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) VisionDeepPlum else VisionCardBorder
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) VisionDeepPlum else VisionTextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun MemoryFactCard(
    memory: MemoryFact,
    onDelete: () -> Unit
) {
    val categoryEnum = MemoryCategory.values().find { it.name == memory.category } ?: MemoryCategory.KNOWLEDGE
    val categoryColor = Color(categoryEnum.colorHex)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = VisionCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, VisionCardBorder.copy(alpha = 0.8f)),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("memory_card_${memory.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = VisionLilacPill,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, VisionCardBorder)
                ) {
                    Text(
                        text = categoryEnum.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = categoryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Importance Stars
                    repeat(memory.importance) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = VisionAmber,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Memory",
                            tint = VisionRose,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = memory.keyName,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                color = VisionDeepPlum
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = memory.factDetail,
                style = MaterialTheme.typography.bodyMedium,
                color = VisionTextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onSave: (category: MemoryCategory, key: String, detail: String, importance: Int) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(MemoryCategory.USER_PROFILE) }
    var keyName by remember { mutableStateOf("") }
    var factDetail by remember { mutableStateOf("") }
    var importance by remember { mutableFloatStateOf(4f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VisionCardBg,
        title = {
            Text(
                text = "Store New Memory Fact",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                color = VisionDeepPlum
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelSmall,
                    color = VisionTextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    items(MemoryCategory.values()) { cat ->
                        CategoryChip(
                            title = cat.displayName,
                            isSelected = selectedCategory == cat,
                            color = Color(cat.colorHex),
                            onClick = { selectedCategory = cat }
                        )
                    }
                }

                OutlinedTextField(
                    value = keyName,
                    onValueChange = { keyName = it },
                    label = { Text("Fact Key (e.g. Favorite Language, Project Name)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VisionDeepPlum,
                        unfocusedBorderColor = VisionCardBorder,
                        focusedTextColor = VisionTextPrimary,
                        unfocusedTextColor = VisionTextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = factDetail,
                    onValueChange = { factDetail = it },
                    label = { Text("Detailed Context / Memory Fact") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VisionDeepPlum,
                        unfocusedBorderColor = VisionCardBorder,
                        focusedTextColor = VisionTextPrimary,
                        unfocusedTextColor = VisionTextPrimary
                    ),
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Priority Rating: ${importance.toInt()} / 5",
                        style = MaterialTheme.typography.labelSmall,
                        color = VisionTextSecondary
                    )
                }

                Slider(
                    value = importance,
                    onValueChange = { importance = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = VisionDeepPlum,
                        activeTrackColor = VisionDeepPlum,
                        inactiveTrackColor = VisionLilacPill
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keyName.isNotBlank() && factDetail.isNotBlank()) {
                        onSave(selectedCategory, keyName, factDetail, importance.toInt())
                    }
                },
                enabled = keyName.isNotBlank() && factDetail.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = VisionDeepPlum, contentColor = Color.White)
            ) {
                Text("Save Memory", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VisionTextMuted)
            }
        }
    )
}

