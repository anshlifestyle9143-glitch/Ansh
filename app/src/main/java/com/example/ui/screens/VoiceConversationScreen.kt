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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.VisionCommandExecutor
import com.example.service.VisionIntentClassifier
import com.example.ui.viewmodel.VisionViewModel
import com.example.util.LiveSpeechRecognizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

    val scope = rememberCoroutineScope()

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
                // Partial text intentionally hidden.
            },

            onFinal = { text ->

                if (!active) {
                    return@start
                }

                listeningStarted = false

                val cleanText = text.trim()

                if (cleanText.isBlank()) {
                    callState = VoiceCallState.IDLE
                    return@start
                }

                scope.launch {

                    val intentType =
                        try {
                            VisionIntentClassifier()
                                .classify(cleanText)
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
                            .IntentType.ACTION -> {

                            callState =
                                VoiceCallState.SPEAKING

                            viewModel.ttsManager.speak(
                                "Ok Boss",
                                -System.currentTimeMillis()
                            )

                            val started =
                                withTimeoutOrNull(3000L) {

                                    viewModel
                                        .ttsManager
                                        .isSpeaking
                                        .first { it }

                                    true
                                } == true

                            if (started) {

                                withTimeoutOrNull(10000L) {

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
                                withTimeoutOrNull(3000L) {

                                    viewModel
                                        .ttsManager
                                        .isSpeaking
                                        .first { it }

                                    true
                                } == true

                            if (resultStarted) {

                                withTimeoutOrNull(10000L) {

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
                            .IntentType.CONVERSATION -> {

                            callState =
                                VoiceCallState.THINKING

                            viewModel.sendMessage(
                                overridePrompt = cleanText,
                                autoSpeak = true
                            )
                        }

                        VisionIntentClassifier
                            .IntentType.QUESTION -> {

                            callState =
                                VoiceCallState.THINKING

                            viewModel.sendMessage(
                                overridePrompt = cleanText,
                                autoSpeak = true
                            )
                        }

                        VisionIntentClassifier
                            .IntentType.SEARCH -> {

                            callState =
                                VoiceCallState.THINKING

                            viewModel.sendMessage(
                                overridePrompt = cleanText,
                                autoSpeak = true
                            )
                        }

                        VisionIntentClassifier
                            .IntentType.UNCLEAR -> {

                            callState =
                                VoiceCallState.SPEAKING

                            viewModel.ttsManager.speak(
                                "Boss, thoda clearly bataiye.",
                                -System.currentTimeMillis()
                            )

                            withTimeoutOrNull(10000L) {

                                viewModel
                                    .ttsManager
                                    .isSpeaking
                                    .first { !it }
                            }

                            if (!active) {
                                return@launch
                            }

                            delay(300L)

                            if (active) {
                                startListening()
                            }
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
                }
            },

            onError = {

                if (!active) {
                    return@start
                }

                listeningStarted = false
                callState = VoiceCallState.IDLE
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
                callState = VoiceCallState.NO_PERMISSION
            }
        }

    fun requestListening() {

        if (!active) return
        if (isGenerating || isSpeaking) return

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
            contract =
                ActivityResultContracts.RequestPermission()
        ) {}

    LaunchedEffect(Unit) {

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    /*
     * Initial voice greeting.
     * Uses native TtsManager.
     */
    LaunchedEffect(Unit) {

        callState =
            VoiceCallState.SPEAKING

        viewModel.ttsManager.speak(
            "Yes Boss",
            -System.currentTimeMillis()
        )

        val started =
            withTimeoutOrNull(3000L) {

                viewModel
                    .ttsManager
                    .isSpeaking
                    .first { it }

                true
            } == true

        if (started) {

            withTimeoutOrNull(10000L) {

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

    /*
     * AI generation state.
     */
    LaunchedEffect(isGenerating) {

        if (!active) {
            return@LaunchedEffect
        }

        if (isGenerating) {

            listeningStarted = false

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

    /*
     * Native TTS state.
     */
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

    NeuralVoiceScreen(
        state = callState,
        onExit = {

            active = false
            listeningStarted = false

            speechRecognizer.stop()
            speechRecognizer.destroy()

            onExit()
        },
        onMicClick = {
            requestListening()
        }
    )
}


/* ============================================================
 * MAIN NEURAL VOICE UI
 * ============================================================ */

@Composable
private fun NeuralVoiceScreen(
    state: VoiceCallState,
    onExit: () -> Unit,
    onMicClick: () -> Unit
) {

    val transition =
        rememberInfiniteTransition(
            label = "vision_neural_motion"
        )

    val flow by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 3200
                        ),
                    repeatMode =
                        RepeatMode.Restart
                ),
            label = "brain_flow"
        )

    val pulse by
        transition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 1100
                        ),
                    repeatMode =
                        RepeatMode.Reverse
                ),
            label = "brain_pulse"
        )

    val active =
        state != VoiceCallState.IDLE &&
            state != VoiceCallState.NO_PERMISSION

    // Big status word shown bottom-left, over the brain (e.g. "Thinking...")
    val statusWord =
        when (state) {

            VoiceCallState.LISTENING ->
                "Listening..."

            VoiceCallState.THINKING ->
                "Thinking..."

            VoiceCallState.SPEAKING ->
                "Responding..."

            VoiceCallState.IDLE ->
                "Vision ready"

            VoiceCallState.NO_PERMISSION ->
                "Mic access needed"
        }

    // Small description line under the status word.
    val statusDescription =
        when (state) {

            VoiceCallState.LISTENING ->
                "Your voice is being\ncaptured in real-time."

            VoiceCallState.THINKING ->
                "Your voice is being\nprocessed in real-time."

            VoiceCallState.SPEAKING ->
                "Vision is replying\nto you right now."

            VoiceCallState.IDLE ->
                "Tap the mic below\nto start talking."

            VoiceCallState.NO_PERMISSION ->
                "Allow microphone access\nto talk with Vision."
        }

    val activeStage =
        when (state) {

            VoiceCallState.LISTENING ->
                0

            VoiceCallState.THINKING ->
                1

            VoiceCallState.SPEAKING ->
                2

            else ->
                -1
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF020207),
                                Color(0xFF04040C),
                                Color(0xFF05040D),
                                Color(0xFF020207)
                            )
                    )
                )
    ) {

        /* ----------------------------------------------------
         * Very subtle ambient light
         * ---------------------------------------------------- */

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color(0xFF263D91)
                                        .copy(alpha = 0.10f),

                                    Color(0xFF351A68)
                                        .copy(alpha = 0.06f),

                                    Color.Transparent
                                ),
                            radius = 850f
                        )
                    )
        )

        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {

            /* ------------------------------------------------
             * HEADER ROW: close (left) + mic status (right)
             * ------------------------------------------------ */

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 6.dp,
                            end = 20.dp,
                            top = 10.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onExit,
                    modifier =
                        Modifier.size(44.dp)
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Close,
                        contentDescription =
                            "Close voice mode",
                        tint =
                            Color.White,
                        modifier =
                            Modifier.size(26.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.weight(1f)
                )

                Icon(
                    imageVector =
                        Icons.Default.Mic,
                    contentDescription =
                        "Microphone status",
                    tint =
                        if (
                            state ==
                                VoiceCallState.NO_PERMISSION
                        ) {
                            Color(0xFFFF5268)
                        } else {
                            Color(0xFF54E39B)
                        },
                    modifier =
                        Modifier.size(20.dp)
                )
            }

            /* ------------------------------------------------
             * LOGO (left) + UNDERSTAND/ANALYZE/RESPOND (right)
             * ------------------------------------------------ */

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 22.dp,
                            vertical = 6.dp
                        ),
                verticalAlignment =
                    Alignment.Top,
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                VisionLogoLockup()

                PipelineSidebar(
                    activeStage = activeStage
                )
            }

            /* ------------------------------------------------
             * BRAIN + status text, sharing the remaining space
             * ------------------------------------------------ */

            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
            ) {

                Canvas(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .size(300.dp)
                ) {

                    drawNeuralBrain(
                        flow = flow,
                        pulse = pulse,
                        active = active,
                        thinking =
                            state ==
                                VoiceCallState.THINKING,
                        speaking =
                            state ==
                                VoiceCallState.SPEAKING
                    )
                }

                Column(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(
                                start = 24.dp,
                                bottom = 4.dp,
                                end = 24.dp
                            )
                ) {

                    Text(
                        text = statusWord,
                        color =
                            Color(0xFFF2F1F7),
                        fontSize = 26.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(
                        text = statusDescription,
                        color =
                            when (state) {

                                VoiceCallState.THINKING ->
                                    Color(0xFFB2A8FF)

                                VoiceCallState.SPEAKING ->
                                    Color(0xFF9AB4FF)

                                else ->
                                    Color(0xFF8E9AD9)
                            },
                        fontSize = 12.5.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            /* ------------------------------------------------
             * WAVEFORM
             * ------------------------------------------------ */

            NeuralWaveform(
                active =
                    state ==
                        VoiceCallState.LISTENING ||
                    state ==
                        VoiceCallState.THINKING ||
                    state ==
                        VoiceCallState.SPEAKING,

                thinking =
                    state ==
                        VoiceCallState.THINKING,

                speaking =
                    state ==
                        VoiceCallState.SPEAKING,

                flow = flow,

                modifier =
                    Modifier.fillMaxWidth()
            )

            Spacer(
                modifier =
                    Modifier.height(10.dp)
            )

            PaginationDots(
                activeStage = activeStage,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
            )

            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )

            /* ------------------------------------------------
             * BOTTOM CONTROL
             * ------------------------------------------------ */

            VoiceBottomControl(
                state = state,
                onMicClick = onMicClick,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 18.dp
                        )
            )
        }
    }
}


/* ============================================================
 * LOGO LOCKUP  ("V" badge + VISION / BY ANSH YADAV)
 * ============================================================ */

@Composable
private fun VisionLogoLockup(
    modifier: Modifier = Modifier
) {

    Row(
        modifier = modifier,
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color(0xFF5C7FFF),
                                    Color(0xFF1B1C36)
                                )
                        )
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text = "V",
                color = Color.White,
                fontSize = 14.sp
            )
        }

        Spacer(
            modifier =
                Modifier.width(9.dp)
        )

        Column {

            Text(
                text = "VISION",
                color = Color.White,
                fontSize = 13.sp,
                letterSpacing = 3.sp
            )

            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )

            Text(
                text = "BY ANSH YADAV",
                color =
                    Color(0xFF70758B),
                fontSize = 7.sp,
                letterSpacing = 1.8.sp
            )
        }
    }
}


/* ============================================================
 * PIPELINE SIDEBAR (Understand / Analyze / Respond)
 * ============================================================ */

@Composable
private fun PipelineSidebar(
    activeStage: Int,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        horizontalAlignment =
            Alignment.End
    ) {

        SidebarItem(
            label = "Understand",
            active = activeStage == 0,
            passed = activeStage > 0
        )

        Spacer(
            modifier =
                Modifier.height(9.dp)
        )

        SidebarItem(
            label = "Analyze",
            active = activeStage == 1,
            passed = activeStage > 1
        )

        Spacer(
            modifier =
                Modifier.height(9.dp)
        )

        SidebarItem(
            label = "Respond",
            active = activeStage == 2,
            passed = false
        )
    }
}

@Composable
private fun SidebarItem(
    label: String,
    active: Boolean,
    passed: Boolean
) {

    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = label,
            color =
                when {

                    active ->
                        Color(0xFFE7E8F1)

                    passed ->
                        Color(0xFF6C7084)

                    else ->
                        Color(0xFF9296A6)
                },
            fontSize = 12.sp
        )

        Spacer(
            modifier =
                Modifier.width(8.dp)
        )

        Box(
            modifier =
                Modifier
                    .size(
                        if (active) {
                            9.dp
                        } else {
                            7.dp
                        }
                    )
                    .clip(CircleShape)
                    .background(
                        when {

                            active ->
                                Color(0xFF6D9FFF)

                            passed ->
                                Color(0xFF52668C)

                            else ->
                                Color(0xFF303340)
                        }
                    )
        )
    }
}


/* ============================================================
 * PAGINATION DOTS (decorative, mirrors the reference carousel)
 * ============================================================ */

@Composable
private fun PaginationDots(
    activeStage: Int,
    modifier: Modifier = Modifier
) {

    val dotCount = 4
    val highlighted =
        activeStage.coerceIn(0, dotCount - 1)

    Row(
        modifier = modifier,
        horizontalArrangement =
            Arrangement.spacedBy(6.dp)
    ) {

        for (i in 0 until dotCount) {

            Box(
                modifier =
                    Modifier
                        .size(
                            if (i == highlighted) {
                                6.dp
                            } else {
                                4.dp
                            }
                        )
                        .clip(CircleShape)
                        .background(
                            if (i == highlighted) {
                                Color(0xFF7B93E0)
                            } else {
                                Color(0xFF33364A)
                            }
                        )
            )
        }
    }
}


/* ============================================================
 * WAVEFORM
 * ============================================================ */

@Composable
private fun NeuralWaveform(
    active: Boolean,
    thinking: Boolean,
    speaking: Boolean,
    flow: Float,
    modifier: Modifier
) {

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 62.dp)
    ) {

        val bars = 41
        val slot = size.width / bars
        val centerY = size.height / 2f

        for (i in 0 until bars) {

            val normalized =
                i.toFloat() /
                    (bars - 1f)

            val envelope =
                (
                    1f -
                        abs(
                            normalized - 0.5f
                        ) * 1.8f
                    )
                    .coerceAtLeast(0f)

            val wave =
                sin(
                    flow * 6.283f +
                        i * 0.72f
                )

            val wave2 =
                sin(
                    flow * 12.566f -
                        i * 0.42f
                )

            val barHeight =
                when {

                    !active ->
                        2.5f

                    speaking ->
                        5f +
                            envelope * 20f +
                            abs(wave) * 4f

                    thinking ->
                        4f +
                            envelope * 14f +
                            abs(wave2) * 3f

                    else ->
                        4f +
                            envelope * 18f +
                            abs(wave) * 5f
                }

            val x =
                slot * i +
                    slot / 2f

            drawLine(
                color =
                    when {

                        speaking ->
                            Color(0xFF648EFF)
                                .copy(alpha = 0.75f)

                        thinking ->
                            Color(0xFFA475FF)
                                .copy(alpha = 0.72f)

                        active ->
                            Color(0xFF568FFF)
                                .copy(alpha = 0.72f)

                        else ->
                            Color(0xFF3D4356)
                                .copy(alpha = 0.42f)
                    },

                start =
                    Offset(
                        x,
                        centerY - barHeight
                    ),

                end =
                    Offset(
                        x,
                        centerY + barHeight
                    ),

                strokeWidth = 2f,

                cap =
                    StrokeCap.Round
            )
        }
    }
}


/* ============================================================
 * NEURAL BRAIN
 * ============================================================ */

private fun DrawScope.drawNeuralBrain(
    flow: Float,
    pulse: Float,
    active: Boolean,
    thinking: Boolean,
    speaking: Boolean
) {

    val cx =
        size.width / 2f

    val cy =
        size.height / 2f

    val brainWidth =
        size.width * 0.78f

    val brainHeight =
        size.height * 0.66f

    val halfW =
        brainWidth / 2f

    val halfH =
        brainHeight / 2f

    val accent =
        when {

            thinking ->
                Color(0xFFAF7BFF)

            speaking ->
                Color(0xFF6490FF)

            active ->
                Color(0xFF56A6FF)

            else ->
                Color(0xFF45516D)
        }

    /* --------------------------------------------------------
     * Ambient volumetric bloom (fakes a soft blur / glow)
     * -------------------------------------------------------- */

    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        accent.copy(
                            alpha =
                                if (active) {
                                    0.30f
                                } else {
                                    0.05f
                                }
                        ),
                        accent.copy(alpha = 0f)
                    ),
                center = Offset(cx, cy),
                radius = halfW * 1.35f
            ),
        radius = halfW * 1.35f,
        center = Offset(cx, cy)
    )

    /* --------------------------------------------------------
     * Outer neural field
     * -------------------------------------------------------- */

    for (i in 1..6) {

        val radius =
            halfW *
                (0.78f + i * 0.085f)

        drawCircle(
            color =
                Color(0xFF6874A8).copy(
                    alpha =
                        if (active) {
                            0.085f
                        } else {
                            0.025f
                        }
                ),

            radius = radius,

            center =
                Offset(
                    cx,
                    cy
                ),

            style =
                Stroke(
                    width = 1.dp.toPx()
                )
        )
    }

    /* --------------------------------------------------------
     * Left hemisphere
     * -------------------------------------------------------- */

    val leftBrain =
        Path().apply {

            moveTo(
                cx,
                cy - halfH
            )

            cubicTo(
                cx - halfW * 0.18f,
                cy - halfH * 1.04f,

                cx - halfW * 0.52f,
                cy - halfH * 0.98f,

                cx - halfW * 0.61f,
                cy - halfH * 0.72f
            )

            cubicTo(
                cx - halfW * 0.88f,
                cy - halfH * 0.78f,

                cx - halfW * 1.00f,
                cy - halfH * 0.50f,

                cx - halfW * 0.82f,
                cy - halfH * 0.27f
            )

            cubicTo(
                cx - halfW * 1.00f,
                cy - halfH * 0.05f,

                cx - halfW * 0.98f,
                cy + halfH * 0.21f,

                cx - halfW * 0.78f,
                cy + halfH * 0.31f
            )

            cubicTo(
                cx - halfW * 0.88f,
                cy + halfH * 0.58f,

                cx - halfW * 0.61f,
                cy + halfH * 0.85f,

                cx - halfW * 0.36f,
                cy + halfH * 0.77f
            )

            cubicTo(
                cx - halfW * 0.19f,
                cy + halfH * 0.97f,

                cx - halfW * 0.08f,
                cy + halfH * 0.98f,

                cx,
                cy + halfH
            )

            lineTo(
                cx,
                cy - halfH
            )

            close()
        }

    /* --------------------------------------------------------
     * Right hemisphere
     * -------------------------------------------------------- */

    val rightBrain =
        Path().apply {

            moveTo(
                cx,
                cy - halfH
            )

            cubicTo(
                cx + halfW * 0.18f,
                cy - halfH * 1.04f,

                cx + halfW * 0.52f,
                cy - halfH * 0.98f,

                cx + halfW * 0.61f,
                cy - halfH * 0.72f
            )

            cubicTo(
                cx + halfW * 0.88f,
                cy - halfH * 0.78f,

                cx + halfW * 1.00f,
                cy - halfH * 0.50f,

                cx + halfW * 0.82f,
                cy - halfH * 0.27f
            )

            cubicTo(
                cx + halfW * 1.00f,
                cy - halfH * 0.05f,

                cx + halfW * 0.98f,
                cy + halfH * 0.21f,

                cx + halfW * 0.78f,
                cy + halfH * 0.31f
            )

            cubicTo(
                cx + halfW * 0.88f,
                cy + halfH * 0.58f,

                cx + halfW * 0.61f,
                cy + halfH * 0.85f,

                cx + halfW * 0.36f,
                cy + halfH * 0.77f
            )

            cubicTo(
                cx + halfW * 0.19f,
                cy + halfH * 0.97f,

                cx + halfW * 0.08f,
                cy + halfH * 0.98f,

                cx,
                cy + halfH
            )

            lineTo(
                cx,
                cy - halfH
            )

            close()
        }

    val glowStrokeLayers =
        listOf(
            11.dp.toPx() to 0.05f,
            7.5.dp.toPx() to 0.09f,
            4.5.dp.toPx() to 0.15f
        )

    for ((glowWidth, glowAlpha) in glowStrokeLayers) {

        val layerAlpha =
            if (active) {
                glowAlpha
            } else {
                glowAlpha * 0.25f
            }

        drawPath(
            path = leftBrain,
            color = accent.copy(alpha = layerAlpha),
            style =
                Stroke(
                    width = glowWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
        )

        drawPath(
            path = rightBrain,
            color = accent.copy(alpha = layerAlpha),
            style =
                Stroke(
                    width = glowWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
        )
    }

    val brainBrush =
        Brush.radialGradient(
            colors =
                listOf(
                    accent.copy(
                        alpha =
                            if (active) {
                                0.46f
                            } else {
                                0.12f
                            }
                    ),

                    Color(0xFF173A78).copy(
                        alpha =
                            if (active) {
                                0.27f
                            } else {
                                0.06f
                            }
                    ),

                    Color.Transparent
                ),

            center =
                Offset(
                    cx,
                    cy
                ),

            radius =
                halfW * 1.15f
        )

    drawPath(
        path = leftBrain,
        brush = brainBrush
    )

    drawPath(
        path = rightBrain,
        brush = brainBrush
    )

    drawPath(
        path = leftBrain,
        color =
            accent.copy(
                alpha =
                    if (active) {
                        0.56f
                    } else {
                        0.17f
                    }
            ),
        style =
            Stroke(
                width = 1.5.dp.toPx()
            )
    )

    drawPath(
        path = rightBrain,
        color =
            accent.copy(
                alpha =
                    if (active) {
                        0.56f
                    } else {
                        0.17f
                    }
            ),
        style =
            Stroke(
                width = 1.5.dp.toPx()
            )
    )

    /* --------------------------------------------------------
     * Volumetric shading (adds a 3D, lit-sphere feel)
     * -------------------------------------------------------- */

    val lightAlpha =
        if (active) {
            0.16f
        } else {
            0.04f
        }

    val shadowAlpha =
        if (active) {
            0.22f
        } else {
            0.06f
        }

    for (hemisphere in listOf(leftBrain, rightBrain)) {

        clipPath(hemisphere) {

            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                Color.White.copy(alpha = lightAlpha),
                                Color.White.copy(alpha = 0f)
                            ),
                        center = Offset(cx - halfW * 0.30f, cy - halfH * 0.62f),
                        radius = halfW * 0.85f
                    ),
                radius = halfW * 0.85f,
                center = Offset(cx - halfW * 0.30f, cy - halfH * 0.62f)
            )

            drawCircle(
                brush =
                    Brush.radialGradient(
                        colors =
                            listOf(
                                Color.Black.copy(alpha = shadowAlpha),
                                Color.Black.copy(alpha = 0f)
                            ),
                        center = Offset(cx + halfW * 0.35f, cy + halfH * 0.55f),
                        radius = halfW * 0.95f
                    ),
                radius = halfW * 0.95f,
                center = Offset(cx + halfW * 0.35f, cy + halfH * 0.55f)
            )
        }
    }

    /* --------------------------------------------------------
     * Cortical folds
     * -------------------------------------------------------- */

    val foldColor =
        accent.copy(
            alpha =
                if (active) {
                    0.44f
                } else {
                    0.12f
                }
        )

    fun corticalFold(
        side: Float,
        yFactor: Float,
        bend: Float,
        length: Float
    ) {

        val path =
            Path()

        val y =
            cy +
                yFactor *
                    halfH

        val startX =
            cx +
                side *
                halfW *
                0.13f

        val endX =
            cx +
                side *
                halfW *
                length

        path.moveTo(
            startX,
            y
        )

        path.cubicTo(
            cx +
                side *
                halfW *
                0.34f,

            y -
                halfH *
                0.11f *
                bend,

            cx +
                side *
                halfW *
                0.51f,

            y +
                halfH *
                0.12f *
                bend,

            endX,

            y +
                halfH *
                0.02f
        )

        drawPath(
            path = path,
            color = foldColor,
            style =
                Stroke(
                    width = 1.1.dp.toPx(),
                    cap =
                        StrokeCap.Round
                )
        )
    }

    for (i in 0 until 7) {

        val y =
            -0.75f +
                i * 0.24f

        corticalFold(
            side = -1f,
            yFactor = y,
            bend = 1f + i * 0.05f,
            length = 0.78f
        )

        corticalFold(
            side = 1f,
            yFactor = y + 0.01f,
            bend = 1.05f + i * 0.05f,
            length = 0.78f
        )
    }

    for (i in 0 until 8) {

        corticalFold(
            side =
                if (i % 2 == 0) {
                    -1f
                } else {
                    1f
                },

            yFactor =
                -0.83f +
                    i * 0.21f,

            bend = 0.75f,

            length = 0.58f
        )
    }

    /* --------------------------------------------------------
     * Neural nodes
     * -------------------------------------------------------- */

    val nodes =
        mutableListOf<Offset>()

    val rows = 7
    val columns = 7

    for (side in listOf(-1f, 1f)) {

        for (row in 0 until rows) {

            val yNorm =
                -0.80f +
                    row * 0.265f

            val availableWidth =
                (
                    1f -
                        abs(yNorm) *
                        0.40f
                    ) *
                    halfW *
                    0.83f

            for (column in 0 until columns) {

                val xNorm =
                    -1f +
                        column *
                        (
                            2f /
                                (columns - 1f)
                        )

                val baseX =
                    cx +
                        side *
                        (
                            halfW * 0.13f +
                                xNorm *
                                availableWidth
                        )

                val baseY =
                    cy +
                        yNorm *
                        halfH *
                        0.91f

                val jitterX =
                    sin(
                        row * 1.65f +
                            column * 0.91f
                    ) *
                        3.8f

                val jitterY =
                    cos(
                        row * 1.15f +
                            column * 0.67f
                    ) *
                        3.0f

                nodes +=
                    Offset(
                        baseX + jitterX,
                        baseY + jitterY
                    )
            }
        }
    }

    /* --------------------------------------------------------
     * Connections
     * -------------------------------------------------------- */

    for (i in nodes.indices) {

        val first =
            nodes[i]

        for (j in i + 1 until nodes.size) {

            val second =
                nodes[j]

            val dx =
                first.x -
                    second.x

            val dy =
                first.y -
                    second.y

            val distance =
                sqrt(
                    dx * dx +
                        dy * dy
                )

            if (distance < 39f) {

                val alpha =
                    if (active) {

                        (
                            0.31f -
                                distance / 180f
                            )
                            .coerceAtLeast(
                                0.035f
                            )

                    } else {
                        0.045f
                    }

                drawLine(
                    color =
                        accent.copy(
                            alpha = alpha
                        ),

                    start = first,

                    end = second,

                    strokeWidth =
                        0.75.dp.toPx()
                )
            }
        }
    }

    /* --------------------------------------------------------
     * Cross hemisphere connections
     * -------------------------------------------------------- */

    for (i in 1..5) {

        val y =
            cy -
                halfH * 0.72f +
                i *
                halfH *
                0.30f

        drawLine(
            color =
                accent.copy(
                    alpha =
                        if (active) {
                            0.30f
                        } else {
                            0.07f
                        }
                ),

            start =
                Offset(
                    cx -
                        halfW *
                        0.10f,

                    y
                ),

            end =
                Offset(
                    cx +
                        halfW *
                        0.10f,

                    y
                ),

            strokeWidth =
                0.8.dp.toPx()
        )
    }

    /* --------------------------------------------------------
     * Animated signal
     * -------------------------------------------------------- */

    if (active && nodes.isNotEmpty()) {

        val signalIndex =
            (
                flow *
                    nodes.size
                )
                .toInt() %
                nodes.size

        val signal =
            nodes[signalIndex]

        val next =
            nodes[
                (signalIndex + 8) %
                    nodes.size
            ]

        drawLine(
            color =
                accent.copy(
                    alpha = 0.50f
                ),

            start = signal,

            end = next,

            strokeWidth =
                1.2.dp.toPx(),

            cap =
                StrokeCap.Round
        )

        drawCircle(
            color =
                accent.copy(
                    alpha = 0.95f
                ),

            radius =
                2.8.dp.toPx(),

            center = signal
        )
    }

    /* --------------------------------------------------------
     * Central brain fissure
     * -------------------------------------------------------- */

    val fissure =
        Path().apply {

            moveTo(
                cx,
                cy - halfH * 0.96f
            )

            cubicTo(
                cx - halfW * 0.035f,
                cy - halfH * 0.54f,

                cx + halfW * 0.025f,
                cy - halfH * 0.08f,

                cx,
                cy + halfH * 0.16f
            )

            cubicTo(
                cx - halfW * 0.025f,
                cy + halfH * 0.50f,

                cx + halfW * 0.03f,
                cy + halfH * 0.78f,

                cx,
                cy + halfH * 0.97f
            )
        }

    drawPath(
        path = fissure,
        color =
            Color.White.copy(
                alpha =
                    if (active) {
                        0.34f
                    } else {
                        0.10f
                    }
            ),
        style =
            Stroke(
                width = 1.2.dp.toPx()
            )
    )

    /* --------------------------------------------------------
     * Nodes
     * -------------------------------------------------------- */

    nodes.forEachIndexed { index, node ->

        val nodePulse =
            if (active) {

                1f +
                    sin(
                        flow * 6.283f +
                            index * 0.37f
                    ) *
                    0.25f

            } else {
                0.70f
            }

        drawCircle(
            color =
                accent.copy(
                    alpha =
                        if (active) {
                            0.88f
                        } else {
                            0.22f
                        }
                ),

            radius =
                1.7.dp.toPx() *
                    nodePulse,

            center = node
        )
    }

    /* --------------------------------------------------------
     * Small central AI core
     * -------------------------------------------------------- */

    drawCircle(
        color =
            accent.copy(
                alpha =
                    if (active) {
                        0.10f *
                            pulse
                    } else {
                        0.02f
                    }
            ),

        radius =
            48.dp.toPx() *
                pulse,

        center =
            Offset(
                cx,
                cy
            )
    )

    drawCircle(
        color =
            accent.copy(
                alpha =
                    if (active) {
                        0.26f *
                            pulse
                    } else {
                        0.06f
                    }
            ),

        radius =
            27.dp.toPx() *
                pulse,

        center =
            Offset(
                cx,
                cy
            )
    )

    /* --------------------------------------------------------
     * Small VISION mark
     * -------------------------------------------------------- */

    val v =
        Path().apply {

            moveTo(
                cx - 11.dp.toPx(),
                cy - 7.dp.toPx()
            )

            lineTo(
                cx,
                cy + 8.dp.toPx()
            )

            lineTo(
                cx + 11.dp.toPx(),
                cy - 7.dp.toPx()
            )
        }

    drawPath(
        path = v,
        color =
            Color.White.copy(
                alpha =
                    if (active) {
                        0.88f
                    } else {
                        0.28f
                    }
            ),
        style =
            Stroke(
                width = 1.8.dp.toPx(),
                cap =
                    StrokeCap.Round,
                join =
                    StrokeJoin.Round
            )
    )

    /* --------------------------------------------------------
     * Very subtle orbiting particles
     * -------------------------------------------------------- */

    if (active) {

        for (i in 0 until 8) {

            val angle =
                flow * 6.283f +
                    i * 0.785f

            val x =
                cx +
                    cos(angle) *
                    halfW *
                    1.07f

            val y =
                cy +
                    sin(angle) *
                    halfH *
                    0.72f

            drawCircle(
                color =
                    accent.copy(
                        alpha = 0.52f
                    ),

                radius =
                    1.4.dp.toPx(),

                center =
                    Offset(
                        x,
                        y
                    )
            )
        }
    }
}


/* ============================================================
 * BOTTOM VOICE CONTROL
 * ============================================================ */

@Composable
private fun VoiceBottomControl(
    state: VoiceCallState,
    onMicClick: () -> Unit,
    modifier: Modifier
) {

    Surface(
        modifier =
            modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(
                28.dp
            ),

        color =
            Color(0xFF090A11)
                .copy(alpha = 0.97f),

        border =
            BorderStroke(
                width = 1.dp,
                color =
                    Color(0xFF292C40)
            ),

        shadowElevation =
            12.dp
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 10.dp,
                        end = 10.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /* ------------------------------------------------
             * Small mic glyph on the LEFT, matching reference.
             * ------------------------------------------------ */

            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            Color(0xFF1A1C29)
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Mic,
                    contentDescription = null,
                    tint =
                        Color(0xFFC7CBDA),
                    modifier =
                        Modifier.size(18.dp)
                )
            }

            Spacer(
                modifier =
                    Modifier.width(12.dp)
            )

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Text(
                    text =
                        when (state) {

                            VoiceCallState.LISTENING ->
                                "Tap to stop"

                            VoiceCallState.THINKING ->
                                "Processing your voice"

                            VoiceCallState.SPEAKING ->
                                "Vision is speaking"

                            VoiceCallState.IDLE ->
                                "Tap to start"

                            VoiceCallState.NO_PERMISSION ->
                                "Microphone permission required"
                        },

                    color = Color.White,

                    fontSize = 14.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )

                Text(
                    text =
                        when (state) {

                            VoiceCallState.LISTENING ->
                                "Listening in real-time"

                            VoiceCallState.THINKING ->
                                "Your voice is being processed"

                            VoiceCallState.SPEAKING ->
                                "Reply in progress"

                            VoiceCallState.IDLE ->
                                "Ready to listen"

                            VoiceCallState.NO_PERMISSION ->
                                "Grant access to continue"
                        },

                    color =
                        Color(0xFF7D8298),

                    fontSize = 10.sp
                )
            }

            /* ------------------------------------------------
             * Mic / Stop control stays on RIGHT.
             * ------------------------------------------------ */

            Box(
                modifier =
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        Color(0xFF5C7FFF),
                                        Color(0xFF1B1C36)
                                    )
                            )
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                if (
                    state ==
                        VoiceCallState.IDLE ||
                    state ==
                        VoiceCallState.NO_PERMISSION
                ) {

                    IconButton(
                        onClick = onMicClick,
                        modifier =
                            Modifier.size(46.dp)
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Mic,

                            contentDescription =
                                "Start listening",

                            tint =
                                Color.White,

                            modifier =
                                Modifier.size(21.dp)
                        )
                    }

                } else {

                    /*
                     * Speaking/listening/processing:
                     * compact square stop indicator.
                     * No extra floating black button.
                     */
                    Box(
                        modifier =
                            Modifier
                                .size(13.dp)
                                .clip(
                                    RoundedCornerShape(
                                        3.dp
                                    )
                                )
                                .background(
                                    Color.White
                                )
                    )
                }
            }
        }
    }
}
