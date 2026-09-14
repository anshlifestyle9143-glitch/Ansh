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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    DisposableEffect(Unit) {

        onDispose {

            active = false
            listeningStarted = false

            speechRecognizer.stop()
            speechRecognizer.destroy()
        }
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

                if (!active) return@start

                listeningStarted = false

                if (text.isNotBlank()) {

                    /*
                     * -------------------------------------------------
                     * COMMAND RECEIVED
                     *
                     * First say "Ok Boss".
                     * Only after TTS finishes, start AI processing.
                     * -------------------------------------------------
                     */

                    callState =
                        VoiceCallState.SPEAKING

                    coroutineScope.launch {

                        viewModel.ttsManager.speak(
                            "Ok Boss",
                            -System.currentTimeMillis()
                        )

                        /*
                         * Wait until Ok Boss TTS actually starts.
                         */
                        viewModel.ttsManager.isSpeaking
                            .first { it }

                        /*
                         * Wait until Ok Boss TTS completely finishes.
                         */
                        viewModel.ttsManager.isSpeaking
                            .first { !it }

                        if (!active) return@launch

                        /*
                         * Now start processing the command.
                         */
                        callState =
                            VoiceCallState.THINKING

                        viewModel.sendMessage(
                            overridePrompt = text,
                            autoSpeak = true
                        )
                    }

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
                     * IMPORTANT:
                     *
                     * onEndOfSpeech() does NOT mean that the final
                     * recognition result has arrived.
                     *
                     * SpeechRecognizer normally calls onResults()
                     * after onEndOfSpeech().
                     *
                     * Therefore we intentionally DO NOT change
                     * LISTENING -> THINKING here.
                     *
                     * The final command is handled only by onFinal().
                     */
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

    /*
     * =============================================================
     * WAKE WORD ACTIVATION
     *
     * Hey Jarvis
     *      ↓
     * Yes Boss
     *      ↓
     * TTS complete
     *      ↓
     * Microphone ON
     * =============================================================
     */
    LaunchedEffect(Unit) {

        callState =
            VoiceCallState.SPEAKING

        viewModel.ttsManager.speak(
            "Yes Boss",
            -System.currentTimeMillis()
        )

        /*
         * Wait until Yes Boss TTS actually starts.
         */
        viewModel.ttsManager.isSpeaking
            .first { it }

        /*
         * Wait until Yes Boss TTS finishes.
         */
        viewModel.ttsManager.isSpeaking
            .first { !it }

        /*
         * Safety gap between TTS releasing the audio path
         * and SpeechRecognizer acquiring the microphone.
         */
        delay(400L)

        if (active) {
            requestListening()
        }
    }

    /*
     * =============================================================
     * AI GENERATION STATE
     * =============================================================
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
     * =============================================================
     * TTS STATE
     *
     * During speech microphone stays OFF.
     * After response speech ends, listening resumes.
     * =============================================================
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
     * =============================================================
     * THINKING → LISTENING
     *
     * After AI processing completes without TTS,
     * listening can resume.
     * =============================================================
     */
    LaunchedEffect(
        isGenerating,
        isSpeaking
    ) {

        if (!active) return@LaunchedEffect

        if (
            !isGenerating &&
            !isSpeaking &&
            callState ==
                VoiceCallState.THINKING
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
     * =============================================================
     * VOICE ORB ANIMATION
     * =============================================================
     */
    val infiniteTransition =
        rememberInfiniteTransition(
            label = "voiceOrb"
        )

    val pulseScale by
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 1100
                        ),
                    repeatMode =
                        RepeatMode.Reverse
                ),
            label = "pulseScale"
        )

    val glowAlpha by
        infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.55f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis = 1100
                        ),
                    repeatMode =
                        RepeatMode.Reverse
                ),
            label = "glowAlpha"
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            VisionBackground,
                            VisionDeepPlum
                        )
                    )
                )
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.End
            ) {

                IconButton(
                    onClick = onExit
                ) {

                    Icon(
                        imageVector =
                            Icons.Default.Close,
                        contentDescription =
                            "Close",
                        tint =
                            VisionTextPrimary
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(50.dp)
            )

            Box(
                modifier =
                    Modifier
                        .size(230.dp)
                        .graphicsLayer {
                            scaleX =
                                if (
                                    callState ==
                                        VoiceCallState.LISTENING
                                ) {
                                    pulseScale
                                } else {
                                    1f
                                }

                            scaleY =
                                if (
                                    callState ==
                                        VoiceCallState.LISTENING
                                ) {
                                    pulseScale
                                } else {
                                    1f
                                }

                            alpha =
                                if (
                                    callState ==
                                        VoiceCallState.LISTENING
                                ) {
                                    glowAlpha
                                } else {
                                    1f
                                }
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    VisionIndigo,
                                    VisionEmerald
                                )
                            )
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Icon(
                    imageVector =
                        Icons.Default.Mic,
                    contentDescription =
                        "Voice",
                    tint =
                        Color.White,
                    modifier =
                        Modifier.size(70.dp)
                )
            }

            Spacer(
                modifier =
                    Modifier.height(35.dp)
            )

            Text(
                text =
                    when (callState) {

                        VoiceCallState.IDLE ->
                            "Ready"

                        VoiceCallState.LISTENING ->
                            "Listening..."

                        VoiceCallState.THINKING ->
                            "Thinking..."

                        VoiceCallState.SPEAKING ->
                            "Speaking..."

                        VoiceCallState.NO_PERMISSION ->
                            "Microphone permission required"
                    },
                color =
                    VisionTextPrimary,
                fontSize =
                    22.sp
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text =
                    when (callState) {

                        VoiceCallState.LISTENING ->
                            "Tell me what you need"

                        VoiceCallState.THINKING ->
                            "Processing your command"

                        VoiceCallState.SPEAKING ->
                            "Vision is speaking"

                        VoiceCallState.NO_PERMISSION ->
                            "Please allow microphone access"

                        else ->
                            "Vision Voice Assistant"
                    },
                color =
                    VisionTextSecondary,
                fontSize =
                    15.sp
            )

            Spacer(
                modifier =
                    Modifier.height(40.dp)
            )

            Surface(
                modifier =
                    Modifier.fillMaxWidth(),
                shape =
                    RoundedCornerShape(22.dp),
                color =
                    Color.White.copy(alpha = 0.08f)
            ) {

                Column(
                    modifier =
                        Modifier.padding(20.dp)
                ) {

                    Text(
                        text =
                            "Voice Mode",
                        color =
                            VisionTextPrimary,
                        fontSize =
                            17.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Say \"Hey Jarvis\" to activate Vision.",
                        color =
                            VisionTextSecondary,
                        fontSize =
                            14.sp
                    )
                }
            }
        }
    }
}
