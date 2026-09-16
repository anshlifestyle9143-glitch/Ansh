package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.util.LiveSpeechRecognizer
import com.example.util.TtsManager
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

        /*
         * If user does not start another command within
         * this time after listening begins, the voice session
         * will automatically dismiss.
         */
        private const val IDLE_TIMEOUT_MS = 6000L
    }

    private val scope =
        CoroutineScope(
            Dispatchers.Main.immediate + SupervisorJob()
        )

    private var recognizer: LiveSpeechRecognizer? = null
    private var listeningJob: Job? = null

    /*
     * Automatically cancels the 6-second idle timer.
     */
    private var idleTimeoutJob: Job? = null

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
        onListening: () -> Unit,
        onThinking: () -> Unit,
        onSpeaking: () -> Unit,
        onConversation: (String) -> Unit,
        onCommand: (String) -> Unit,
        onDismiss: () -> Unit
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

        cancelIdleTimeout()

        onListeningCallback = onListening
        onThinkingCallback = onThinking
        onSpeakingCallback = onSpeaking
        onConversationCallback = onConversation
        onCommandCallback = onCommand
        onDismissCallback = onDismiss

        recognizer =
            LiveSpeechRecognizer(context)

        listeningJob =
            scope.launch {

                Log.d(
                    TAG,
                    "Speaking: Yes Boss"
                )

                speakAndWait(
                    "Yes Boss",
                    onSpeaking
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

        if (
            !active ||
            processing
        ) {
            return
        }

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            Log.w(
                TAG,
                "RECORD_AUDIO permission missing"
            )

            cancelIdleTimeout()

            stop()
            onDismissCallback()

            return
        }

        processing = true

        val speech =
            recognizer
                ?: LiveSpeechRecognizer(context).also {
                    recognizer = it
                }

        speech.start(

            onPartial = { text ->

                if (!active) {
                    return@start
                }

                /*
                 * User has started speaking.
                 * Cancel the idle timeout immediately.
                 */
                cancelIdleTimeout()

                Log.d(
                    TAG,
                    "Partial speech: $text"
                )
            },

            onFinal = { text ->

                if (!active) {
                    return@start
                }

                /*
                 * User produced a final result.
                 * The current listening timeout is no longer needed.
                 */
                cancelIdleTimeout()

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

                    /*
                     * Start the 6-second countdown whenever
                     * the microphone becomes ready.
                     *
                     * This covers:
                     * 1. Initial listening after "Yes Boss"
                     * 2. Listening after an AI response
                     * 3. Listening after a command
                     */
                    startIdleTimeout()

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

                cancelIdleTimeout()

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
                            recognitionRetryCount
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

    /*
     * ------------------------------------------------------------
     * 6 SECOND IDLE TIMEOUT
     * ------------------------------------------------------------
     */
    private fun startIdleTimeout() {

        cancelIdleTimeout()

        if (!active) {
            return
        }

        idleTimeoutJob =
            scope.launch {

                Log.d(
                    TAG,
                    "Starting ${IDLE_TIMEOUT_MS}ms voice idle timeout"
                )

                delay(IDLE_TIMEOUT_MS)

                if (
                    !active ||
                    processing
                ) {
                    return@launch
                }

                Log.d(
                    TAG,
                    "Voice idle timeout reached - " +
                        "dismissing session"
                )

                stop()

                onDismissCallback()
            }
    }

    private fun cancelIdleTimeout() {

        idleTimeoutJob?.cancel()
        idleTimeoutJob = null
    }

    private fun classifyIntent(
        text: String
    ) {

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

                    // -------------------------------------------------
                    // ACTION
                    // -------------------------------------------------

                    VisionIntentClassifier.IntentType.ACTION -> {

                        Log.d(
                            TAG,
                            "Action detected"
                        )

                        onThinkingCallback()

                        speakAndWait(
                            "Ok Boss",
                            onSpeakingCallback
                        )

                        if (!active) {
                            return@launch
                        }

                        /*
                         * WakeWordService executes the command.
                         *
                         * After execution it calls resumeListening().
                         */
                        onCommandCallback(text)
                    }

                    // -------------------------------------------------
                    // CONVERSATION
                    // -------------------------------------------------

                    VisionIntentClassifier.IntentType.CONVERSATION -> {

                        Log.d(
                            TAG,
                            "Normal conversation detected"
                        )

                        onThinkingCallback()

                        /*
                         * Do NOT resume here.
                         *
                         * WakeWordService:
                         *
                         * AI → TTS → resumeListening()
                         */
                        onConversationCallback(text)
                    }

                    // -------------------------------------------------
                    // QUESTION
                    // -------------------------------------------------

                    VisionIntentClassifier.IntentType.QUESTION -> {

                        Log.d(
                            TAG,
                            "Question detected"
                        )

                        onThinkingCallback()

                        onConversationCallback(text)
                    }

                    // -------------------------------------------------
                    // SEARCH
                    // -------------------------------------------------

                    VisionIntentClassifier.IntentType.SEARCH -> {

                        Log.d(
                            TAG,
                            "Search request detected"
                        )

                        onThinkingCallback()

                        onConversationCallback(text)
                    }

                    // -------------------------------------------------
                    // UNCLEAR
                    // -------------------------------------------------

                    VisionIntentClassifier.IntentType.UNCLEAR -> {

                        Log.d(
                            TAG,
                            "Intent unclear"
                        )

                        speakAndWait(
                            "Boss, thoda clearly bataiye.",
                            onSpeakingCallback
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
                 * If classification fails, still send the
                 * original text to the AI brain.
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

        cancelIdleTimeout()

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

    fun resumeListening(
        delayMs: Long = 250L
    ) {

        if (!active) {
            return
        }

        cancelIdleTimeout()

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

        cancelIdleTimeout()

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
