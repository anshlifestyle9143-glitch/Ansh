package com.example.service

import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.util.LiveSpeechRecognizer
import com.example.util.GeminiTtsManager
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
 * Central voice pipeline for Wake Word activation.
 *
 * Current pipeline:
 *
 * Hey Jarvis
 *      ↓
 * Yes Boss
 *      ↓
 * Listen
 *      ↓
 * User speech
 *      ↓
 * Intent layer
 *
 * The intent layer is deliberately kept separate from
 * keyword matching so that natural language commands can
 * be added later without changing the wake-word system.
 */
class VisionVoiceController(
    private val context: Context,
    private val ttsManager: GeminiTtsManager
) {

    private val scope =
        CoroutineScope(
            Dispatchers.Main.immediate +
                SupervisorJob()
        )

    private var recognizer: LiveSpeechRecognizer? =
        null

    private var listeningJob: Job? = null

    @Volatile
    private var active = false

    @Volatile
    private var processing = false

    /**
     * Starts the complete wake-word voice interaction.
     *
     * This method does NOT use fixed command keywords.
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
            return
        }

        active = true
        processing = false

        recognizer =
            LiveSpeechRecognizer(context)

        scope.launch {

            /*
             * ---------------------------------------------
             * 1. WAKE WORD RESPONSE
             * ---------------------------------------------
             */

            speakAndWait(
                text = "Yes Boss",
                onSpeaking = onSpeaking
            )

            if (!active) {
                return@launch
            }

            delay(400L)

            /*
             * ---------------------------------------------
             * 2. FIRST USER INPUT
             * ---------------------------------------------
             */

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
     * Listens for one user utterance.
     *
     * IMPORTANT:
     *
     * There is NO:
     *
     * if (text.contains("flashlight"))
     *
     * type of command matching here.
     *
     * The raw speech is passed to the intent layer.
     */
    private fun listenForInput(
        onListening: () -> Unit,
        onThinking: () -> Unit,
        onSpeaking: () -> Unit,
        onConversation: (String) -> Unit,
        onCommand: (String) -> Unit,
        onDismiss: () -> Unit
    ) {

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

            onPartial = {
                /*
                 * Partial speech is intentionally not
                 * processed as a command.
                 */
            },

            onFinal = { text ->

                if (!active) {
                    return@start
                }

                val cleanText =
                    text.trim()

                processing = false

                if (cleanText.isBlank()) {

                    stop()

                    onDismiss()

                    return@start
                }

                /*
                 * -----------------------------------------
                 * 3. INTENT CLASSIFICATION
                 * -----------------------------------------
                 *
                 * CURRENT STEP:
                 *
                 * We keep this method isolated.
                 *
                 * In the next step this becomes the actual
                 * natural-language intent engine.
                 */
                classifyIntent(
                    cleanText,
                    onConversation,
                    onCommand
                )
            },

            onListeningChange = { listening ->

                if (!active) {
                    return@start
                }

                if (listening) {
                    onListening()
                }
            },

            onError = {

                if (!active) {
                    return@start
                }

                processing = false

                stop()

                onDismiss()
            }
        )
    }

    /**
     * Temporary intent gateway.
     *
     * IMPORTANT:
     *
     * This is intentionally NOT keyword matching.
     *
     * For now every utterance is passed through as natural
     * speech. The next step will connect this gateway to
     * Vision's intelligence layer.
     */
    private fun classifyIntent(
        text: String,
        onConversation: (String) -> Unit,
        onCommand: (String) -> Unit
    ) {

        /*
         * Do not guess from keywords.
         *
         * Until the actual intent classifier is connected,
         * treat the input as conversational AI input.
         *
         * This prevents the system from becoming a
         * "rattu tota".
         */
        onConversation(text)
    }

    /**
     * Speaks a short system response and waits for TTS
     * completion before continuing.
     */
    private suspend fun speakAndWait(
        text: String,
        onSpeaking: () -> Unit
    ) {

        if (!active) {
            return
        }

        onSpeaking()

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
        }
    }

    /**
     * Stops current voice interaction.
     *
     * This does NOT permanently disable the Wake Word
     * service. The service can restart passive listening
     * after the overlay task is finished.
     */
    fun stop() {

        active = false
        processing = false

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

        stop()

        scope.coroutineContext[kotlinx.coroutines.Job]
            ?.cancel()
    }
}
