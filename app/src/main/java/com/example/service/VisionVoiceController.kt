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
 * Final speech text
 *      ↓
 * Intent / AI layer
 *
 * This controller does NOT use fixed command keywords.
 */
class VisionVoiceController(
    private val context: Context,
    private val ttsManager: GeminiTtsManager
) {

    companion object {
        private const val TAG = "VisionVoiceController"

        /*
         * If speech recognition fails without producing
         * a final result, retry once instead of immediately
         * killing the complete voice session.
         */
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

        active = true
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
             * Give the audio system a moment to release
             * the TTS audio path before opening recognition.
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

            Log.d(TAG, "Starting user speech recognition")

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
     * Starts speech recognition for the user command.
     *
     * No hard-coded command matching happens here.
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

            Log.e(TAG, "RECORD_AUDIO permission missing")

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
             * PARTIAL RESULTS
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
                 * We intentionally do not classify
                 * partial speech.
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
                 * INTENT / AI GATEWAY
                 * -----------------------------------------
                 *
                 * No keyword matching.
                 *
                 * The complete natural-language sentence
                 * is passed to the intelligence layer.
                 */

                classifyIntent(
                    text = cleanText,
                    onConversation = onConversation,
                    onCommand = onCommand
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
                     * IMPORTANT:
                     *
                     * Do NOT stop the complete controller here.
                     *
                     * LiveSpeechRecognizer may briefly report
                     * listening=false between speech phases.
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

                /*
                 * Do not immediately destroy the complete
                 * wake-word conversation.
                 *
                 * Retry recognition a limited number of times.
                 */
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
            }
        )
    }

    /**
     * Temporary natural-language gateway.
     *
     * This intentionally does NOT inspect individual
     * keywords such as "flashlight", "light", etc.
     *
     * The actual semantic intent engine will be connected
     * here after the microphone pipeline is confirmed stable.
     */
    private fun classifyIntent(
        text: String,
        onConversation: (String) -> Unit,
        onCommand: (String) -> Unit
    ) {

        Log.d(
            TAG,
            "Passing natural-language input to AI layer: [$text]"
        )

        /*
         * For now everything goes to the conversation
         * gateway.
         *
         * This will later become:
         *
         * ACTION       -> onCommand()
         * CONVERSATION -> onConversation()
         * QUESTION     -> AI/search
         * UNCLEAR      -> clarification
         *
         * without hard-coded sentence matching.
         */

        onConversation(text)
    }

    /**
     * Speaks a short system response and waits for TTS
     * completion before opening the microphone.
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
         * Wait until TTS actually enters speaking state.
         *
         * If it doesn't, continue after timeout instead
         * of getting stuck forever.
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
             * Wait for TTS completion.
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
     * Stops current voice interaction.
     *
     * Wake-word service itself can restart passive
     * listening after the overlay session finishes.
     */
    fun stop() {

        Log.d(TAG, "Stopping voice controller")

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

    /**
     * Completely releases controller resources.
     */
    fun destroy() {

        Log.d(TAG, "Destroying voice controller")

        stop()

        scope.coroutineContext[
            kotlinx.coroutines.Job
        ]?.cancel()
    }
}
