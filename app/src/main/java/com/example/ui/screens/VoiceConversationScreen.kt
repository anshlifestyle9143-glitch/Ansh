package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.VisionCommandExecutor
import com.example.service.VisionIntentClassifier
import com.example.ui.theme.VisionBackground
import com.example.ui.theme.VisionDeepPlum
import com.example.ui.theme.VisionEmerald
import com.example.ui.theme.VisionIndigo
import com.example.ui.theme.VisionTextPrimary
import com.example.ui.theme.VisionTextSecondary
import com.example.ui.viewmodel.VisionViewModel
import com.example.util.LiveSpeechRecognizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private enum class VoiceCallState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    NO_PERMISSION
}

@Composable
fun VoiceConversationScreen(
    viewModel: VisionViewModel,
    onExit: () -> Unit
) {
    val context = LocalContext.current

    val isGenerating by
        viewModel.isGenerating.collectAsState()

    val isSpeaking by
        viewModel.ttsManager.isSpeaking.collectAsState()

    var callState by remember {
        mutableStateOf(VoiceCallState.IDLE)
    }

    var active by remember {
        mutableStateOf(true)
    }

    var listeningStarted by remember {
        mutableStateOf(false)
    }

    val coroutineScope = rememberCoroutineScope()

    val speechRecognizer = remember {
        LiveSpeechRecognizer(context)
    }

    fun startListening() {

        if (!active) return

        if (listeningStarted) return

        if (isGenerating || isSpeaking) return

        listeningStarted = true

        speechRecognizer.start(

            onPartial = {
                /*
                 * Partial speech intentionally hidden.
                 */
            },

            onFinal = { text ->

                if (!active) {
                    return@start
                }

                listeningStarted = false

                val cleanText =
                    text.trim()

                if (cleanText.isBlank()) {

                    callState =
                        VoiceCallState.IDLE

                    return@start
                }

                coroutineScope.launch {

                    val intentType =
                        try {

                            VisionIntentClassifier()
                                .classify(
                                    cleanText
                                )
                                .type

                        } catch (e: Exception) {

                            VisionIntentClassifier
                                .IntentType
                                .CONVERSATION
                        }

                    if (!active) {
                        return@launch
                    }

                    when (intentType) {

                        VisionIntentClassifier
                            .IntentType
                            .ACTION -> {

                            callState =
                                VoiceCallState.SPEAKING

                            viewModel.ttsManager.speak(
                                "Ok Boss",
                                -System.currentTimeMillis()
                            )

                            val okBossStarted =
                                withTimeoutOrNull(
                                    3000L
                                ) {

                                    viewModel
                                        .ttsManager
                                        .isSpeaking
                                        .first { it }

                                    true

                                } == true

                            if (okBossStarted) {

                                withTimeoutOrNull(
                                    10000L
                                ) {

                                    viewModel
                                        .ttsManager
                                        .isSpeaking
                                        .first { !it }
                                }
                            }

                            if (!active) {
                                return@launch
                            }

                            callState =
                                VoiceCallState.THINKING

                            /*
                             * ---------------------------------------
                             * ACTUALLY EXECUTE THE DEVICE COMMAND
                             * ---------------------------------------
                             */
                            val result =
                                try {

                                    VisionCommandExecutor(context)
                                        .execute(cleanText)

                                } catch (e: Exception) {

                                    VisionCommandExecutor.CommandResult(
                                        success = false,
                                        action = "UNKNOWN",
                                        message =
                                            "Command execute nahi ho payi, Boss."
                                    )
                                }

                            if (!active) {
                                return@launch
                            }

                            callState =
                                VoiceCallState.SPEAKING

                            viewModel.ttsManager.speak(
                                result.message,
                                -System.currentTimeMillis()
                            )

                            val resultStarted =
                                withTimeoutOrNull(
                                    3000L
                                ) {

                                    viewModel
                                        .ttsManager
                                        .isSpeaking
                                        .first { it }

                                    true

                                } == true

                            if (resultStarted) {

                                withTimeoutOrNull(
                                    10000L
                                ) {

                                    viewModel
                                        .ttsManager
                                        .isSpeaking
                                        .first { !it }
                                }
                            }

                            if (!active) {
                                return@launch
                            }

                            delay(300L)

                            if (active) {
                                startListening()
                            }
                        }

                        VisionIntentClassifier
                            .IntentType
                            .CONVERSATION -> {

                            callState =
                                VoiceCallState.THINKING

                            viewModel.sendMessage(
                                overridePrompt =
                                    cleanText,
                                autoSpeak = true
                            )
                        }

                        VisionIntentClassifier
                            .IntentType
                            .QUESTION -> {

                            callState =
                                VoiceCallState.THINKING

                            viewModel.sendMessage(
                                overridePrompt =
                                    cleanText,
                                autoSpeak = true
                            )
                        }

                        VisionIntentClassifier
                            .IntentType
                            .SEARCH -> {

                            callState =
                                VoiceCallState.THINKING

                            viewModel.sendMessage(
                                overridePrompt =
                                    cleanText,
                                autoSpeak = true
                            )
                        }

                        VisionIntentClassifier
                            .IntentType
                            .UNCLEAR -> {

                            callState =
                                VoiceCallState.SPEAKING

                            viewModel.ttsManager.speak(
                                "Boss, thoda clearly bataiye.",
                                -System.currentTimeMillis()
                            )

                            withTimeoutOrNull(
                                10000L
                            ) {

                                viewModel
                                    .ttsManager
                                    .isSpeaking
                                    .first { !it }
                            }

                            if (!active) {
                                return@launch
                            }

                            delay(300L)

                            if (!active) {
                                return@launch
                            }

                            startListening()
                        }
                    }
                }
            },

            onListeningChange = { listening ->

                if (!active) {
                    return@start
                }

                if (listening) {

                    listeningStarted = true

                    callState =
                        VoiceCallState.LISTENING

                } else {
                    // Final result/error controls state.
                }
            },

            onError = {

                if (!active) {
                    return@start
                }

                listeningStarted = false

                callState =
                    VoiceCallState.IDLE
            },

            continuous = true
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (!active) {
                return@rememberLauncherForActivityResult
            }

            if (granted) {

                startListening()

            } else {

                listeningStarted = false

                callState =
                    VoiceCallState.NO_PERMISSION
            }
        }

    fun requestListening() {

        if (!active) return

        if (isGenerating || isSpeaking) return

        val hasPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) ==
                PackageManager.PERMISSION_GRANTED

        if (hasPermission) {

            startListening()

        } else {

            permissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO
            )
        }
    }

    DisposableEffect(Unit) {

        onDispose {

            active = false
            listeningStarted = false

            speechRecognizer.stop()
            speechRecognizer.destroy()
        }
    }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) {}

    LaunchedEffect(Unit) {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(Unit) {

        callState =
            VoiceCallState.SPEAKING

        viewModel.ttsManager.speak(
            "Yes Boss",
            -System.currentTimeMillis()
        )

        val yesBossStarted =
            withTimeoutOrNull(
                3000L
            ) {

                viewModel
                    .ttsManager
                    .isSpeaking
                    .first { it }

                true

            } == true

        if (yesBossStarted) {

            withTimeoutOrNull(
                10000L
            ) {

                viewModel
                    .ttsManager
                    .isSpeaking
                    .first { !it }
            }
        }

        delay(400L)

        if (active) {
            requestListening()
        }
    }

    LaunchedEffect(isGenerating) {

        if (!active) {
            return@LaunchedEffect
        }

        if (isGenerating) {

            listeningStarted = false

            callState =
                VoiceCallState.THINKING

            speechRecognizer.stop()
        }
    }

    LaunchedEffect(isSpeaking) {

        if (!active) {
            return@LaunchedEffect
        }

        if (isSpeaking) {

            if (
                callState !=
                    VoiceCallState.LISTENING
            ) {

                callState =
                    VoiceCallState.SPEAKING
            }

        } else {

            if (!isGenerating) {

                delay(300L)

                if (
                    active &&
                    !isGenerating &&
                    !isSpeaking
                ) {
                    requestListening()
                }
            }
        }
    }

    LaunchedEffect(isGenerating) {

        if (!active) {
            return@LaunchedEffect
        }

        if (isGenerating) {

            callState =
                VoiceCallState.THINKING

            speechRecognizer.stop()

        } else {

            delay(200L)

            if (
                active &&
                !isGenerating &&
                !isSpeaking
            ) {
                requestListening()
            }
        }
    }


    val infiniteTransition =
        rememberInfiniteTransition(label = "neural_interface")

    val flow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200),
            repeatMode = RepeatMode.Restart
        ),
        label = "neural_flow"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1050),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brain_pulse"
    )

    val stateTitle = when (callState) {
        VoiceCallState.LISTENING -> "LISTENING"
        VoiceCallState.THINKING -> "THINKING"
        VoiceCallState.SPEAKING -> "RESPONDING"
        VoiceCallState.IDLE -> "VISION READY"
        VoiceCallState.NO_PERMISSION -> "MICROPHONE ACCESS"
    }

    val stateSubtitle = when (callState) {
        VoiceCallState.LISTENING -> "Sun rahi hoon..."
        VoiceCallState.THINKING -> "Thinking..."
        VoiceCallState.SPEAKING -> "Bol rahi hoon..."
        VoiceCallState.IDLE -> "Ready"
        VoiceCallState.NO_PERMISSION -> "Microphone permission required"
    }

    val activeStage = when (callState) {
        VoiceCallState.LISTENING -> 0
        VoiceCallState.THINKING -> 1
        VoiceCallState.SPEAKING -> 2
        else -> -1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF020208),
                        Color(0xFF040510),
                        Color(0xFF05040D),
                        Color(0xFF020207)
                    )
                )
            )
    ) {

        // Very restrained ambient light. The interface should stay almost black.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF253D93).copy(alpha = 0.13f),
                            Color(0xFF301A66).copy(alpha = 0.07f),
                            Color.Transparent
                        ),
                        radius = 900f
                    )
                )
        )

        /* =====================================================
         * HEADER
         * ===================================================== */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 11.dp,
                    end = 18.dp,
                    top = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    active = false
                    listeningStarted = false
                    speechRecognizer.stop()
                    speechRecognizer.destroy()
                    onExit()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "End voice mode",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "VISION",
                    color = Color.White,
                    fontSize = 15.sp,
                    letterSpacing = 4.2.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "BY ANSH YADAV",
                    color = Color(0xFF6A6E86),
                    fontSize = 8.sp,
                    letterSpacing = 2.4.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(
                        when (callState) {
                            VoiceCallState.NO_PERMISSION -> Color(0xFFFF5B70)
                            else -> Color(0xFF4FE39A)
                        }
                    )
            )
        }

        /* =====================================================
         * STATE TITLE
         * ===================================================== */
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 105.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stateTitle,
                color = when (callState) {
                    VoiceCallState.LISTENING -> Color(0xFF74AFFF)
                    VoiceCallState.THINKING -> Color(0xFFAA7CFF)
                    VoiceCallState.SPEAKING -> Color(0xFF6D8FFF)
                    else -> Color(0xFF7B8097)
                },
                fontSize = 11.sp,
                letterSpacing = 3.6.sp
            )

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = stateSubtitle,
                color = Color(0xFFF1F0F7),
                fontSize = 22.sp
            )
        }

        /* =====================================================
         * BRAIN VISUALIZER
         * ===================================================== */
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 285.dp)
                .size(318.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawNeuralBrain(
                    pulse = pulse,
                    flow = flow,
                    active = callState != VoiceCallState.IDLE &&
                        callState != VoiceCallState.NO_PERMISSION,
                    thinking = callState == VoiceCallState.THINKING,
                    speaking = callState == VoiceCallState.SPEAKING
                )
            }
        }

        /* =====================================================
         * PROCESS PIPELINE
         * ===================================================== */
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 600.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val labels = listOf("UNDERSTAND", "ANALYZE", "RESPOND")

            labels.forEachIndexed { index, label ->
                val passed = activeStage >= index
                val current = activeStage == index

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (current) 9.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    current -> Color(0xFF6EA2FF)
                                    passed -> Color(0xFF52688F)
                                    else -> Color(0xFF303241)
                                }
                            )
                    )

                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = label,
                        color = when {
                            current -> Color(0xFFE9EAF5)
                            passed -> Color(0xFF6D7187)
                            else -> Color(0xFF464958)
                        },
                        fontSize = 8.sp,
                        letterSpacing = 1.35.sp
                    )
                }

                if (index < 2) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 0.dp)
                            .width(37.dp)
                            .height(1.dp)
                            .background(
                                if (activeStage > index)
                                    Color(0xFF586D9A)
                                else
                                    Color(0xFF2A2C39)
                            )
                    )
                }
            }
        }

        /* =====================================================
         * WAVEFORM
         * ===================================================== */
        NeuralWaveform(
            active = callState == VoiceCallState.LISTENING ||
                callState == VoiceCallState.THINKING ||
                callState == VoiceCallState.SPEAKING,
            thinking = callState == VoiceCallState.THINKING,
            speaking = callState == VoiceCallState.SPEAKING,
            flow = flow,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 648.dp)
        )

        /* =====================================================
         * COMPACT VOICE CONTROL
         * ===================================================== */
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = 22.dp,
                    end = 22.dp,
                    bottom = 20.dp
                ),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF0A0A12).copy(alpha = 0.97f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color(0xFF2A2D42)
            ),
            shadowElevation = 14.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 18.dp,
                        end = 10.dp,
                        top = 11.dp,
                        bottom = 11.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "VISION",
                        color = Color.White,
                        fontSize = 15.sp,
                        letterSpacing = 1.8.sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = when (callState) {
                            VoiceCallState.LISTENING -> "Listening in real-time"
                            VoiceCallState.THINKING -> "Processing your voice"
                            VoiceCallState.SPEAKING -> "Vision is speaking"
                            VoiceCallState.IDLE -> "Ready to listen"
                            VoiceCallState.NO_PERMISSION -> "Microphone permission required"
                        },
                        color = Color(0xFF7C8199),
                        fontSize = 10.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF5A7EFF),
                                    Color(0xFF1A1A36)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (callState == VoiceCallState.IDLE ||
                        callState == VoiceCallState.NO_PERMISSION
                    ) {
                        IconButton(
                            onClick = { requestListening() },
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Start listening",
                                tint = Color.White,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(13.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NeuralWaveform(
    active: Boolean,
    thinking: Boolean,
    speaking: Boolean,
    flow: Float,
    modifier: Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 66.dp)
    ) {
        val bars = 41
        val slot = size.width / bars
        val centerY = size.height / 2f

        for (i in 0 until bars) {
            val normalized = i.toFloat() / (bars - 1f)
            val centerEnvelope = 1f - kotlin.math.abs(normalized - 0.5f) * 1.9f
            val wave = kotlin.math.sin(flow * 6.283f + i * 0.72f)
            val secondary = kotlin.math.sin(flow * 12.566f - i * 0.38f)

            val barHeight = when {
                !active -> 3.2f
                speaking -> 5f + (centerEnvelope.coerceAtLeast(0f) * 21f) + kotlin.math.abs(wave) * 4f
                thinking -> 4f + (centerEnvelope.coerceAtLeast(0f) * 14f) + kotlin.math.abs(secondary) * 3f
                else -> 4f + (centerEnvelope.coerceAtLeast(0f) * 18f) + kotlin.math.abs(wave) * 5f
            }

            val x = slot * i + slot / 2f

            drawLine(
                color = when {
                    speaking -> Color(0xFF668FFF).copy(alpha = 0.72f)
                    thinking -> Color(0xFFA275FF).copy(alpha = 0.68f)
                    active -> Color(0xFF568CFF).copy(alpha = 0.70f)
                    else -> Color(0xFF3A4053).copy(alpha = 0.48f)
                },
                start = Offset(x, centerY - barHeight),
                end = Offset(x, centerY + barHeight),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawNeuralBrain(
    pulse: Float,
    flow: Float,
    active: Boolean,
    thinking: Boolean,
    speaking: Boolean
) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    val brainW = size.width * 0.82f
    val brainH = size.height * 0.70f
    val halfW = brainW / 2f
    val halfH = brainH / 2f

    val accent = when {
        thinking -> Color(0xFFAF7BFF)
        speaking -> Color(0xFF648FFF)
        active -> Color(0xFF56A6FF)
        else -> Color(0xFF46516D)
    }

    /* ---------------------------------------------------------
     * Outer orbital rings: subtle, not the main object.
     * --------------------------------------------------------- */
    for (i in 1..6) {
        val radius = halfW * (0.72f + i * 0.09f)
        drawCircle(
            color = Color(0xFF6570A1).copy(
                alpha = if (active) 0.09f else 0.035f
            ),
            radius = radius,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.dp.toPx()
            )
        )
    }

    /* ---------------------------------------------------------
     * Brain silhouette — two actual hemispheres.
     * --------------------------------------------------------- */
    val left = Path().apply {
        moveTo(cx, cy - halfH)
        cubicTo(
            cx - halfW * 0.20f, cy - halfH * 1.05f,
            cx - halfW * 0.48f, cy - halfH * 0.98f,
            cx - halfW * 0.58f, cy - halfH * 0.77f
        )
        cubicTo(
            cx - halfW * 0.88f, cy - halfH * 0.78f,
            cx - halfW * 0.99f, cy - halfH * 0.48f,
            cx - halfW * 0.82f, cy - halfH * 0.29f
        )
        cubicTo(
            cx - halfW * 1.00f, cy - halfH * 0.08f,
            cx - halfW * 0.99f, cy + halfH * 0.20f,
            cx - halfW * 0.79f, cy + halfH * 0.30f
        )
        cubicTo(
            cx - halfW * 0.88f, cy + halfH * 0.57f,
            cx - halfW * 0.65f, cy + halfH * 0.83f,
            cx - halfW * 0.38f, cy + halfH * 0.78f
        )
        cubicTo(
            cx - halfW * 0.20f, cy + halfH * 0.98f,
            cx - halfW * 0.08f, cy + halfH * 0.95f,
            cx, cy + halfH
        )
        lineTo(cx, cy - halfH)
        close()
    }

    val right = Path().apply {
        moveTo(cx, cy - halfH)
        cubicTo(
            cx + halfW * 0.20f, cy - halfH * 1.05f,
            cx + halfW * 0.48f, cy - halfH * 0.98f,
            cx + halfW * 0.58f, cy - halfH * 0.77f
        )
        cubicTo(
            cx + halfW * 0.88f, cy - halfH * 0.78f,
            cx + halfW * 0.99f, cy - halfH * 0.48f,
            cx + halfW * 0.82f, cy - halfH * 0.29f
        )
        cubicTo(
            cx + halfW * 1.00f, cy - halfH * 0.08f,
            cx + halfW * 0.99f, cy + halfH * 0.20f,
            cx + halfW * 0.79f, cy + halfH * 0.30f
        )
        cubicTo(
            cx + halfW * 0.88f, cy + halfH * 0.57f,
            cx + halfW * 0.65f, cy + halfH * 0.83f,
            cx + halfW * 0.38f, cy + halfH * 0.78f
        )
        cubicTo(
            cx + halfW * 0.20f, cy + halfH * 0.98f,
            cx + halfW * 0.08f, cy + halfH * 0.95f,
            cx, cy + halfH
        )
        lineTo(cx, cy - halfH)
        close()
    }

    val fillBrush = Brush.radialGradient(
        colors = listOf(
            accent.copy(alpha = if (active) 0.52f else 0.16f),
            Color(0xFF17366D).copy(alpha = if (active) 0.30f else 0.08f),
            Color(0xFF080A17).copy(alpha = 0.05f)
        ),
        center = Offset(cx, cy),
        radius = halfW * 1.12f
    )

    drawPath(
        path = left,
        brush = fillBrush
    )
    drawPath(
        path = right,
        brush = fillBrush
    )

    drawPath(
        path = left,
        color = accent.copy(alpha = if (active) 0.52f else 0.20f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4.dp.toPx())
    )
    drawPath(
        path = right,
        color = accent.copy(alpha = if (active) 0.52f else 0.20f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4.dp.toPx())
    )

    /* ---------------------------------------------------------
     * Cortical folds. These make it read as a brain, not a globe.
     * --------------------------------------------------------- */
    val foldColor = accent.copy(alpha = if (active) 0.48f else 0.16f)

    fun fold(
        side: Float,
        yFactor: Float,
        bend: Float,
        length: Float
    ) {
        val path = Path()
        val y = cy + yFactor * halfH
        val startX = cx + side * halfW * 0.15f
        val endX = cx + side * halfW * length

        path.moveTo(startX, y)
        path.cubicTo(
            cx + side * halfW * 0.34f,
            y - halfH * 0.11f * bend,
            cx + side * halfW * 0.48f,
            y + halfH * 0.12f * bend,
            endX,
            y + halfH * 0.02f
        )

        drawPath(
            path = path,
            color = foldColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.15.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }

    for (index in 0 until 7) {
        val yFactor = -0.75f + index * 0.24f
        fold(-1f, yFactor, 1f + index * 0.05f, 0.78f)
        fold(1f, yFactor + 0.01f, 1.1f + index * 0.04f, 0.78f)
    }

    // Short secondary folds.
    for (index in 0 until 10) {
        val yFactor = -0.86f + index * 0.18f
        fold(
            if (index % 2 == 0) -1f else 1f,
            yFactor,
            0.7f,
            0.59f
        )
    }

    /* ---------------------------------------------------------
     * Neural network nodes distributed through both hemispheres.
     * --------------------------------------------------------- */
    val nodes = mutableListOf<Offset>()
    val rows = 7
    val cols = 7

    for (side in listOf(-1f, 1f)) {
        for (row in 0 until rows) {
            val ny = -0.82f + row * 0.27f
            val rowNorm = kotlin.math.abs(ny)
            val widthAtRow =
                (1f - rowNorm * 0.42f) * halfW * 0.84f

            for (col in 0 until cols) {
                val xNorm = -1f + col * (2f / (cols - 1f))
                val x = cx + side * (0.13f * halfW + xNorm * widthAtRow)
                val y = cy + ny * halfH * 0.90f
                val jitterX = kotlin.math.sin(row * 1.7f + col * 0.95f) * 4.5f
                val jitterY = kotlin.math.cos(row * 1.2f + col * 0.68f) * 3.5f
                nodes += Offset(x + jitterX, y + jitterY)
            }
        }
    }

    // Network connections.
    for (i in nodes.indices) {
        val a = nodes[i]
        for (j in i + 1 until nodes.size) {
            val b = nodes[j]
            val dx = a.x - b.x
            val dy = a.y - b.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

            if (distance < 39f) {
                drawLine(
                    color = accent.copy(
                        alpha = if (active) {
                            (0.32f - distance / 170f).coerceAtLeast(0.035f)
                        } else {
                            0.055f
                        }
                    ),
                    start = a,
                    end = b,
                    strokeWidth = 0.75.dp.toPx()
                )
            }
        }
    }

    // Central inter-hemisphere links.
    for (row in 1..5) {
        val y = cy - halfH * 0.74f + row * halfH * 0.30f
        drawLine(
            color = accent.copy(alpha = if (active) 0.35f else 0.08f),
            start = Offset(cx - halfW * 0.11f, y),
            end = Offset(cx + halfW * 0.11f, y),
            strokeWidth = 0.8.dp.toPx()
        )
    }

    // Animated signal line through the brain.
    if (active) {
        val signal = (flow * nodes.size).toInt() % nodes.size
        val p = nodes[signal]
        drawCircle(
            color = accent.copy(alpha = 0.95f),
            radius = 2.7.dp.toPx(),
            center = p
        )

        val next = nodes[(signal + 8) % nodes.size]
        drawLine(
            color = accent.copy(alpha = 0.48f),
            start = p,
            end = next,
            strokeWidth = 1.1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Center fissure.
    val fissure = Path()
    fissure.moveTo(cx, cy - halfH * 0.96f)
    fissure.cubicTo(
        cx - halfW * 0.035f,
        cy - halfH * 0.53f,
        cx + halfW * 0.026f,
        cy - halfH * 0.08f,
        cx,
        cy + halfH * 0.15f
    )
    fissure.cubicTo(
        cx - halfW * 0.028f,
        cy + halfH * 0.52f,
        cx + halfW * 0.032f,
        cy + halfH * 0.78f,
        cx,
        cy + halfH * 0.98f
    )

    drawPath(
        path = fissure,
        color = Color.White.copy(alpha = if (active) 0.35f else 0.12f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
    )

    // Nodes.
    nodes.forEachIndexed { index, node ->
        val nodePulse =
            if (active) {
                1f + kotlin.math.sin(flow * 6.283f + index * 0.37f) * 0.25f
            } else {
                0.72f
            }

        drawCircle(
            color = accent.copy(alpha = if (active) 0.88f else 0.23f),
            radius = 1.7.dp.toPx() * nodePulse,
            center = node
        )
    }

    /* ---------------------------------------------------------
     * Center V core — deliberately small, not another giant orb.
     * --------------------------------------------------------- */
    drawCircle(
        color = accent.copy(alpha = if (active) 0.16f * pulse else 0.05f),
        radius = 27.dp.toPx() * pulse,
        center = Offset(cx, cy)
    )

    val v = Path().apply {
        moveTo(cx - 11.dp.toPx(), cy - 7.dp.toPx())
        lineTo(cx, cy + 8.dp.toPx())
        lineTo(cx + 11.dp.toPx(), cy - 7.dp.toPx())
    }

    drawPath(
        path = v,
        color = Color.White.copy(alpha = if (active) 0.86f else 0.30f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
    )

    // A few orbiting particles outside the brain, very subtle.
    if (active) {
        for (i in 0 until 8) {
            val angle = flow * 6.283f + i * 0.785f
            val x = cx + kotlin.math.cos(angle) * halfW * 1.08f
            val y = cy + kotlin.math.sin(angle) * halfH * 0.72f
            drawCircle(
                color = accent.copy(alpha = 0.55f),
                radius = 1.5.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
