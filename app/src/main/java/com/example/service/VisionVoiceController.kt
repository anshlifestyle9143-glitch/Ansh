package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.util.TtsManager
import com.example.util.LiveSpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class VisionVoiceController(
    private val context: Context,
    private val ttsManager: TtsManager
) {

    companion object {
        private const val TAG = "VisionVoiceController"
        private const val MAX_RECOGNITION_RETRIES = 2
    }

    private val scope =
        CoroutineScope(
            Dispatchers.Main.immediate +
                SupervisorJob()
        )

    private var recognizer: LiveSpeechRecognizer? = null
    private var listeningJob: Job? = null

    @Volatile
    private var active = false

    @Volatile
    private var processing = false

    private var recognitionRetryCount = 0

    private var onListeningCallback: () -> Unit = {}
    private var onThinkingCallback: () -> Unit = {}
    private var onSpeakingCallback: () -> Unit = {}
    private var onConversationCallback: (String) -> Unit = {}
    private var onCommandCallback: (String) -> Unit = {}
    private var onDismissCallback: () -> Unit = {}

    fun start(
        onListening: () -> Unit = {},
        onThinking: () -> Unit = {},
        onSpeaking: () -> Unit = {},
        onConversation: (String) -> Unit = {},
        onCommand: (String) -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {

        if (active) {
            Log.d(TAG, "start(): already active")
            return
        }

        active = true
        processing = false
        recognitionRetryCount = 0

        onListeningCallback = onListening
        onThinkingCallback = onThinking
        onSpeakingCallback = onSpeaking
        onConversationCallback = onConversation
        onCommandCallback = onCommand
        onDismissCallback = onDismiss

        recognizer = LiveSpeechRecognizer(context)

        listeningJob = scope.launch {

            Log.d(TAG, "Speaking: Yes Boss")

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

            listenForInput()
        }
    }

    private fun listenForInput() {

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
            onDismissCallback()

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

                val cleanText = text.trim()

                Log.d(
                    TAG,
                    "Final speech: [$cleanText]"
                )

                processing = false
                recognitionRetryCount = 0

                if (cleanText.isBlank()) {

                    Log.d(
                        TAG,
                        "Empty result - restarting listening"
                    )

                    scope.launch {
                        delay(250L)

                        if (active) {
                            listenForInput()
                        }
                    }

                    return@start
                }

                classifyIntent(cleanText)
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

                    onListeningCallback()

                } else {

                    Log.d(
                        TAG,
                        "Speech recognition temporarily ended"
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
                        "Retrying speech recognition: " +
                            "$recognitionRetryCount"
                    )

                    scope.launch {

                        delay(350L)

                        if (active) {
                            listenForInput()
                        }
                    }

                } else {

                    Log.w(
                        TAG,
                        "Speech recognition retries exhausted"
                    )

                    stop()
                    onDismissCallback()
                }
            },

            continuous = true
        )
    }

    private fun classifyIntent(text: String) {

        if (!active) {
            return
        }

        scope.launch {

            try {

                Log.d(
                    TAG,
                    "Classifying semantic intent: [$text]"
                )

                val classifier =
                    VisionIntentClassifier()

                val result =
                    classifier.classify(text)

                if (!active) {
                    return@launch
                }

                Log.d(
                    TAG,
                    "Intent result: ${result.type}"
                )

                when (result.type) {

                    VisionIntentClassifier.IntentType.ACTION -> {

                        onThinkingCallback()

                        speakAndWait(
                            text = "Ok Boss",
                            onSpeaking = onSpeakingCallback
                        )

                        if (!active) {
                            return@launch
                        }

                        onCommandCallback(text)
                    }

                    VisionIntentClassifier.IntentType.CONVERSATION -> {

                        Log.d(
                            TAG,
                            "Normal conversation"
                        )

                        onConversationCallback(text)

                        resumeListening()
                    }

                    VisionIntentClassifier.IntentType.QUESTION -> {

                        Log.d(
                            TAG,
                            "Question detected"
                        )

                        onConversationCallback(text)

                        resumeListening()
                    }

                    VisionIntentClassifier.IntentType.SEARCH -> {

                        Log.d(
                            TAG,
                            "Search request detected"
                        )

                        onConversationCallback(text)

                        resumeListening()
                    }

                    VisionIntentClassifier.IntentType.UNCLEAR -> {

                        Log.d(
                            TAG,
                            "Intent unclear"
                        )

                        speakAndWait(
                            text = "Boss, thoda clearly bataiye.",
                            onSpeaking = onSpeakingCallback
                        )

                        if (!active) {
                            return@launch
                        }

                        delay(400L)

                        if (!active) {
                            return@launch
                        }

                        listenForInput()
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Intent classification failed",
                    e
                )

                if (!active) {
                    return@launch
                }

                /*
                 * Classification failure must never
                 * directly execute a device action.
                 *
                 * Send it to normal AI conversation.
                 */
                onConversationCallback(text)
            }
        }
    }

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

            Log.d(
                TAG,
                "TTS speaking"
            )

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
                "TTS speaking state was not detected"
            )
        }
    }

    /**
     * Resume microphone listening after the current
     * voice task has completed.
     */
    fun resumeListening(delayMs: Long = 250L) {

        if (!active) {
            return
        }

        processing = false

        scope.launch {

            delay(delayMs)

            if (active) {
                listenForInput()
            }
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

        onListeningCallback = {}
        onThinkingCallback = {}
        onSpeakingCallback = {}
        onConversationCallback = {}
        onCommandCallback = {}
        onDismissCallback = {}

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
