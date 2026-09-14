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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.VisionBackground
import com.example.ui.theme.VisionDeepPlum
import com.example.ui.theme.VisionEmerald
import com.example.ui.theme.VisionIndigo
import com.example.ui.theme.VisionTextPrimary
import com.example.ui.theme.VisionTextSecondary
import com.example.ui.viewmodel.VisionViewModel
import com.example.util.LiveSpeechRecognizer
import kotlinx.coroutines.delay

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

    /*
     * Prevent multiple SpeechRecognizer sessions
     * from being started at the same time.
     */
    var listeningStarted by remember {
        mutableStateOf(false)
    }

    val speechRecognizer = remember {
        LiveSpeechRecognizer(context)
    }

    /*
     * Clean everything when the voice screen disappears.
     */
    DisposableEffect(Unit) {

        onDispose {

            active = false
            listeningStarted = false

            speechRecognizer.stop()
            speechRecognizer.destroy()
        }
    }

    /*
     * Start one speech-recognition session.
     */
    fun startListening() {

        if (!active) return

        /*
         * Never start another recognizer while one
         * is already active.
         */
        if (listeningStarted) return

        /*
         * Never listen while Vision is generating
         * or speaking.
         */
        if (isGenerating || isSpeaking) return

        listeningStarted = true

        speechRecognizer.start(

            onPartial = {
                /*
                 * Partial speech intentionally not displayed.
                 */
            },

            onFinal = { text ->

                if (!active) return@start

                listeningStarted = false

                if (text.isNotBlank()) {

                    callState =
                        VoiceCallState.THINKING

                    viewModel.sendMessage(
                        overridePrompt = text,
                        autoSpeak = true
                    )
                } else {

                    callState =
                        VoiceCallState.IDLE
                }
            },

            onListeningChange = { listening ->

                if (!active) return@start

                if (listening) {

                    listeningStarted = true

                    callState =
                        VoiceCallState.LISTENING

                } else {

                    /*
                     * Do not immediately restart here.
                     *
                     * LiveSpeechRecognizer now stops after a
                     * final result. The AI/TTS flow decides
                     * when listening should resume.
                     */
                    if (
                        callState ==
                            VoiceCallState.LISTENING
                    ) {

                        callState =
                            VoiceCallState.THINKING
                    }
                }
            },

            onError = {

                if (!active) return@start

                listeningStarted = false

                callState =
                    VoiceCallState.IDLE
            }
        )
    }

    /*
     * Microphone permission.
     */
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (!active) return@rememberLauncherForActivityResult

            if (granted) {

                startListening()

            } else {

                listeningStarted = false

                callState =
                    VoiceCallState.NO_PERMISSION
            }
        }

    /*
     * Request microphone listening.
     */
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

    /*
     * Initial voice activation.
     *
     * The wake-word engine has just released the microphone,
     * so give Android a short moment before SpeechRecognizer
     * takes control.
     */
    LaunchedEffect(Unit) {

        delay(600L)

        if (active) {
            requestListening()
        }
    }

    /*
     * AI generation state.
     */
    LaunchedEffect(isGenerating) {

        if (!active) return@LaunchedEffect

        if (isGenerating) {

            listeningStarted = false

            callState =
                VoiceCallState.THINKING

            speechRecognizer.stop()
        }
    }

    /*
     * TTS lifecycle.
     *
     * Speaking:
     *      microphone OFF
     *
     * Finished:
     *      microphone ON again
     */
    LaunchedEffect(isSpeaking) {

        if (!active) return@LaunchedEffect

        if (isSpeaking) {

            listeningStarted = false

            callState =
                VoiceCallState.SPEAKING

            speechRecognizer.stop()

        } else if (
            callState ==
                VoiceCallState.SPEAKING
        ) {

            /*
             * Small gap between TTS and microphone.
             *
             * This prevents the last few milliseconds of
             * Vision's voice from being captured as a command.
             */
            delay(350L)

            if (
                active &&
                !isGenerating
            ) {

                requestListening()
            }
        }
    }

    /*
     * If generation finishes without TTS starting,
     * return to listening.
     *
     * This also covers cases where the TTS provider
     * fails or returns without entering speaking state.
     */
    LaunchedEffect(isGenerating, isSpeaking) {

        if (!active) return@LaunchedEffect

        if (
            !isGenerating &&
            !isSpeaking &&
            callState == VoiceCallState.THINKING
        ) {

            delay(350L)

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
     * Animated voice orb.
     */
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "voiceOrb"
        )

    val pulse by
        infiniteTransition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1.1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(1200),
                    repeatMode =
                        RepeatMode.Reverse
                ),
            label = "pulse"
        )

    val orbColor =
        when (callState) {

            VoiceCallState.LISTENING ->
                VisionDeepPlum

            VoiceCallState.THINKING ->
                VisionEmerald

            VoiceCallState.SPEAKING ->
                VisionIndigo

            VoiceCallState.IDLE,
            VoiceCallState.NO_PERMISSION ->
                VisionDeepPlum.copy(
                    alpha = 0.5f
                )
        }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    VisionBackground
                ),
        contentAlignment =
            Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Box(
                modifier =
                    Modifier
                        .size(180.dp)
                        .graphicsLayer {

                            val scale =
                                if (
                                    callState ==
                                        VoiceCallState.IDLE ||
                                    callState ==
                                        VoiceCallState.NO_PERMISSION
                                ) {

                                    1f

                                } else {

                                    pulse
                                }

                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        orbColor,
                                        orbColor.copy(
                                            alpha = 0.15f
                                        )
                                    )
                            )
                        )
            )

            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )

            Text(
                text =
                    when (callState) {

                        VoiceCallState.LISTENING ->
                            "Sun rahi hoon..."

                        VoiceCallState.THINKING ->
                            "Soch rahi hoon..."

                        VoiceCallState.SPEAKING ->
                            "Bol rahi hoon..."

                        VoiceCallState.IDLE ->
                            "Bolne ke liye mic dabao"

                        VoiceCallState.NO_PERMISSION ->
                            "Mic permission chahiye — settings me allow karo"
                    },

                color =
                    VisionTextSecondary,

                fontSize =
                    14.sp
            )
        }

        /*
         * Close voice conversation.
         */
        IconButton(
            onClick = {

                active = false
                listeningStarted = false

                speechRecognizer.stop()
                speechRecognizer.destroy()

                onExit()
            },

            modifier =
                Modifier
                    .align(
                        Alignment.TopStart
                    )
                    .padding(20.dp)
        ) {

            Icon(
                Icons.Default.Close,
                contentDescription =
                    "End call",
                tint =
                    VisionTextPrimary
            )
        }

        /*
         * Manual microphone button.
         */
        if (
            callState ==
                VoiceCallState.IDLE ||
            callState ==
                VoiceCallState.NO_PERMISSION
        ) {

            IconButton(
                onClick = {

                    requestListening()
                },

                modifier =
                    Modifier
                        .align(
                            Alignment.BottomCenter
                        )
                        .padding(
                            bottom = 56.dp
                        )
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            VisionDeepPlum
                        )
            ) {

                Icon(
                    Icons.Default.Mic,
                    contentDescription =
                        "Speak",
                    tint =
                        Color.White
                )
            }
        }
    }
}
