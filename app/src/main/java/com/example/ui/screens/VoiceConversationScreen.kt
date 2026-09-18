package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.cos
import kotlin.math.sin

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
                // Partial speech intentionally hidden.
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
                            .IntentType
                            .ACTION -> {

                            callState =
                                VoiceCallState.SPEAKING

                            viewModel.ttsManager.speak(
                                "Ok Boss",
                                -System.currentTimeMillis()
                            )

                            val okBossStarted =
                                withTimeoutOrNull(3000L) {

                                    viewModel
                                        .ttsManager
                                        .isSpeaking
                                        .first { it }

                                    true

                                } == true

                            if (okBossStarted) {

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

    LaunchedEffect(Unit) {

        callState =
            VoiceCallState.SPEAKING

        viewModel.ttsManager.speak(
            "Yes Boss",
            -System.currentTimeMillis()
        )

        val yesBossStarted =
            withTimeoutOrNull(3000L) {

                viewModel
                    .ttsManager
                    .isSpeaking
                    .first { it }

                true

            } == true

        if (yesBossStarted) {

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

    NeuralVoiceInterface(
        callState = callState,
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
 * NEURAL VOICE INTERFACE
 * ============================================================ */

@Composable
private fun NeuralVoiceInterface(
    callState: VoiceCallState,
    onExit: () -> Unit,
    onMicClick: () -> Unit
) {

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "neural_interface"
        )

    val flow by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 4200
                        ),
                    repeatMode =
                        RepeatMode.Restart
                ),
            label = "neural_flow"
        )

    val pulse by
        infiniteTransition.animateFloat(
            initialValue = 0.90f,
            targetValue = 1.10f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 1100
                        ),
                    repeatMode =
                        RepeatMode.Reverse
                ),
            label = "neural_pulse"
        )

    val brainScale by
        animateFloatAsState(
            targetValue =
                when (callState) {

                    VoiceCallState.LISTENING ->
                        1.02f

                    VoiceCallState.THINKING ->
                        1.07f

                    VoiceCallState.SPEAKING ->
                        1.04f

                    else ->
                        1f
                },
            animationSpec =
                tween(450),
            label = "brain_scale"
        )

    val backgroundBrush =
        Brush.verticalGradient(
            colors =
                listOf(
                    Color(0xFF03040A),
                    Color(0xFF070510),
                    Color(0xFF05030B),
                    Color(0xFF020207)
                )
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(backgroundBrush)
    ) {

        /* ---------------------------------------------
         * Ambient glow behind brain
         * --------------------------------------------- */

        Box(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .size(330.dp)
                    .graphicsLayer {

                        scaleX =
                            brainScale * pulse

                        scaleY =
                            brainScale * pulse

                        alpha = 0.24f
                    }
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color(0xFF6845FF),
                                    Color(0xFF326DFF).copy(
                                        alpha = 0.35f
                                    ),
                                    Color.Transparent
                                )
                        ),
                        CircleShape
                    )
        )

        /* ---------------------------------------------
         * TOP HEADER
         * --------------------------------------------- */

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        end = 18.dp,
                        top = 18.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onExit,
                modifier =
                    Modifier.size(46.dp)
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Close,
                    contentDescription =
                        "Close voice mode",
                    tint =
                        VisionTextPrimary
                )
            }

            Column(
                modifier =
                    Modifier.weight(1f),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = "VISION",
                    color =
                        VisionTextPrimary,
                    fontSize = 15.sp,
                    letterSpacing = 4.sp
                )

                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )

                Text(
                    text = "BY ANSH YADAV",
                    color =
                        VisionTextSecondary,
                    fontSize = 8.sp,
                    letterSpacing = 2.sp
                )
            }

            StatusDot(
                active =
                    callState !=
                        VoiceCallState.IDLE &&
                    callState !=
                        VoiceCallState.NO_PERMISSION
            )
        }

        /* ---------------------------------------------
         * STATUS TITLE
         * --------------------------------------------- */

        Column(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 92.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text =
                    when (callState) {

                        VoiceCallState.LISTENING ->
                            "LISTENING"

                        VoiceCallState.THINKING ->
                            "THINKING"

                        VoiceCallState.SPEAKING ->
                            "RESPONDING"

                        VoiceCallState.IDLE ->
                            "VISION READY"

                        VoiceCallState.NO_PERMISSION ->
                            "MICROPHONE ACCESS"
                    },
                color =
                    when (callState) {

                        VoiceCallState.LISTENING ->
                            Color(0xFF61A7FF)

                        VoiceCallState.THINKING ->
                            Color(0xFFB084FF)

                        VoiceCallState.SPEAKING ->
                            Color(0xFF6C8DFF)

                        else ->
                            VisionTextSecondary
                    },
                fontSize = 11.sp,
                letterSpacing = 3.sp
            )

            Spacer(
                modifier =
                    Modifier.height(7.dp)
            )

            Text(
                text =
                    when (callState) {

                        VoiceCallState.LISTENING ->
                            "Sun rahi hoon..."

                        VoiceCallState.THINKING ->
                            "Thinking..."

                        VoiceCallState.SPEAKING ->
                            "Bol rahi hoon..."

                        VoiceCallState.IDLE ->
                            "Ready"

                        VoiceCallState.NO_PERMISSION ->
                            "Microphone permission required"
                    },
                color =
                    VisionTextPrimary,
                fontSize = 20.sp
            )
        }

        /* ---------------------------------------------
         * NEURAL BRAIN
         * --------------------------------------------- */

        Box(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(
                        bottom = 80.dp
                    )
                    .size(330.dp)
        ) {

            NeuralBrainVisualizer(
                modifier =
                    Modifier.fillMaxSize(),
                active =
                    callState !=
                        VoiceCallState.IDLE &&
                    callState !=
                        VoiceCallState.NO_PERMISSION,
                thinking =
                    callState ==
                        VoiceCallState.THINKING,
                speaking =
                    callState ==
                        VoiceCallState.SPEAKING,
                flow = flow,
                pulse = pulse
            )
        }

        /* ---------------------------------------------
         * PROCESSING STAGES
         * --------------------------------------------- */

        ProcessingStages(
            callState = callState,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(
                        top = 375.dp
                    )
        )

        /* ---------------------------------------------
         * LIVE WAVEFORM
         * --------------------------------------------- */

        NeuralWaveform(
            active =
                callState ==
                    VoiceCallState.LISTENING ||
                callState ==
                    VoiceCallState.SPEAKING,
            thinking =
                callState ==
                    VoiceCallState.THINKING,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 158.dp)
        )

        /* ---------------------------------------------
         * BOTTOM CONTROL
         * --------------------------------------------- */

        VoiceControlBar(
            callState = callState,
            onMicClick = onMicClick,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 22.dp
                    )
        )
    }
}


/* ============================================================
 * STATUS DOT
 * ============================================================ */

@Composable
private fun StatusDot(
    active: Boolean
) {

    val transition =
        rememberInfiniteTransition(
            label = "status_dot"
        )

    val alpha by
        transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(700),
                    repeatMode =
                        RepeatMode.Reverse
                ),
            label = "status_alpha"
        )

    Box(
        modifier =
            Modifier
                .size(9.dp)
                .graphicsLayer {
                    this.alpha =
                        if (active) alpha else 0.35f
                }
                .clip(CircleShape)
                .background(
                    if (active)
                        Color(0xFF55E69B)
                    else
                        Color(0xFF6C6C78)
                )
    )
}


/* ============================================================
 * NEURAL BRAIN VISUALIZER
 * ============================================================ */

@Composable
private fun NeuralBrainVisualizer(
    modifier: Modifier,
    active: Boolean,
    thinking: Boolean,
    speaking: Boolean,
    flow: Float,
    pulse: Float
) {

    Canvas(
        modifier = modifier
    ) {

        val center =
            Offset(
                x = size.width / 2f,
                y = size.height / 2f
            )

        val baseRadius =
            size.minDimension * 0.27f

        val nodeColor =
            when {

                thinking ->
                    Color(0xFFB875FF)

                speaking ->
                    Color(0xFF5D8DFF)

                active ->
                    Color(0xFF4CBBFF)

                else ->
                    Color(0xFF526080)
            }

        /* -----------------------------------------
         * Outer orbital rings
         * ----------------------------------------- */

        for (i in 1..4) {

            val radius =
                baseRadius +
                    i * 17f

            drawCircle(
                color =
                    nodeColor.copy(
                        alpha =
                            if (active)
                                0.08f
                            else
                                0.035f
                    ),
                radius = radius,
                center = center,
                style =
                    Stroke(
                        width = 1.2f
                    )
            )
        }

        /* -----------------------------------------
         * Neural nodes
         * ----------------------------------------- */

        val rings =
            6

        val pointsPerRing =
            16

        val nodes =
            mutableListOf<Offset>()

        for (ring in 0 until rings) {

            val ringRadius =
                baseRadius *
                    (0.30f + ring * 0.14f)

            for (index in 0 until pointsPerRing) {

                val angle =
                    (index.toFloat() /
                        pointsPerRing) *
                        Math.PI.toFloat() *
                        2f

                val distortion =
                    sin(
                        angle * 3f +
                            flow * 6.283f
                    ) * 4f

                val x =
                    center.x +
                        cos(angle) *
                            (ringRadius + distortion)

                val y =
                    center.y +
                        sin(angle) *
                            (ringRadius + distortion) *
                            0.72f

                nodes.add(
                    Offset(x, y)
                )
            }
        }

        /* -----------------------------------------
         * Neural connections
         * ----------------------------------------- */

        for (i in nodes.indices) {

            val point =
                nodes[i]

            for (j in i + 1 until nodes.size) {

                val other =
                    nodes[j]

                val dx =
                    point.x - other.x

                val dy =
                    point.y - other.y

                val distance =
                    kotlin.math.sqrt(
                        dx * dx +
                            dy * dy
                    )

                if (distance < 48f) {

                    val connectionAlpha =
                        if (active)
                            (1f -
                                distance / 48f) *
                                0.42f
                        else
                            (1f -
                                distance / 48f) *
                                0.12f

                    drawLine(
                        color =
                            nodeColor.copy(
                                alpha =
                                    connectionAlpha
                            ),
                        start = point,
                        end = other,
                        strokeWidth =
                            if (thinking)
                                1.7f
                            else
                                1.15f
                    )
                }
            }
        }

        /* -----------------------------------------
         * Brain contour
         * ----------------------------------------- */

        val brainPath =
            Path()

        val brainWidth =
            baseRadius * 1.75f

        val brainHeight =
            baseRadius * 1.32f

        val left =
            center.x - brainWidth / 2f

        val right =
            center.x + brainWidth / 2f

        val top =
            center.y - brainHeight / 2f

        val bottom =
            center.y + brainHeight / 2f

        brainPath.moveTo(
            center.x,
            top
        )

        brainPath.cubicTo(
            right * 0.94f,
            top,
            right,
            center.y - brainHeight * 0.25f,
            right,
            center.y
        )

        brainPath.cubicTo(
            right,
            bottom * 0.98f,
            center.x + brainWidth * 0.17f,
            bottom,
            center.x,
            bottom
        )

        brainPath.cubicTo(
            center.x - brainWidth * 0.17f,
            bottom,
            left,
            bottom * 0.98f,
            left,
            center.y
        )

        brainPath.cubicTo(
            left,
            center.y - brainHeight * 0.25f,
            center.x - brainWidth * 0.18f,
            top,
            center.x,
            top
        )

        drawPath(
            path = brainPath,
            color =
                nodeColor.copy(
                    alpha =
                        if (active)
                            0.65f
                        else
                            0.22f
                ),
            style =
                Stroke(
                    width = 2f,
                    join = StrokeJoin.Round
                )
        )

        /* -----------------------------------------
         * Brain center glow
         * ----------------------------------------- */

        drawCircle(
            brush =
                Brush.radialGradient(
                    colors =
                        listOf(
                            nodeColor.copy(
                                alpha =
                                    if (active)
                                        0.75f
                                    else
                                        0.22f
                            ),
                            nodeColor.copy(
                                alpha = 0.16f
                            ),
                            Color.Transparent
                        )
                ),
            radius =
                baseRadius * 0.72f,
            center = center
        )

        /* -----------------------------------------
         * Animated energy particles
         * ----------------------------------------- */

        if (active) {

            for (i in 0 until 12) {

                val progress =
                    (
                        flow +
                            i.toFloat() / 12f
                    ) % 1f

                val angle =
                    i *
                        0.87f +
                        flow * 6.28f

                val radius =
                    baseRadius *
                        (0.30f +
                            progress * 1.15f)

                val particle =
                    Offset(
                        x =
                            center.x +
                                cos(angle) *
                                    radius,
                        y =
                            center.y +
                                sin(angle) *
                                    radius *
                                    0.72f
                    )

                drawCircle(
                    color =
                        nodeColor.copy(
                            alpha =
                                0.75f -
                                    progress * 0.45f
                        ),
                    radius =
                        if (thinking)
                            3.3f
                        else
                            2.5f,
                    center = particle
                )
            }
        }

        /* -----------------------------------------
         * Main nodes
         * ----------------------------------------- */

        nodes.forEachIndexed { index, node ->

            val nodePulse =
                if (active) {

                    1f +
                        sin(
                            flow * 6.28f +
                                index * 0.55f
                        ) * 0.32f

                } else {
                    0.7f
                }

            drawCircle(
                color =
                    nodeColor.copy(
                        alpha =
                            if (active)
                                0.85f
                            else
                                0.28f
                    ),
                radius =
                    2.2f * nodePulse,
                center = node
            )
        }

        /* -----------------------------------------
         * Central VISION core
         * ----------------------------------------- */

        drawCircle(
            color =
                nodeColor.copy(
                    alpha =
                        if (active)
                            0.90f
                        else
                            0.30f
                ),
            radius =
                29f * pulse,
            center = center,
            style =
                Stroke(
                    width = 1.6f
                )
        )

        drawCircle(
            color =
                nodeColor.copy(
                    alpha =
                        if (active)
                            0.22f
                        else
                            0.08f
                ),
            radius =
                42f * pulse,
            center = center,
            style =
                Stroke(
                    width = 1f
                )
        )

        /* -----------------------------------------
         * Central V
         * ----------------------------------------- */

        val vPath =
            Path()

        val vWidth =
            25f

        val vTop =
            center.y - 13f

        val vBottom =
            center.y + 13f

        vPath.moveTo(
            center.x - vWidth,
            vTop
        )

        vPath.lineTo(
            center.x,
            vBottom
        )

        vPath.lineTo(
            center.x + vWidth,
            vTop
        )

        drawPath(
            path = vPath,
            color =
                Color.White.copy(
                    alpha =
                        if (active)
                            0.90f
                        else
                            0.35f
                ),
            style =
                Stroke(
                    width = 3f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
        )
    }
}


/* ============================================================
 * PROCESSING STAGES
 * ============================================================ */

@Composable
private fun ProcessingStages(
    callState: VoiceCallState,
    modifier: Modifier
) {

    val activeStage =
        when (callState) {

            VoiceCallState.LISTENING ->
                0

            VoiceCallState.THINKING ->
                1

            VoiceCallState.SPEAKING ->
                2

            else ->
                -1
        }

    Row(
        modifier = modifier,
        horizontalArrangement =
            Arrangement.Center,
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        ProcessingStage(
            title = "UNDERSTAND",
            active =
                activeStage == 0,
            completed =
                activeStage > 0
        )

        StageLine(
            active =
                activeStage >= 1
        )

        ProcessingStage(
            title = "ANALYZE",
            active =
                activeStage == 1,
            completed =
                activeStage > 1
        )

        StageLine(
            active =
                activeStage >= 2
        )

        ProcessingStage(
            title = "RESPOND",
            active =
                activeStage == 2,
            completed = false
        )
    }
}

@Composable
private fun ProcessingStage(
    title: String,
    active: Boolean,
    completed: Boolean
) {

    val dotScale by
        animateFloatAsState(
            targetValue =
                if (active) 1.3f else 1f,
            animationSpec =
                tween(350),
            label = "stage_dot"
        )

    Column(
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        (7 * dotScale).dp
                    )
                    .clip(CircleShape)
                    .background(
                        when {

                            active ->
                                Color(0xFF73A7FF)

                            completed ->
                                Color(0xFF536B91)

                            else ->
                                Color(0xFF252837)
                        }
                    )
        )

        Spacer(
            modifier =
                Modifier.height(5.dp)
        )

        Text(
            text = title,
            color =
                if (active)
                    VisionTextPrimary
                else
                    VisionTextSecondary.copy(
                        alpha = 0.55f
                    ),
            fontSize = 7.sp,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun StageLine(
    active: Boolean
) {

    Box(
        modifier =
            Modifier
                .padding(
                    horizontal = 7.dp
                )
                .width(27.dp)
                .height(1.dp)
                .background(
                    if (active)
                        Color(0xFF536B91)
                    else
                        Color(0xFF202331)
                )
    )
}


/* ============================================================
 * LIVE NEURAL WAVEFORM
 * ============================================================ */

@Composable
private fun NeuralWaveform(
    active: Boolean,
    thinking: Boolean,
    modifier: Modifier
) {

    val transition =
        rememberInfiniteTransition(
            label = "waveform"
        )

    val movement by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 1200
                        ),
                    repeatMode =
                        RepeatMode.Restart
                ),
            label = "wave_movement"
        )

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(42.dp)
                .padding(horizontal = 65.dp)
    ) {

        val bars = 38

        val gap =
            size.width / bars

        for (i in 0 until bars) {

            val normalized =
                i.toFloat() /
                    (bars - 1)

            val distanceFromCenter =
                kotlin.math.abs(
                    normalized - 0.5f
                )

            val envelope =
                1f -
                    distanceFromCenter * 1.7f

            val wave =
                sin(
                    movement * 6.283f +
                        i * 0.62f
                )

            val heightMultiplier =
                if (active || thinking)
                    0.35f +
                        kotlin.math.abs(wave) *
                            envelope *
                            1.8f
                else
                    0.12f

            val barHeight =
                (size.height * 0.18f) *
                    heightMultiplier

            val x =
                gap * i +
                    gap / 2f

            drawLine(
                color =
                    when {

                        thinking ->
                            Color(0xFFB477FF)
                                .copy(
                                    alpha =
                                        0.45f +
                                            envelope * 0.35f
                                )

                        active ->
                            Color(0xFF609DFF)
                                .copy(
                                    alpha =
                                        0.40f +
                                            envelope * 0.45f
                                )

                        else ->
                            Color(0xFF56627B)
                                .copy(
                                    alpha = 0.25f
                                )
                    },
                start =
                    Offset(
                        x,
                        size.height / 2f -
                            barHeight
                    ),
                end =
                    Offset(
                        x,
                        size.height / 2f +
                            barHeight
                    ),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
        }
    }
}


/* ============================================================
 * BOTTOM VOICE CONTROL
 * ============================================================ */

@Composable
private fun VoiceControlBar(
    callState: VoiceCallState,
    onMicClick: () -> Unit,
    modifier: Modifier
) {

    Surface(
        modifier =
            modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(
                27.dp
            ),
        color =
            Color(0xFF0B0B15).copy(
                alpha = 0.96f
            ),
        tonalElevation = 4.dp,
        shadowElevation = 16.dp
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 15.dp
                    )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    modifier =
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            Color(0xFF4D78FF),
                                            Color(0xFF241E49)
                                        )
                                )
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(
                        imageVector =
                            when (callState) {

                                VoiceCallState.SPEAKING ->
                                    Icons.Default.Stop

                                else ->
                                    Icons.Default.Mic
                            },
                        contentDescription =
                            "Voice control",
                        tint =
                            Color.White,
                        modifier =
                            Modifier.size(20.dp)
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(13.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text = "VISION",
                        color =
                            VisionTextPrimary,
                        fontSize = 15.sp,
                        letterSpacing = 1.5.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(2.dp)
                    )

                    Text(
                        text =
                            when (callState) {

                                VoiceCallState.LISTENING ->
                                    "Listening in real-time"

                                VoiceCallState.THINKING ->
                                    "Processing your voice"

                                VoiceCallState.SPEAKING ->
                                    "Vision is speaking"

                                VoiceCallState.IDLE ->
                                    "Ready to listen"

                                VoiceCallState.NO_PERMISSION ->
                                    "Microphone permission required"
                            },
                        color =
                            VisionTextSecondary,
                        fontSize = 11.sp
                    )
                }

                if (
                    callState ==
                        VoiceCallState.IDLE ||
                    callState ==
                        VoiceCallState.NO_PERMISSION
                ) {

                    IconButton(
                        onClick = onMicClick
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Mic,
                            contentDescription =
                                "Start listening",
                            tint =
                                Color.White
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(9.dp)
            )

            Text(
                text =
                    when (callState) {

                        VoiceCallState.LISTENING ->
                            "I'm listening for your command"

                        VoiceCallState.THINKING ->
                            "Understanding • analyzing • preparing response"

                        VoiceCallState.SPEAKING ->
                            "Vision is responding..."

                        VoiceCallState.IDLE ->
                            "Tap the microphone to speak"

                        VoiceCallState.NO_PERMISSION ->
                            "Allow microphone access to continue"
                    },
                color =
                    VisionTextSecondary.copy(
                        alpha = 0.78f
                    ),
                fontSize = 10.sp
            )
        }
    }
}
