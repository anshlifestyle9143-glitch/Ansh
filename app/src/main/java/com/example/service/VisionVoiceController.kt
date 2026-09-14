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
 * Microphone ON
 *      ↓
 * User speech
 *      ↓
 * Semantic AI Intent Classification
 *      ↓
 * ┌──────────────────────────────────────────────┐
 * │ ACTION       → Yes → Ok Boss → execute       │
 * │ CONVERSATION → natural conversation          │
 * │ QUESTION     → natural answer/search         │
 * │ SEARCH       → search/AI response            │
 * │ UNCLEAR      → clarification                 │
 * └──────────────────────────────────────────────┘
 *
 * IMPORTANT:
 * No hard-coded user-command keyword matching is
 * performed here.
 */
class VisionVoiceController(
    private val context: Context,
    private val ttsManager: GeminiTtsManager
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

    /**
     * Keeps the latest listening callback so that
     * the controller can return to listening after
     * clarification or another internal voice phase.
     */
    private var onListeningCallback: () -> Unit = {}

    /**
     * Starts the wake-word voice interaction.
     */
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

        /*
         * IMPORTANT:
         * Save the callback immediately after activating
         * the controller.
         *
         * This is required when the controller needs to
         * start listening again later.
         */
        active = true
        onListeningCallback = onListening
        processing = false
        recognitionRetryCount = 0

        recognizer = LiveSpeechRecognizer(context)

        listeningJob = scope.launch {

            /*
             * ---------------------------------------------
             * 1. WAKE WORD RESPONSE
             * ---------------------------------------------
             */

            Log.d(TAG, "Speaking: Yes Boss")

            speakAndWait(
                text = "Yes Boss",
                onSpeaking = onSpeaking
            )

            if (!active) {
                return@launch
            }

            /*
             * Give TTS/audio system time to release
             * before opening the microphone.
             */
            delay(700L)

            if (!active) {
                return@launch
            }

            /*
             * ---------------------------------------------
             * 2. START USER LISTENING
             * ---------------------------------------------
             */

            Log.d(
                TAG,
                "Starting continuous user speech recognition"
            )

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

    /**
     * Starts speech recognition for the current
     * voice interaction.
     */
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

            /*
             * ---------------------------------------------
             * PARTIAL RESULT
             * ---------------------------------------------
             */

            onPartial = { text ->

                if (!active) {
                    return@start
                }

                Log.d(
                    TAG,
                    "Partial speech: $text"
                )

                /*
                 * Partial speech is intentionally NOT
                 * classified.
                 *
                 * Vision waits for the complete sentence.
                 */
            },

            /*
             * ---------------------------------------------
             * FINAL RESULT
             * ---------------------------------------------
             */

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
                        "Final result was empty"
                    )

                    stop()
                    onDismiss()

                    return@start
                }

                /*
                 * -----------------------------------------
                 * SEMANTIC AI INTENT LAYER
                 * -----------------------------------------
                 *
                 * The complete natural-language input is
                 * passed to VisionIntentClassifier.
                 *
                 * No:
                 *
                 * text.contains("flashlight")
                 * text.contains("light")
                 * text.contains("call")
                 *
                 * etc.
                 *
                 * The classifier understands the meaning.
                 */

                classifyIntent(
                    text = cleanText,
                    onSpeaking = onSpeaking,
                    onConversation = onConversation,
                    onCommand = onCommand,
                    onDismiss = onDismiss
                )
            },

            /*
             * ---------------------------------------------
             * LISTENING STATE
             * ---------------------------------------------
             */

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
                        "Speech recognition temporarily ended"
                    )

                    /*
                     * DO NOT stop controller here.
                     *
                     * Recognition can report false between
                     * speech phases.
                     */
                }
            },

            /*
             * ---------------------------------------------
             * RECOGNITION ERROR
             * ---------------------------------------------
             */

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
                        "Speech recognition retries exhausted"
                    )

                    stop()
                    onDismiss()
                }
            },

            /*
             * ---------------------------------------------
             * IMPORTANT
             * ---------------------------------------------
             *
             * Continuous mode disables the short
             * 2.5-second watchdog inside
             * LiveSpeechRecognizer.
             *
             * This keeps the active voice conversation
             * alive until Vision itself decides to stop.
             */
            continuous = true
        )
    }

    /**
     * Semantic intent classification.
     *
     * The user's complete sentence is sent to the
     * AI intent layer. This is NOT a keyword matcher.
     */
    private fun classifyIntent(
        text: String,
        onSpeaking: () -> Unit,
        onConversation: (String) -> Unit,
        onCommand: (String) -> Unit,
        onDismiss: () -> Unit
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
                    VisionIntentClassifier(context)

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

                    /*
                     * -------------------------------------
                     * ACTION
                     * -------------------------------------
                     *
                     * Example:
                     *
                     * "Phone ki torch on kar do"
                     * "Andhera hai light jala do"
                     * "Flashlight band kar do"
                     *
                     * All can map to ACTION without
                     * hard-coded sentence matching.
                     */
                    VisionIntentClassifier.IntentType.ACTION -> {

                        onSpeaking()

                        speakAndWait(
                            text = "Ok Boss",
                            onSpeaking = onSpeaking
                        )

                        if (!active) {
                            return@launch
                        }

                        onCommand(text)
                    }

                    /*
                     * -------------------------------------
                     * CONVERSATION
                     * -------------------------------------
                     *
                     * Example:
                     *
                     * "Kaise ho?"
                     *
                     * No "Ok Boss".
                     */
                    VisionIntentClassifier.IntentType.CONVERSATION -> {

                        Log.d(
                            TAG,
                            "Normal conversation"
                        )

                        onConversation(text)
                    }

                    /*
                     * -------------------------------------
                     * QUESTION
                     * -------------------------------------
                     *
                     * Example:
                     *
                     * "Aaj mausam kaisa hai?"
                     */
                    VisionIntentClassifier.IntentType.QUESTION -> {

                        Log.d(
                            TAG,
                            "Question detected"
                        )

                        onConversation(text)
                    }

                    /*
                     * -------------------------------------
                     * SEARCH
                     * -------------------------------------
                     *
                     * Search/web handling is delegated
                     * to the higher AI layer.
                     */
                    VisionIntentClassifier.IntentType.SEARCH -> {

                        Log.d(
                            TAG,
                            "Search request detected"
                        )

                        onConversation(text)
                    }

                    /*
                     * -------------------------------------
                     * UNCLEAR
                     * -------------------------------------
                     *
                     * Vision asks for clarification
                     * instead of guessing.
                     */
                    VisionIntentClassifier.IntentType.UNCLEAR -> {

                        Log.d(
                            TAG,
                            "Intent unclear"
                        )

                        speakAndWait(
                            text = "Boss, thoda clearly bataiye.",
                            onSpeaking = onSpeaking
                        )

                        if (!active) {
                            return@launch
                        }

                        /*
                         * Return to listening after the
                         * clarification.
                         */
                        delay(400L)

                        if (!active) {
                            return@launch
                        }

                        listenForInput(
                            onListening = onListeningCallback,
                            onThinking = {},
                            onSpeaking = onSpeaking,
                            onConversation = onConversation,
                            onCommand = onCommand,
                            onDismiss = onDismiss
                        )
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
                 * If semantic classification fails,
                 * do not blindly say "Ok Boss".
                 *
                 * Send the text to the normal AI
                 * conversation layer instead.
                 */
                onConversation(text)
            }
        }
    }

    /**
     * Speaks a short system response and waits for
     * TTS completion before continuing.
     */
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

        /*
         * Wait until TTS enters speaking state.
         */
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

            /*
             * Wait until TTS finishes.
             */
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
     * Stops the current voice interaction.
     *
     * WakeWordService can restart passive wake-word
     * listening after the voice session is finished.
     */
    fun stop() {

        Log.d(
            TAG,
            "Stopping voice controller"
        )

        active = false
        processing = false
        recognitionRetryCount = 0
        onListeningCallback = {}

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

    /**
     * Completely releases controller resources.
     */
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
