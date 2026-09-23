package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import kotlin.math.abs
import kotlin.math.sin

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

    var isListening by remember {
        mutableStateOf(false)
    }

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
                /*
                 * keepAlive recognizer internally restart ho sakta hai.
                 * Isliye UI state ko yahan false nahi kar rahe.
                 * User jab × dabayega tabhi listening mode close hoga.
                 */
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
                        key = { message ->
                            message.id
                        }
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
    Spacer(
        modifier = Modifier.fillMaxSize()
    )
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
            border = BorderStroke(
                1.dp,
                VisionCardBorder.copy(alpha = 0.7f)
            )
        ) {

            Text(
                text =
                    "${engine.displayName} is synthesizing neural response...",
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
        border = BorderStroke(
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

            /*
             * =====================================================
             * LISTENING MODE
             *
             * LEFT  -> STOP
             * CENTER -> LIVE WAVEFORM
             * RIGHT -> NOTHING
             * =====================================================
             */

            if (isListening) {

                IconButton(
                    onClick = onStopVoiceClick,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            VisionRose.copy(
                                alpha = 0.16f
                            )
                        )
                        .testTag("stop_voice_button")
                ) {

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop listening",
                        tint = VisionRose,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {

                    VoiceListeningWaveform()
                }

            } else {

                /*
                 * =================================================
                 * NORMAL CHAT MODE
                 *
                 * LEFT
                 *   +
                 *
                 * CENTER
                 *   Message Vision...
                 *
                 * RIGHT
                 *   MIC + SEND
                 *
                 * PLUS IS INTENTIONALLY NON-CLICKABLE.
                 * =================================================
                 */

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .testTag(
                            "attachment_placeholder"
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = VisionTextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                OutlinedTextField(
                    value = value,

                    onValueChange = onValueChange,

                    placeholder = {

                        Text(
                            text = "Message Vision...",
                            style =
                                MaterialTheme
                                    .typography
                                    .bodyMedium,
                            color = VisionTextMuted
                        )
                    },

                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor =
                                Color.Transparent,

                            unfocusedBorderColor =
                                Color.Transparent,

                            focusedTextColor =
                                VisionTextPrimary,

                            unfocusedTextColor =
                                VisionTextPrimary,

                            cursorColor =
                                VisionDeepPlum
                        ),

                    maxLines = 4,

                    modifier = Modifier
                        .weight(1f)
                        .testTag(
                            "chat_text_input"
                        )
                )

                /*
                 * =================================================
                 * MICROPHONE
                 *
                 * RIGHT SIDE
                 * =================================================
                 */

                IconButton(
                    onClick = onVoiceClick,
                    enabled = !isGenerating,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag(
                            "voice_input_button"
                        )
                ) {

                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription =
                            "Voice Dictation",
                        tint = VisionPrimaryPurple,
                        modifier = Modifier.size(23.dp)
                    )
                }

                /*
                 * =================================================
                 * SEND BUTTON
                 * =================================================
                 */

                val canSend =
                    value.isNotBlank() &&
                    !isGenerating

                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                        .testTag(
                            "send_button"
                        ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.ArrowUpward,
                        contentDescription =
                            "Send prompt",
                        tint =
                            if (canSend) {
                                Color.White
                            } else {
                                VisionTextMuted
                            },
                        modifier =
                            Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}


/*
 * =============================================================
 * LIVE VOICE WAVEFORM
 *
 * Purple / blue glowing-style animated waveform.
 *
 * It starts automatically whenever isListening == true.
 * =============================================================
 */

@Composable
private fun VoiceListeningWaveform() {

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "voice_waveform_transition"
        )

    val phase by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (Math.PI * 2.0).toFloat(),
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 850,
                            easing = LinearEasing
                        ),
                    repeatMode =
                        RepeatMode.Restart
                ),
            label = "voice_waveform_phase"
        )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {

        val centerY =
            size.height / 2f

        val centerX =
            size.width / 2f

        val halfWidth =
            size.width * 0.44f

        val barCount = 31

        /*
         * Purple -> blue -> purple
         */
        val waveformBrush =
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF6A28FF),
                    Color(0xFF815CFF),
                    Color(0xFF596BFF),
                    Color(0xFF168FFF),
                    Color(0xFF596BFF),
                    Color(0xFF815CFF),
                    Color(0xFF6A28FF)
                )
            )

        /*
         * Draw waveform bars.
         */
        for (index in 0 until barCount) {

            val normalized =
                index / (barCount - 1f)

            val x =
                centerX -
                    halfWidth +
                    (
                        halfWidth *
                            2f *
                            normalized
                    )

            /*
             * Bigger waveform in the center,
             * smaller toward both ends.
             */
            val distanceFromCenter =
                abs(
                    normalized - 0.5f
                ) * 2f

            val envelope =
                (
                    1f -
                        distanceFromCenter
                ).coerceIn(
                    0f,
                    1f
                )

            /*
             * Animated movement.
             */
            val wave =
                (
                    0.5f +
                        0.5f *
                        sin(
                            phase * 2f +
                                normalized *
                                Math.PI.toFloat() *
                                5f
                        )
                )

            val secondWave =
                (
                    0.5f +
                        0.5f *
                        sin(
                            phase * 1.35f +
                                normalized *
                                Math.PI.toFloat() *
                                8f
                        )
                )

            val amplitude =
                3f +
                    envelope *
                    (
                        14f +
                            wave * 8f +
                            secondWave * 4f
                    )

            /*
             * Main glowing-looking waveform.
             */
            drawLine(
                brush = waveformBrush,

                start =
                    androidx.compose.ui.geometry
                        .Offset(
                            x = x,
                            y = centerY - amplitude
                        ),

                end =
                    androidx.compose.ui.geometry
                        .Offset(
                            x = x,
                            y = centerY + amplitude
                        ),

                strokeWidth = 3.2f,

                cap = StrokeCap.Round
            )
        }

        /*
         * Thin center line.
         */
        drawLine(
            brush = waveformBrush,

            start =
                androidx.compose.ui.geometry
                    .Offset(
                        x = centerX - halfWidth,
                        y = centerY
                    ),

            end =
                androidx.compose.ui.geometry
                    .Offset(
                        x = centerX + halfWidth,
                        y = centerY
                    ),

            strokeWidth = 1.2f,

            cap = StrokeCap.Round
        )
    }
}
