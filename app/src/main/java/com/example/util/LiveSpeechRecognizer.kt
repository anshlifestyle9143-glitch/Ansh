package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class LiveSpeechRecognizer(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var active = false

    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onListeningChange: (Boolean) -> Unit,
        onError: () -> Unit
    ) {
        active = true

        startRecognition(
            onPartial = onPartial,
            onFinal = onFinal,
            onListeningChange = onListeningChange,
            onError = onError
        )
    }

    private fun startRecognition(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onListeningChange: (Boolean) -> Unit,
        onError: () -> Unit
    ) {
        if (!active) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError()
            return
        }

        try {
            recognizer?.destroy()
        } catch (_: Exception) {
        }

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {

            setRecognitionListener(
                object : RecognitionListener {

                    override fun onReadyForSpeech(params: Bundle?) {
                        onListeningChange(true)
                    }

                    override fun onBeginningOfSpeech() {
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                    }

                    override fun onEndOfSpeech() {
                        onListeningChange(false)
                    }

                    override fun onError(error: Int) {
                        onListeningChange(false)

                        /*
                         * IMPORTANT:
                         * Do NOT automatically restart recognition here.
                         *
                         * The previous implementation restarted the
                         * SpeechRecognizer after every error, which could
                         * create a race between the UI state and the actual
                         * recognizer state.
                         *
                         * A new recognition session will only be started
                         * when start() is explicitly called again.
                         */

                        if (active) {
                            onError()
                        }
                    }

                    override fun onResults(results: Bundle?) {

                        val text =
                            results
                                ?.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                                )
                                ?.firstOrNull()

                        onListeningChange(false)

                        try {
                            recognizer?.stopListening()
                        } catch (_: Exception) {
                        }

                        if (!text.isNullOrBlank()) {
                            onFinal(text)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {

                        val text =
                            partialResults
                                ?.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                                )
                                ?.firstOrNull()

                        if (!text.isNullOrBlank()) {
                            onPartial(text)
                        }
                    }

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?
                    ) {
                    }
                }
            )
        }

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )

            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )

            putExtra(
                RecognizerIntent.EXTRA_CALLING_PACKAGE,
                context.packageName
            )

            /*
             * Keep command finalization reasonably fast.
             */
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                1500L
            )

            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                1500L
            )

            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                1500L
            )
        }

        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            onListeningChange(false)

            if (active) {
                onError()
            }
        }
    }

    fun stop() {
        active = false

        try {
            recognizer?.stopListening()
        } catch (_: Exception) {
        }
    }

    fun destroy() {
        active = false

        try {
            recognizer?.stopListening()
        } catch (_: Exception) {
        }

        try {
            recognizer?.destroy()
        } catch (_: Exception) {
        }

        recognizer = null
    }
}
