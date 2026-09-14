package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class LiveSpeechRecognizer(
    private val context: Context
) {

    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())

    private var active = false
    private var sessionId = 0L
    private var lastPartialText = ""
    private var finalDelivered = false
    private var continuousMode = false

    fun start(
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onListeningChange: (Boolean) -> Unit,
        onError: () -> Unit,
        continuous: Boolean = false
    ) {
        active = true
        sessionId++
        lastPartialText = ""
        finalDelivered = false
        continuousMode = continuous

        cancelFinishWatchdog()

        startRecognition(
            sessionId,
            onPartial,
            onFinal,
            onListeningChange,
            onError
        )
    }

    private fun startRecognition(
        currentSession: Long,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onListeningChange: (Boolean) -> Unit,
        onError: () -> Unit
    ) {
        if (
            !active ||
            currentSession != sessionId
        ) {
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError()
            return
        }

        try {
            recognizer?.destroy()
        } catch (_: Exception) {
        }

        recognizer =
            SpeechRecognizer
                .createSpeechRecognizer(context)
                .apply {

                    setRecognitionListener(
                        object : RecognitionListener {

                            override fun onReadyForSpeech(
                                params: Bundle?
                            ) {
                                if (
                                    active &&
                                    currentSession == sessionId
                                ) {
                                    onListeningChange(true)
                                }
                            }

                            override fun onBeginningOfSpeech() {
                                cancelFinishWatchdog()
                            }

                            override fun onRmsChanged(
                                rmsdB: Float
                            ) = Unit

                            override fun onBufferReceived(
                                buffer: ByteArray?
                            ) = Unit

                            override fun onEndOfSpeech() {
                                if (
                                    !active ||
                                    currentSession != sessionId ||
                                    finalDelivered
                                ) {
                                    return
                                }

                                onListeningChange(false)

                                if (!continuousMode) {
                                    scheduleFinishWatchdog(
                                        currentSession,
                                        onFinal,
                                        onError
                                    )
                                }
                            }

                            override fun onError(
                                error: Int
                            ) {
                                if (
                                    !active ||
                                    currentSession != sessionId ||
                                    finalDelivered
                                ) {
                                    return
                                }

                                cancelFinishWatchdog()
                                onListeningChange(false)

                                if (continuousMode) {

                                    handler.postDelayed({

                                        if (
                                            active &&
                                            currentSession == sessionId
                                        ) {
                                            finalDelivered = false
                                            lastPartialText = ""

                                            startRecognition(
                                                currentSession,
                                                onPartial,
                                                onFinal,
                                                onListeningChange,
                                                onError
                                            )
                                        }

                                    }, 350L)

                                } else {
                                    onError()
                                }
                            }

                            override fun onResults(
                                results: Bundle?
                            ) {
                                if (
                                    !active ||
                                    currentSession != sessionId ||
                                    finalDelivered
                                ) {
                                    return
                                }

                                cancelFinishWatchdog()

                                val text =
                                    results
                                        ?.getStringArrayList(
                                            SpeechRecognizer
                                                .RESULTS_RECOGNITION
                                        )
                                        ?.firstOrNull()
                                        ?.trim()
                                        .orEmpty()

                                val finalText =
                                    if (text.isNotBlank()) {
                                        text
                                    } else {
                                        lastPartialText.trim()
                                    }

                                finalDelivered = true

                                onListeningChange(false)

                                try {
                                    recognizer?.stopListening()
                                } catch (_: Exception) {
                                }

                                onFinal(finalText)
                            }

                            override fun onPartialResults(
                                partialResults: Bundle?
                            ) {
                                if (
                                    !active ||
                                    currentSession != sessionId ||
                                    finalDelivered
                                ) {
                                    return
                                }

                                val text =
                                    partialResults
                                        ?.getStringArrayList(
                                            SpeechRecognizer
                                                .RESULTS_RECOGNITION
                                        )
                                        ?.firstOrNull()
                                        ?.trim()
                                        .orEmpty()

                                if (text.isNotBlank()) {
                                    lastPartialText = text
                                    onPartial(text)
                                }
                            }

                            override fun onEvent(
                                eventType: Int,
                                params: Bundle?
                            ) = Unit
                        }
                    )
                }

        val intent =
            Intent(
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

                putExtra(
                    RecognizerIntent
                        .EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                    if (continuousMode) 5000L else 1500L
                )

                putExtra(
                    RecognizerIntent
                        .EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    if (continuousMode) 5000L else 1500L
                )

                putExtra(
                    RecognizerIntent
                        .EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                    if (continuousMode) 1000L else 1500L
                )
            }

        try {

            recognizer?.startListening(intent)

        } catch (_: Exception) {

            if (
                active &&
                currentSession == sessionId
            ) {

                if (continuousMode) {

                    handler.postDelayed({

                        if (
                            active &&
                            currentSession == sessionId
                        ) {
                            finalDelivered = false
                            lastPartialText = ""

                            startRecognition(
                                currentSession,
                                onPartial,
                                onFinal,
                                onListeningChange,
                                onError
                            )
                        }

                    }, 350L)

                } else {

                    onListeningChange(false)
                    onError()
                }
            }
        }
    }

    private fun scheduleFinishWatchdog(
        currentSession: Long,
        onFinal: (String) -> Unit,
        onError: () -> Unit
    ) {
        cancelFinishWatchdog()

        handler.postDelayed({

            if (
                !active ||
                currentSession != sessionId ||
                finalDelivered
            ) {
                return@postDelayed
            }

            val text =
                lastPartialText.trim()

            finalDelivered = true

            try {
                recognizer?.stopListening()
            } catch (_: Exception) {
            }

            if (text.isNotBlank()) {
                onFinal(text)
            } else {
                onError()
            }

        }, 2500L)
    }

    private fun cancelFinishWatchdog() {
        handler.removeCallbacksAndMessages(null)
    }

    fun stop() {
        active = false
        sessionId++

        lastPartialText = ""
        finalDelivered = false
        continuousMode = false

        cancelFinishWatchdog()

        try {
            recognizer?.stopListening()
        } catch (_: Exception) {
        }
    }

    fun destroy() {
        active = false
        sessionId++

        lastPartialText = ""
        finalDelivered = false
        continuousMode = false

        cancelFinishWatchdog()

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
