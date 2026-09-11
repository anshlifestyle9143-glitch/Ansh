package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.data.model.ChatMessage
import com.example.ui.components.ChatMessageItem
import com.example.ui.theme.VisionBackground
import com.example.ui.theme.VisionCardBg
import com.example.ui.theme.VisionCardBorder
import com.example.ui.theme.VisionDeepPlum
import com.example.ui.theme.VisionEmerald
import com.example.ui.theme.VisionLilacLight
import com.example.ui.theme.VisionLilacPill
import com.example.ui.theme.VisionPrimaryPurple
import com.example.ui.theme.VisionSurface
import com.example.ui.theme.VisionTextMuted
import com.example.ui.theme.VisionTextPrimary
import com.example.ui.theme.VisionTextSecondary
import com.example.ui.viewmodel.VisionViewModel
import java.util.Locale

@Composable
fun ChatScreen(
    viewModel: VisionViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val userInput by viewModel.userInput.collectAsState()
    val activeEngine by viewModel.activeEngine.collectAsState()
    val isSpeaking by viewModel.ttsManager.isSpeaking.collectAsState()
    val currentSpeakingId by viewModel.ttsManager.currentSpeakingId.collectAsState()

    val listState = rememberLazyListState()

    // Auto-scroll to latest message
    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice recognition launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.onUserInputChange(spokenText)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VisionBackground)
            .imePadding()
    ) {
        // Message list or empty starter guide
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                EmptyChatGuide(
                    activeEngine = activeEngine,
                    onPromptClick = { prompt -> viewModel.sendMessage(prompt) }
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatMessageItem(
                            message = message,
                            isSpeaking = isSpeaking && currentSpeakingId == message.id,
                            onSpeakClick = { viewModel.toggleSpeak(message) }
                        )
                    }

                    if (isGenerating) {
                        item {
                            ThinkingIndicator(engine = activeEngine)
                        }
                    }
                }
            }
        }

        // Quick suggestions chips
        QuickPromptChips(
            onPromptSelected = { prompt -> viewModel.sendMessage(prompt) }
        )

        // Input Bar
        ChatInputBar(
            value = userInput,
            onValueChange = { viewModel.onUserInputChange(it) },
            onSend = { viewModel.sendMessage() },
            isGenerating = isGenerating,
            onVoiceClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Vision AI...")
                }
                try {
                    speechLauncher.launch(intent)
                } catch (e: Exception) {
                    // Speech intent unavailable
                }
            }
        )
    }
}

@Composable
fun EmptyChatGuide(
    activeEngine: AiEngineType,
    onPromptClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(VisionDeepPlum),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Vision AI",
                tint = Color.White,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Vision AI Assistant",
            style = MaterialTheme.typography.displayMedium,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
            color = VisionDeepPlum
        )

        Text(
            text = "Engineered by Ansh Yadav with modular neural intelligence and persistent local memory vault.",
            style = MaterialTheme.typography.bodyMedium,
            color = VisionTextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "SUGGESTED INITIALIZATIONS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = VisionPrimaryPurple
        )

        Spacer(modifier = Modifier.height(12.dp))

        val prompts = listOf(
            "Who created you and what is your architecture?",
            "What facts are currently in your Memory Vault?",
            "Write a clean Jetpack Compose Architecture sample",
            "Run Vision HUD system diagnostics"
        )

        prompts.forEach { prompt ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = VisionCardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, VisionCardBorder.copy(alpha = 0.7f)),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onPromptClick(prompt) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(VisionLilacPill),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = VisionDeepPlum,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = VisionTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun ThinkingIndicator(engine: AiEngineType) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(VisionLilacPill),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = VisionDeepPlum,
                strokeWidth = 2.dp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = VisionCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, VisionCardBorder.copy(alpha = 0.7f))
        ) {
            Text(
                text = "${engine.displayName} is synthesizing neural response...",
                style = MaterialTheme.typography.bodySmall,
                color = VisionTextSecondary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun QuickPromptChips(
    onPromptSelected: (String) -> Unit
) {
    val quickItems = listOf(
        "🧠 Check Memory Vault" to "What facts do you remember about me?",
        "⚡ Diagnostics" to "Run Vision HUD system diagnostics",
        "💻 Kotlin Code" to "Explain Kotlin Coroutines and StateFlow in Jetpack Compose",
        "🎨 Creative Mode" to "Generate 3 innovative Android app concepts for 2026",
        "👤 Creator Bio" to "Who created Vision AI and what was their design vision?"
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        items(quickItems) { item ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VisionCardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, VisionCardBorder.copy(alpha = 0.7f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onPromptSelected(item.second) }
            ) {
                Text(
                    text = item.first,
                    style = MaterialTheme.typography.labelSmall,
                    color = VisionTextSecondary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isGenerating: Boolean,
    onVoiceClick: () -> Unit
) {
    Surface(
        color = VisionCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, VisionCardBorder),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onVoiceClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("voice_input_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Dictation",
                    tint = VisionPrimaryPurple,
                    modifier = Modifier.size(22.dp)
                )
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = "Message Vision...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VisionTextMuted
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = VisionTextPrimary,
                    unfocusedTextColor = VisionTextPrimary,
                    cursorColor = VisionDeepPlum
                ),
                maxLines = 4,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_text_input")
            )

            val canSend = value.isNotBlank() && !isGenerating

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (canSend) VisionDeepPlum else VisionLilacPill)
                    .clickable(enabled = canSend) { onSend() }
                    .testTag("send_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Send prompt",
                    tint = if (canSend) Color.White else VisionTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

