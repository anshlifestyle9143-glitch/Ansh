package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AiEngineType
import com.example.ui.components.ChatMessageItem
import com.example.ui.theme.VisionBackground
import com.example.ui.theme.VisionCardBg
import com.example.ui.theme.VisionCardBorder
import com.example.ui.theme.VisionDeepPlum
import com.example.ui.theme.VisionLilacPill
import com.example.ui.theme.VisionPrimaryPurple
import com.example.ui.theme.VisionRose
import com.example.ui.theme.VisionTextMuted
import com.example.ui.theme.VisionTextPrimary
import com.example.ui.theme.VisionTextSecondary
import com.example.ui.viewmodel.VisionViewModel
import com.example.util.LiveSpeechRecognizer

@Composable
fun ChatScreen(
    viewModel: VisionViewModel,
    autoStartVoice: Boolean = false,
    onAutoStartHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val userInput by viewModel.userInput.collectAsState()
    val activeEngine by viewModel.activeEngine.collectAsState()

    val isSpeaking by viewModel.ttsManager.isSpeaking.collectAsState()
    val currentSpeakingId by viewModel.ttsManager.currentSpeakingId.collectAsState()

    val listState = rememberLazyListState()

    val speechRecognizer = remember {
        LiveSpeechRecognizer(context)
    }

    var isListening by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    fun startListening() {
        isListening = true
        speechRecognizer.start(
            onPartial = { text ->
                viewModel.onUserInputChange(text)
            },
            onFinal = { text ->
                viewModel.onUserInputChange(text)
            },
            onListeningChange = { listening ->
                // keepAlive restarts happen fast; only flip the
                // flag off when the user explicitly stops.
                if (!listening) {
                    // still "active" from the user's point of view —
                    // recognizer is mid-restart, not truly off.
                }
            },
            onError = {},
            keepAlive = true
        )
    }

    fun stopListening() {
        isListening = false
        speechRecognizer.stop()
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startListening()
            }
        }

    fun requestListening() {
        val hasPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startListening()
        } else {
            permissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    LaunchedEffect(autoStartVoice) {
        if (autoStartVoice) {
            requestListening()
            onAutoStartHandled()
        }
    }

    LaunchedEffect(messages.size, isGenerating) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(
                messages.size - 1
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VisionBackground)
            .imePadding()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                EmptyChatGuide(
                    activeEngine = activeEngine,
                    onPromptClick = { prompt ->
                        viewModel.sendMessage(prompt)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        vertical = 12.dp
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = messages,
                        key = { message -> message.id }
                    ) { message ->

                        ChatMessageItem(
                            message = message,
                            isSpeaking =
                                isSpeaking &&
                                currentSpeakingId == message.id,
                            onSpeakClick = {
                                viewModel.toggleSpeak(message)
                            }
                        )
                    }

                    if (isGenerating) {
                        item {
                            ThinkingIndicator(
                                engine = activeEngine
                            )
                        }
                    }
                }
            }
        }

        ChatInputBar(
            value = userInput,
            onValueChange = {
                viewModel.onUserInputChange(it)
            },
            onSend = {
                viewModel.sendMessage()
            },
            isGenerating = isGenerating,
            isListening = isListening,
            onVoiceClick = {
                requestListening()
            },
            onStopVoiceClick = {
                stopListening()
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

        Spacer(
            modifier = Modifier.height(16.dp)
        )

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
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 6.dp
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ThinkingIndicator(
    engine: AiEngineType
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
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

        Spacer(
            modifier = Modifier.width(10.dp)
        )

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = VisionCardBg,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                VisionCardBorder.copy(alpha = 0.7f)
            )
        ) {
            Text(
                text = "${engine.displayName} is synthesizing neural response...",
                style = MaterialTheme.typography.bodySmall,
                color = VisionTextSecondary,
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                )
            )
        }
    }
}

@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isGenerating: Boolean,
    isListening: Boolean,
    onVoiceClick: () -> Unit,
    onStopVoiceClick: () -> Unit
) {
    Surface(
        color = VisionCardBg,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            VisionCardBorder
        ),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp
            )
            .clip(
                RoundedCornerShape(26.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp
                ),
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
                    tint = if (isListening) {
                        VisionRose
                    } else {
                        VisionPrimaryPurple
                    },
                    modifier = Modifier.size(22.dp)
                )
            }

            if (isListening) {
                IconButton(
                    onClick = onStopVoiceClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(VisionRose.copy(alpha = 0.15f))
                        .testTag("stop_voice_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop listening",
                        tint = VisionRose,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = if (isListening) {
                            "Sun rahi hoon..."
                        } else {
                            "Message Vision..."
                        },
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

            val canSend =
                value.isNotBlank() && !isGenerating

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) {
                            VisionDeepPlum
                        } else {
                            VisionLilacPill
                        }
                    )
                    .clickable(
                        enabled = canSend
                    ) {
                        onSend()
                    }
                    .testTag("send_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Send prompt",
                    tint =
                        if (canSend) {
                            Color.White
                        } else {
                            VisionTextMuted
                        },
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
