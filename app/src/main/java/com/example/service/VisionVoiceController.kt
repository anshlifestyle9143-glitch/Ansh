package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.util.GeminiTtsManager
import com.example.util.LiveSpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * VisionVoiceController
 *
 * Wake-word voice pipeline:
 *
 * Hey Jarvis
 *      ↓
 * Yes Boss
 *      ↓
 * User speech
 *      ↓
 * Semantic AI intent classification
 *      ↓
 * ┌──────────────┬──────────────┬──────────────┐
 * │ ACTION       │ CONVERSATION  │ QUESTION     │
 * │              │               │ SEARCH       │
 * ↓              ↓               ↓
 * Ok Boss        Natural reply   Information
 * ↓
 * Executor
 *
 * No fixed sentence matching.
 * No keyword-based command detection.
 */
class VisionVoiceController(
    private val context: Context,
    private val ttsManager: GeminiTtsManager
) {

    companion object {
        private const val TAG =
            "VisionVoiceController"

        private const val MAX_RECOGNITION_RETRIES = 2
    }

    private val scope =
        CoroutineScope(
            Dispatchers.Main.immediate +
                SupervisorJob()
        )

    private val intentClassifier =
        VisionIntentClassifier()

    private var recognizer:
        LiveSpeechRecognizer? = null

    private var listeningJob:
        Job? = null

    @Volatile
    private var active = false

    @Volatile
    private var processing = false

    private var recognitionRetryCount = 0

    fun start(
        onListening: () -> Unit = {},
        onThinking: () -> Unit = {},
        onSpeaking: () -> Unit = {},
        onConversation: (String) -> Unit = {},
        onCommand: (String) -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {

        if (active) {
            Log.d(
                TAG,
                "start(): already active"
            )
            return
        }

        active = true
        processing = false
        recognitionRetryCount = 0

        recognizer =
            LiveSpeechRecognizer(context)

        listeningJob =
            scope.launch {

                Log.d(
                    TAG,
                    "Speaking: Yes Boss"
                )

                speakAndWait(
                    text = "Yes Boss",
                    onSpeaking = onSpeaking
                )

                if (!active) {
                    return@launch
                }

                delay(700L)

                if (!active) {
                    return@launch
                }

                listenForInput(
                    onListening = onListening,
                    onThinking = onThinking,
                    onSpeaking = onSpeaking,
                    onConversation = onConversation,
                    onCommand = onCommand,
                    onDismiss = onDismiss
                )
            }
    }

    private fun listenForInput(
        onListening: () -> Unit,
        onThinking: () -> Unit,
        onSpeaking: () -> Unit,
        onConversation: (String) -> Unit,
        onCommand: (String) -> Unit,
        onDismiss: () -> Unit
    ) {

        if (!active || processing) {
            return
        }

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            Log.e(
                TAG,
                "RECORD_AUDIO permission missing"
            )

            stop()
            onDismiss()

            return
        }

        processing = true

        val speech =
            recognizer
                ?: LiveSpeechRecognizer(context)
                    .also {
                        recognizer = it
                    }

        speech.start(

            onPartial = { text ->

                if (!active) {
                    return@start
                }

                Log.d(
                    TAG,
                    "Partial speech: $text"
                )
            },

            onFinal = { text ->

                if (!active) {
                    return@start
                }

                val cleanText =
                    text.trim()

                Log.d(
                    TAG,
                    "Final speech: [$cleanText]"
                )

                processing = false
                recognitionRetryCount = 0

                if (cleanText.isBlank()) {

                    Log.d(
                        TAG,
                        "Empty final speech"
                    )

                    stop()
                    onDismiss()

                    return@start
                }

                classifyIntent(
                    text = cleanText,
                    onConversation = onConversation,
                    onCommand = onCommand,
                    onThinking = onThinking,
                    onSpeaking = onSpeaking,
                    onDismiss = onDismiss
                )
            },

            onListeningChange = { listening ->

                if (!active) {
                    return@start
                }

                if (listening) {

                    Log.d(
                        TAG,
                        "Microphone/listening = ON"
                    )

                    onListening()

                } else {

                    Log.d(
                        TAG,
                        "Recognition phase ended"
                    )
                }
            },

            onError = {

                if (!active) {
                    return@start
                }

                processing = false

                Log.w(
                    TAG,
                    "Speech recognition error"
                )

                if (
                    recognitionRetryCount <
                    MAX_RECOGNITION_RETRIES
                ) {

                    recognitionRetryCount++

                    Log.d(
                        TAG,
                        "Retrying recognition: " +
                            recognitionRetryCount
                    )

                    scope.launch {

                        delay(350L)

                        if (!active) {
                            return@launch
                        }

                        listenForInput(
                            onListening = onListening,
                            onThinking = onThinking,
                            onSpeaking = onSpeaking,
                            onConversation = onConversation,
                            onCommand = onCommand,
                            onDismiss = onDismiss
                        )
                    }

                } else {

                    Log.w(
                        TAG,
                        "Recognition retries exhausted"
                    )

                    stop()
                    onDismiss()
                }
            },

            /*
             * IMPORTANT:
             *
             * Manual voice page remains continuous.
             * Overlay keeps its normal timeout because it
             * does not pass this flag.
             */
            continuous = true
        )
    }

    /**
     * Semantic AI gateway.
     *
     * There is deliberately NO:
     *
     * text.contains("flashlight")
     * text.contains("light")
     * text.contains("call")
     *
     * etc.
     *
     * The AI determines what the user means.
     */
    private fun classifyIntent(
        text: String,
        onConversation: (String) -> Unit,
        onCommand: (String) -> Unit,
        onThinking: () -> Unit,
        onSpeaking: () -> Unit,
        onDismiss: () -> Unit
    ) {

        scope.launch {

            if (!active) {
                return@launch
            }

            onThinking()

            val result =
                intentClassifier.classify(text)

            if (!active) {
                return@launch
            }

            Log.d(
                TAG,
                "Intent result: ${result.type}"
            )

            when (result.type) {

                VisionIntentClassifier.IntentType.ACTION -> {

                    /*
                     * ACTION:
                     *
                     * Only now say Ok Boss.
                     *
                     * Normal conversation never reaches here.
                     */
                    speakAndWait(
                        text = "Ok Boss",
                        onSpeaking = onSpeaking
                    )

                    if (!active) {
                        return@launch
                    }

                    onCommand(
                        result.originalText
                    )
                }

                VisionIntentClassifier.IntentType.CONVERSATION -> {

                    /*
                     * Normal conversation.
                     *
                     * NO "Ok Boss".
                     */
                    onConversation(
                        result.originalText
                    )
                }

                VisionIntentClassifier.IntentType.QUESTION -> {

                    /*
                     * Questions are not device actions.
                     *
                     * NO "Ok Boss".
                     */
                    onConversation(
                        result.originalText
                    )
                }

                VisionIntentClassifier.IntentType.SEARCH -> {

                    /*
                     * Search requests are also not treated
                     * as generic device actions.
                     *
                     * NO forced "Ok Boss" here.
                     */
                    onConversation(
                        result.originalText
                    )
                }

                VisionIntentClassifier.IntentType.UNCLEAR -> {

                    /*
                     * Never execute something when intent
                     * is uncertain.
                     */
                    speakAndWait(
                        text =
                            "Boss, mujhe samajh nahi aaya. Dobara boliye.",
                        onSpeaking = onSpeaking
                    )

                    if (!active) {
                        return@launch
                    }

                    /*
                     * Return to listening rather than
                     * destroying the complete voice session.
                     */
                    delay(300L)

                    listenForInput(
                        onListening = onListeningCallback,
                        onThinking = onThinking,
                        onSpeaking = onSpeaking,
                        onConversation = onConversation,
                        onCommand = onCommand,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }

    /*
     * These callbacks are temporarily stored so the
     * semantic layer can return to listening after
     * an unclear request.
     */
    private var onListeningCallback:
        () -> Unit = {}

    private suspend fun speakAndWait(
        text: String,
        onSpeaking: () -> Unit
    ) {

        if (!active) {
            return
        }

        onSpeaking()

        Log.d(
            TAG,
            "TTS start: [$text]"
        )

        ttsManager.speak(
            text,
            -System.currentTimeMillis()
        )

        val started =
            withTimeoutOrNull(3000L) {

                ttsManager
                    .isSpeaking
                    .first { it }

                true

            } == true

        if (started) {

            withTimeoutOrNull(10000L) {

                ttsManager
                    .isSpeaking
                    .first { !it }
            }

            Log.d(
                TAG,
                "TTS finished"
            )

        } else {

            Log.w(
                TAG,
                "TTS speaking state not detected"
            )
        }
    }

    fun stop() {

        Log.d(
            TAG,
            "Stopping voice controller"
        )

        active = false
        processing = false
        recognitionRetryCount = 0

        listeningJob?.cancel()
        listeningJob = null

        try {
            recognizer?.stop()
        } catch (_: Exception) {
        }

        try {
            recognizer?.destroy()
        } catch (_: Exception) {
        }

        recognizer = null
    }

    fun destroy() {

        Log.d(
            TAG,
            "Destroying voice controller"
        )

        stop()

        scope.coroutineContext[
            kotlinx.coroutines.Job
        ]?.cancel()
    }
}
