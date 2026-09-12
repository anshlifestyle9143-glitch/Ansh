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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

private enum class VoiceCallState { IDLE, LISTENING, THINKING, SPEAKING, NO_PERMISSION }

@Composable
fun VoiceConversationScreen(
    viewModel: VisionViewModel,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isSpeaking by viewModel.ttsManager.isSpeaking.collectAsState()
    var callState by remember { mutableStateOf(VoiceCallState.IDLE) }
    var active by remember { mutableStateOf(true) }

    val speechRecognizer = remember { LiveSpeechRecognizer(context) }

    DisposableEffect(Unit) {
        onDispose { speechRecognizer.destroy() }
    }

    fun startListening() {
        speechRecognizer.start(
            onPartial = {},
            onFinal = { text ->
                callState = VoiceCallState.THINKING
                viewModel.sendMessage(overridePrompt = text, autoSpeak = true)
            },
            onListeningChange = { listening ->
                if (listening) callState = VoiceCallState.LISTENING
            },
            onError = {
                if (active) callState = VoiceCallState.IDLE
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening() else callState = VoiceCallState.NO_PERMISSION
    }

    fun requestListening() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        requestListening()
    }

    LaunchedEffect(isGenerating) {
        if (isGenerating) callState = VoiceCallState.THINKING
    }

    LaunchedEffect(isSpeaking) {
        if (isSpeaking) {
            callState = VoiceCallState.SPEAKING
        } else if (callState == VoiceCallState.SPEAKING && active) {
            requestListening()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "voiceOrb")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(animation = tween(1200), repeatMode = RepeatMode.Reverse),
        label = "pulse"
    )

    val orbColor = when (callState) {
        VoiceCallState.LISTENING -> VisionDeepPlum
        VoiceCallState.THINKING -> VisionEmerald
        VoiceCallState.SPEAKING -> VisionIndigo
        VoiceCallState.IDLE, VoiceCallState.NO_PERMISSION -> VisionDeepPlum.copy(alpha = 0.5f)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(VisionBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        val s = if (callState == VoiceCallState.IDLE || callState == VoiceCallState.NO_PERMISSION) 1f else pulse
                        scaleX = s
                        scaleY = s
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(orbColor, orbColor.copy(alpha = 0.15f))
                        )
                    )
            )
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = when (callState) {
                    VoiceCallState.LISTENING -> "Sun rahi hoon..."
                    VoiceCallState.THINKING -> "Soch rahi hoon..."
                    VoiceCallState.SPEAKING -> "Bol rahi hoon..."
                    VoiceCallState.IDLE -> "Bolne ke liye mic dabao"
                    VoiceCallState.NO_PERMISSION -> "Mic permission chahiye — settings me allow karo"
                },
                color = VisionTextSecondary,
                fontSize = 14.sp
            )
        }

        IconButton(
            onClick = {
                active = false
                onExit()
            },
            modifier = Modifier.align(Alignment.TopStart).padding(20.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "End call", tint = VisionTextPrimary)
        }

        if (callState == VoiceCallState.IDLE || callState == VoiceCallState.NO_PERMISSION) {
            IconButton(
                onClick = { requestListening() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(VisionDeepPlum)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Speak", tint = Color.White)
            }
        }
    }
}
