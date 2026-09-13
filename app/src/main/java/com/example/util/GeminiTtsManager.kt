package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import com.example.data.api.VisionRetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiTtsManager(context: Context) {

    private val fallback = TtsManager(context)
    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var audioTrack: AudioTrack? = null
    private var fallbackJob: Job? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _currentSpeakingId = MutableStateFlow<Long?>(null)
    val currentSpeakingId: StateFlow<Long?> = _currentSpeakingId

    fun speak(text: String, messageId: Long) {
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

        scope.launch {
            val apiKey = VisionRetrofitClient.getApiKey()
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                withContext(Dispatchers.Main) { fallback.speak(cleanText, messageId) }
                observeFallback(messageId)
                return@launch
            }

            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-preview-tts:generateContent"

                val body = JSONObject().apply {
                    put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(
                            JSONObject().put("text", cleanText)
                        ))
                    ))
                    put("generationConfig", JSONObject().apply {
                        put("responseModalities", JSONArray().put("AUDIO"))
                        put("speechConfig", JSONObject().apply {
                            put("voiceConfig", JSONObject().apply {
                                put("prebuiltVoiceConfig", JSONObject().apply {
                                    put("voiceName", "Kore")
                                })
                            })
                        })
                    })
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("x-goog-api-key", apiKey)
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseText = response.body?.string() ?: throw Exception("Empty TTS response")
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}: $responseText")

                val json = JSONObject(responseText)
                val base64Audio = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getJSONObject("inlineData")
                    .getString("data")

                val pcmData = Base64.decode(base64Audio, Base64.DEFAULT)
                withContext(Dispatchers.Main) { playPcm(pcmData, messageId) }
            } catch (e: Exception) {
                Log.e("GeminiTtsManager", "Falling back to system TTS: ${e.message}")
                withContext(Dispatchers.Main) { fallback.speak(cleanText, messageId) }
                observeFallback(messageId)
            }
        }
    }

    private fun observeFallback(messageId: Long) {
        fallbackJob?.cancel()
        fallbackJob = scope.launch {
            fallback.isSpeaking.collect { speaking ->
                withContext(Dispatchers.Main) {
                    _isSpeaking.value = speaking
                    _currentSpeakingId.value = if (speaking) messageId else null
                }
            }
        }
    }

    private fun playPcm(pcmData: ByteArray, messageId: Long) {
        val sampleRate = 24000
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack?.stop()
        audioTrack?.release()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBufferSize, pcmData.size))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack?.write(pcmData, 0, pcmData.size)
        _isSpeaking.value = true
        _currentSpeakingId.value = messageId

        audioTrack?.setNotificationMarkerPosition(pcmData.size / 2)
        audioTrack?.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                _isSpeaking.value = false
                _currentSpeakingId.value = null
            }
            override fun onPeriodicNotification(track: AudioTrack?) {}
        })
        audioTrack?.play()
    }

    fun stop() {
        audioTrack?.stop()
        fallback.stop()
        _isSpeaking.value = false
        _currentSpeakingId.value = null
    }

    fun shutdown() {
        audioTrack?.stop()
        audioTrack?.release()
        fallback.shutdown()
    }
}
