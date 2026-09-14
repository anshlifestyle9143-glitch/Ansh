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

    var listeningStarted by remember {
        mutableStateOf(false)
    }

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
     * Wake-word activation enters this screen here.
     * The popup is visible immediately.
     */
    LaunchedEffect(Unit) {
    callState = VoiceCallState.SPEAKING

    viewModel.ttsManager.speak(
        "Yes Boss",
        -System.currentTimeMillis()
    )

    delay(600L)

    if (active) {
        requestListening()
    }
    }

    LaunchedEffect(isGenerating) {

        if (!active) return@LaunchedEffect

        if (isGenerating) {

            listeningStarted = false

            callState =
                VoiceCallState.THINKING

            speechRecognizer.stop()
        }
    }

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

    LaunchedEffect(isGenerating, isSpeaking) {

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
                )
    ) {

        /*
         * Existing voice orb.
         */
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = 170.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
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
         * Close button.
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
         * =====================================================
         * VISION BOTTOM POPUP
         * =====================================================
         */
        Surface(
            modifier =
                Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 20.dp
                    ),

            shape =
                RoundedCornerShape(
                    28.dp
                ),

            color =
                VisionDeepPlum.copy(
                    alpha = 0.96f
                ),

            tonalElevation =
                8.dp,

            shadowElevation =
                12.dp
        ) {

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 16.dp
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
                                .clip(
                                    CircleShape
                                )
                                .background(
                                    orbColor
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            Icons.Default.Mic,
                            contentDescription =
                                "Vision microphone",
                            tint =
                                Color.White,
                            modifier =
                                Modifier.size(21.dp)
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.size(14.dp)
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(
                            text = "VISION",
                            color =
                                VisionTextPrimary,
                            fontSize =
                                16.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.height(2.dp)
                        )

                        Text(
                            text =
                                when (callState) {

                                    VoiceCallState.LISTENING ->
                                        "Listening..."

                                    VoiceCallState.THINKING ->
                                        "Processing..."

                                    VoiceCallState.SPEAKING ->
                                        "Speaking..."

                                    VoiceCallState.IDLE ->
                                        "Ready"

                                    VoiceCallState.NO_PERMISSION ->
                                        "Microphone permission required"
                                },

                            color =
                                VisionTextSecondary,

                            fontSize =
                                13.sp
                        )
                    }

                    if (
                        callState ==
                            VoiceCallState.IDLE ||
                        callState ==
                            VoiceCallState.NO_PERMISSION
                    ) {

                        IconButton(
                            onClick = {
                                requestListening()
                            }
                        ) {

                            Icon(
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
                        Modifier.height(10.dp)
                )

                Text(
                    text =
                        when (callState) {

                            VoiceCallState.LISTENING ->
                                "I'm listening for your command"

                            VoiceCallState.THINKING ->
                                "Working on your command..."

                            VoiceCallState.SPEAKING ->
                                "Vision is responding..."

                            VoiceCallState.IDLE ->
                                "Tap the microphone to speak"

                            VoiceCallState.NO_PERMISSION ->
                                "Allow microphone access to continue"
                        },

                    color =
                        VisionTextSecondary,

                    fontSize =
                        12.sp
                )
            }
        }

        /*
         * Manual microphone button is now inside
         * the bottom popup, so no separate button
         * is displayed here.
         */
    }
}
