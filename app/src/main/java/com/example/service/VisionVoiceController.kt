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
     * Stores the latest listening callback so that
     * Vision can return to listening after clarification.
     */
    private var onListeningCallback: () -> Unit = {}

    /**
     * Starts the voice interaction after wake word detection.
     *
     * Flow:
     *
     * Hey Jarvis
     *      ↓
     * Yes Boss
     *      ↓
     * Listen
     *      ↓
     * Semantic intent classification
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
         * Save callback immediately.
         *
         * This is used later if Vision needs to
         * return to listening after clarification.
         */
        active = true
        onListeningCallback = onListening
        processing = false
        recognitionRetryCount = 0

        recognizer = LiveSpeechRecognizer(context)

        listeningJob = scope.launch {

            /*
             * ---------------------------------------------
             * WAKE WORD RESPONSE
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
             * Allow TTS/audio system to release.
             */
            delay(700L)

            if (!active) {
                return@launch
            }

            /*
             * ---------------------------------------------
             * START LISTENING
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
     * Starts recognition for the active voice session.
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

        /*
         * Check microphone permission.
         */
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
                 * Do NOT classify partial speech.
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
                 * SEMANTIC INTENT CLASSIFICATION
                 * -----------------------------------------
                 *
                 * The complete sentence is passed to
                 * VisionIntentClassifier.
                 *
                 * There is NO hard-coded command matching
                 * here.
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
                     * Do NOT stop Vision here.
                     *
                     * The recognizer can report false between
                     * speech phases.
                     */
                }
            },

            /*
             * ---------------------------------------------
             * ERROR HANDLING
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
             * CONTINUOUS MODE
             * ---------------------------------------------
             *
             * This disables the short watchdog timeout
             * inside LiveSpeechRecognizer.
             *
             * The active voice session is therefore controlled
             * by VisionVoiceController rather than being
             * automatically killed after a few seconds.
             */
            continuous = true
        )
    }

    /**
     * Performs semantic intent classification.
     *
     * IMPORTANT:
     * This does NOT inspect user text using keywords.
     *
     * Example:
     *
     * "Kaise ho?"
     *      → CONVERSATION
     *
     * "Phone ki torch jala do"
     *      → ACTION
     *
     * "India ki capital kya hai?"
     *      → QUESTION
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

                /*
                 * IMPORTANT:
                 *
                 * VisionIntentClassifier currently has
                 * a no-argument constructor.
                 *
                 * DO NOT pass context here.
                 */
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

                    /*
                     * -------------------------------------
                     * ACTION
                     * -------------------------------------
                     *
                     * Only actual action requests receive
                     * "Ok Boss".
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

                        /*
                         * Send the ORIGINAL complete command
                         * to the command executor.
                         */
                        onCommand(text)
                    }

                    /*
                     * -------------------------------------
                     * CONVERSATION
                     * -------------------------------------
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
                     * No "Ok Boss".
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
                     * Ask user to clarify instead of
                     * guessing an action.
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

                        delay(400L)

                        if (!active) {
                            return@launch
                        }

                        /*
                         * Return to listening.
                         *
                         * This uses the callback saved in start().
                         */
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
                 * SAFETY:
                 *
                 * If classification fails, do NOT execute
                 * a device action.
                 *
                 * Send it to the normal AI conversation layer.
                 */
                onConversation(text)
            }
        }
    }

    /**
     * Speaks text and waits for TTS completion.
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
         * Wait for TTS to start.
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
             * Wait for TTS to finish.
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
     * Completely releases resources.
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
