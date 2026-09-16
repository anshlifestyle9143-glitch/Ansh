package com.example.util

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

class GeminiTtsManager(context: Context) {

    private val ttsManager = TtsManager(context)

    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    val currentSpeakingId: StateFlow<Long?> = ttsManager.currentSpeakingId

    fun speak(text: String, messageId: Long) {
        ttsManager.speak(text, messageId)
    }

    fun stop() {
        ttsManager.stop()
    }

    fun shutdown() {
        ttsManager.shutdown()
    }
}
