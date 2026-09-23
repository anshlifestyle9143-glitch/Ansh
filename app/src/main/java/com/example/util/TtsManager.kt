package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class TtsManager private constructor(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _currentSpeakingId = MutableStateFlow<Long?>(null)
    val currentSpeakingId: StateFlow<Long?> = _currentSpeakingId

    init {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                _currentSpeakingId.value = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                _currentSpeakingId.value = null
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val hindiResult = tts?.setLanguage(Locale("hi", "IN"))
            val usable = hindiResult != TextToSpeech.LANG_MISSING_DATA &&
                hindiResult != TextToSpeech.LANG_NOT_SUPPORTED

            if (usable) {
                isInitialized = true
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.05f)
            } else {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w("TtsManager", "Language not supported")
                } else {
                    isInitialized = true
                    tts?.setPitch(1.0f)
                    tts?.setSpeechRate(1.05f)
                }
            }
        }
    }

    fun speak(text: String, messageId: Long) {
        if (!isInitialized) return

        if (_isSpeaking.value && _currentSpeakingId.value == messageId) {
            stop()
            return
        }

        stop()
        val cleanText = text
            .replace(Regex("```[\\s\\S]*?```"), " Code block omitted. ")
            .replace(Regex("[#*`_]"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")

        _currentSpeakingId.value = messageId
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, messageId.toString())
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        _currentSpeakingId.value = null
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        @Volatile
        private var INSTANCE: TtsManager? = null

        fun getInstance(context: Context): TtsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TtsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
