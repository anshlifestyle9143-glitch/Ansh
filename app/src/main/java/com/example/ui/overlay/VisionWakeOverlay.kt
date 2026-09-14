package com.example.ui.overlay

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.VisionEmerald
import com.example.ui.theme.VisionIndigo
import com.example.ui.theme.VisionTextPrimary
import com.example.ui.viewmodel.VisionViewModel
import com.example.util.LiveSpeechRecognizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private enum class WakeOverlayState {
    AWAKENING,
    LISTENING,
    THINKING,
    SPEAKING,
    IDLE
}

@Composable
fun VisionWakeOverlay(
    context: Context,
    viewModel: VisionViewModel,
    onDismiss: () -> Unit
) {
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isSpeaking by viewModel.ttsManager.isSpeaking.collectAsState()

    var state by remember {
        mutableStateOf(WakeOverlayState.AWAKENING)
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
        if (
            !active ||
            listeningStarted ||
            isGenerating ||
            isSpeaking
        ) {
            return
        }

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            state = WakeOverlayState.IDLE
            return
        }

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

                if (text.isBlank()) {
                    state = WakeOverlayState.IDLE
                    return@start
                }

                state = WakeOverlayState.SPEAKING

                scope.launch {

                    viewModel.ttsManager.speak(
                        "Ok Boss",
                        -System.currentTimeMillis()
                    )

                    val started =
                        withTimeoutOrNull(3000L) {
                            viewModel.ttsManager
                                .isSpeaking
                                .first { it }

                            true
                        } == true

                    if (started) {

                        withTimeoutOrNull(10000L) {

                            viewModel.ttsManager
                                .isSpeaking
                                .first { !it }
                        }
                    }

                    if (!active) {
                        return@launch
                    }

                    state =
                        WakeOverlayState.THINKING

                    viewModel.sendMessage(
                        overridePrompt = text,
                        autoSpeak = true
                    )
                }
            },

            onListeningChange = { listening ->

                if (!active) {
                    return@start
                }

                if (listening) {

                    listeningStarted = true

                    state =
                        WakeOverlayState.LISTENING
                }
            },

            onError = {

                if (!active) {
                    return@start
                }

                listeningStarted = false

                state =
                    WakeOverlayState.IDLE
            }
        )
    }

    DisposableEffect(Unit) {

        onDispose {

            active = false
            listeningStarted = false

            speechRecognizer.stop()
            speechRecognizer.destroy()
        }
    }

    /*
     * ---------------------------------------------------------
     * WAKE WORD → YES BOSS → LISTENING
     * ---------------------------------------------------------
     */
    LaunchedEffect(Unit) {

        state =
            WakeOverlayState.AWAKENING

        viewModel.ttsManager.speak(
            "Yes Boss",
            -System.currentTimeMillis()
        )

        val started =
            withTimeoutOrNull(3000L) {

                viewModel.ttsManager
                    .isSpeaking
                    .first { it }

                true
            } == true

        if (started) {

            withTimeoutOrNull(10000L) {

                viewModel.ttsManager
                    .isSpeaking
                    .first { !it }
            }
        }

        delay(400L)

        if (active) {
            startListening()
        }
    }

    /*
     * ---------------------------------------------------------
     * AI PROCESSING
     * ---------------------------------------------------------
     */
    LaunchedEffect(isGenerating) {

        if (!active) {
            return@LaunchedEffect
        }

        if (isGenerating) {

            listeningStarted = false

            state =
                WakeOverlayState.THINKING

            speechRecognizer.stop()
        }
    }

    /*
     * ---------------------------------------------------------
     * TTS STATE
     * ---------------------------------------------------------
     */
    LaunchedEffect(isSpeaking) {

        if (!active) {
            return@LaunchedEffect
        }

        if (
            isSpeaking &&
            !isGenerating
        ) {

            state =
                WakeOverlayState.SPEAKING
        }
    }

    /*
     * ---------------------------------------------------------
     * AFTER RESPONSE → LISTEN AGAIN
     * ---------------------------------------------------------
     */
    LaunchedEffect(
        isGenerating,
        isSpeaking
    ) {

        if (!active) {
            return@LaunchedEffect
        }

        if (
            !isGenerating &&
            !isSpeaking &&
            state == WakeOverlayState.THINKING
        ) {

            delay(300L)

            if (active) {
                startListening()
            }
        }
    }

    /*
     * ---------------------------------------------------------
     * HOLOGRAPHIC ANIMATION
     * ---------------------------------------------------------
     */
    val infinite =
        rememberInfiniteTransition(
            label = "vision_holo"
        )

    val ring1 by
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,

            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(5200),

                    repeatMode =
                        RepeatMode.Restart
                ),

            label = "ring1"
        )

    val ring2 by
        infinite.animateFloat(
            initialValue = 360f,
            targetValue = 0f,

            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(7200),

                    repeatMode =
                        RepeatMode.Restart
                ),

            label = "ring2"
        )

    val ring3 by
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = 360f,

            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(9400),

                    repeatMode =
                        RepeatMode.Restart
                ),

            label = "ring3"
        )

    val pulse by
        infinite.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.08f,

            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(1100),

                    repeatMode =
                        RepeatMode.Reverse
                ),

            label = "orbPulse"
        )

    val accent =
        when (state) {

            WakeOverlayState.AWAKENING ->
                VisionIndigo

            WakeOverlayState.LISTENING ->
                Color(0xFF63E6FF)

            WakeOverlayState.THINKING ->
                VisionEmerald

            WakeOverlayState.SPEAKING ->
                Color(0xFFB47CFF)

            WakeOverlayState.IDLE ->
                Color(0xFF6D6DFF)
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .alpha(0.98f)
    ) {

        Canvas(
            modifier =
                Modifier
                    .size(330.dp)
                    .align(
                        Alignment.BottomCenter
                    )
        ) {

            val cx =
                size.width / 2f

            val cy =
                size.height * 0.56f

            val orbRadius =
                size.minDimension * 0.22f

            fun drawRing(
                angle: Float,
                scaleX: Float,
                scaleY: Float,
                alpha: Float
            ) {

                withTransform({

                    translate(
                        cx,
                        cy
                    )

                    rotate(angle)

                    scale(
                        scaleX,
                        scaleY
                    )

                    translate(
                        -cx,
                        -cy
                    )

                }) {

                    drawOval(

                        brush =
                            Brush.sweepGradient(

                                listOf(

                                    Color.Transparent,

                                    accent.copy(
                                        alpha = alpha
                                    ),

                                    Color.Transparent,

                                    accent.copy(
                                        alpha =
                                            alpha * 0.7f
                                    ),

                                    Color.Transparent
                                ),

                                center =
                                    androidx.compose.ui.geometry.Offset(
                                        cx,
                                        cy
                                    )
                            ),

                        topLeft =
                            androidx.compose.ui.geometry.Offset(

                                cx -
                                    orbRadius * 1.65f,

                                cy -
                                    orbRadius * 0.72f
                            ),

                        size =
                            androidx.compose.ui.geometry.Size(

                                orbRadius * 3.3f,

                                orbRadius * 1.44f
                            ),

                        style =
                            Stroke(
                                width = 5f
                            )
                    )
                }
            }

            /*
             * THREE REVOLVING RINGS
             */
            drawRing(
                ring1,
                1f,
                0.70f,
                0.95f
            )

            drawRing(
                ring2,
                0.72f,
                1.0f,
                0.80f
            )

            drawRing(
                ring3,
                0.92f,
                0.58f,
                0.65f
            )

            /*
             * CORE ORB
             */
            drawCircle(

                brush =
                    Brush.radialGradient(

                        colors =
                            listOf(

                                Color.White.copy(
                                    alpha = 0.95f
                                ),

                                accent.copy(
                                    alpha = 0.90f
                                ),

                                accent.copy(
                                    alpha = 0.18f
                                ),

                                Color.Transparent
                            ),

                        center =
                            androidx.compose.ui.geometry.Offset(

                                cx -
                                    orbRadius * 0.20f,

                                cy -
                                    orbRadius * 0.20f
                            ),

                        radius =
                            orbRadius * 1.55f
                    ),

                radius =
                    orbRadius * pulse,

                center =
                    androidx.compose.ui.geometry.Offset(
                        cx,
                        cy
                    )
            )

            /*
             * ORB OUTER GLOW
             */
            drawCircle(

                color =
                    accent.copy(
                        alpha = 0.25f
                    ),

                radius =
                    orbRadius *
                        1.65f *
                        pulse,

                center =
                    androidx.compose.ui.geometry.Offset(
                        cx,
                        cy
                    ),

                style =
                    Stroke(
                        width = 2f
                    )
            )

            /*
             * STAND
             */
            val standTop =
                size.height * 0.84f

            drawRoundRect(

                brush =
                    Brush.verticalGradient(

                        listOf(

                            accent.copy(
                                alpha = 0.65f
                            ),

                            accent.copy(
                                alpha = 0.15f
                            ),

                            Color.Transparent
                        )
                    ),

                topLeft =
                    androidx.compose.ui.geometry.Offset(

                        cx - 40f,
                        standTop
                    ),

                size =
                    androidx.compose.ui.geometry.Size(
                        80f,
                        45f
                    ),

                cornerRadius =
                    androidx.compose.ui.geometry.CornerRadius(
                        18f,
                        18f
                    )
            )

            /*
             * ORB → STAND ENERGY COLUMN
             */
            drawLine(

                color =
                    accent.copy(
                        alpha = 0.65f
                    ),

                start =
                    androidx.compose.ui.geometry.Offset(

                        cx,

                        cy +
                            orbRadius * 1.35f
                    ),

                end =
                    androidx.compose.ui.geometry.Offset(

                        cx,

                        standTop + 4f
                    ),

                strokeWidth = 3f
            )

            /*
             * BASE
             */
            drawRoundRect(

                color =
                    accent.copy(
                        alpha = 0.75f
                    ),

                topLeft =
                    androidx.compose.ui.geometry.Offset(

                        cx - 58f,

                        size.height - 24f
                    ),

                size =
                    androidx.compose.ui.geometry.Size(
                        116f,
                        12f
                    ),

                cornerRadius =
                    androidx.compose.ui.geometry.CornerRadius(
                        6f,
                        6f
                    )
            )
        }

        Text(
            text =
                when (state) {

                    WakeOverlayState.AWAKENING ->
                        "YES BOSS"

                    WakeOverlayState.LISTENING ->
                        "LISTENING..."

                    WakeOverlayState.THINKING ->
                        "PROCESSING..."

                    WakeOverlayState.SPEAKING ->
                        "VISION SPEAKING..."

                    WakeOverlayState.IDLE ->
                        "READY"
                },

            modifier =
                Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .padding(
                        bottom = 20.dp
                    ),

            color =
                VisionTextPrimary,

            fontSize = 11.sp,

            letterSpacing = 2.sp
        )
    }
}
